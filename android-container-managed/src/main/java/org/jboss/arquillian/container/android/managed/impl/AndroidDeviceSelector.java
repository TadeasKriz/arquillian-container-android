/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.android.managed.impl;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidDeviceReady;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceAvailable;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceCreated;
import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.api.DeviceSelector;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.managed.configuration.Validate;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Selects real physical Android device if serial id was specified in the configuration. If serial number was not specified
 * one of the following holds:
 *
 * <br>
 * <br>
 * 1. If console port was specified but avd name was not, we try to connect to running emulator which listens to specified port.
 * If we fails to connect, {@link AndroidExecutionException} is thrown. <br>
 * 2. If avd name was specified but console port was not, we try to connect to the first running emulator of such avd name. <br>
 * 3. If both avd name and console port were specified, we try to connect to this combination. <br>
 * 4. We can fail to get device in the step 3 so we check if there is such avd present in the system. If it is not, we create
 * it. <br>
 * <br>
 *
 * Observes:
 * <ul>
 * <li>{@link AndroidBridgeInitialized}</li>
 * </ul>
 *
 * Creates:
 * <ul>
 * <li>{@link AndroidDevice}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidVirtualDeviceCreated} - when we created avd of some specified name</li>
 * <li>{@link AndroidVirtualDeviceAvailable} - when there is already avd of name we want in the system</li>
 * <li>{@link AndroidDeviceReady} - when we get intance of running Android device</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidDeviceSelector implements DeviceSelector {

    private static Logger logger = Logger.getLogger(AndroidDeviceSelector.class.getSimpleName());

    @Inject
    @ContainerScoped
    private InstanceProducer<AndroidDevice> androidDevice;

    @Inject
    private Instance<AndroidBridge> androidBridge;

    @Inject
    private Instance<AndroidManagedContainerConfiguration> configuration;

    @Inject
    private Instance<AndroidSDK> androidSDK;

    @Inject
    private Event<AndroidVirtualDeviceAvailable> androidVirtualDeviceAvailable;

    @Inject
    private Event<AndroidDeviceReady> androidDeviceReady;

    public void selectDevice(@Observes AndroidBridgeInitialized event) throws AndroidExecutionException {

        AndroidDevice device = null;

        logger.log(Level.INFO, "Before isConnectingToPhysicalDevice");

        if (isConnectingToPhysicalDevice()) {
            device = getPhysicalDevice();
            androidDevice.set(device);
            androidDeviceReady.fire(new AndroidDeviceReady(device));
            return;
        }

        logger.log(Level.INFO, "After isConnectingToPhysicalDevice");

        if (isConnectingToVirtualDevice()) {
            device = getVirtualDevice();
            if (device != null) {
                androidDevice.set(device);
                androidDeviceReady.fire(new AndroidDeviceReady(device));
                return;
            }
        }

        String avdName = configuration.get().getAvdName();

        logger.log(Level.INFO, "Before AVDIdentifierGenerator.getRandomAVDName");

        if (avdName == null) {
            String generatedAvdName = AVDIdentifierGenerator.getRandomAVDName();
            configuration.get().setAvdName(generatedAvdName);
            configuration.get().setAvdGenerated(true);
        }

        logger.log(Level.INFO, "After AVDIdentifierGenerator.getRandomAVDName");

        logger.log(Level.INFO, "Before if(!avdExists())");
        if (!avdExists(avdName)) {
            logger.log(Level.INFO, "Before createAVD");
            createAVD(avdName);
            logger.log(Level.INFO, "After createAVD");
            androidVirtualDeviceAvailable.fire(new AndroidVirtualDeviceAvailable(avdName));
            logger.log(Level.INFO, "after fire in createAVD");
            return;
        }

        logger.log(Level.INFO, "After if(!avdExists())");
        androidVirtualDeviceAvailable.fire(new AndroidVirtualDeviceAvailable(avdName));
        logger.log(Level.INFO, "very last line in selectDevice");
    }

    private boolean avdExists(String avdName) throws AndroidExecutionException {
        ProcessExecutor executor = new ProcessExecutor();
        Set<String> devices = getAvdDeviceNames(executor);
        return devices.contains(avdName);
    }

    private boolean isConnectingToVirtualDevice() {
        return isConsolePortDefined() || isAvdNameDefined();
    }

    private boolean isConsolePortDefined() {
        String consolePort = configuration.get().getConsolePort();
        return consolePort != null && !consolePort.trim().equals("");
    }

    private boolean isAvdNameDefined() {
        String avdName = configuration.get().getAvdName();
        return avdName != null && !avdName.trim().equals("");
    }

    private boolean isOnlyConsolePortAvailable() {
        return isConsolePortDefined() && !isAvdNameDefined();
    }

    private boolean isOnlyAvdNameAvailable() {
        return isAvdNameDefined() && !isConsolePortDefined();
    }

    private AndroidDevice getVirtualDevice() throws AndroidExecutionException {

        String consolePort = configuration.get().getConsolePort();
        String avdName = configuration.get().getAvdName();

        if (isOnlyConsolePortAvailable()) {
            return getVirtualDeviceByConsolePort(consolePort);
        }

        if (isOnlyAvdNameAvailable()) {
            return getVirtualDeviceByAvdName(avdName);
        }

        return getVirtualDevice(consolePort, avdName);
    }

    private AndroidDevice getVirtualDevice(String consolePort, String avdName) throws AndroidExecutionException {
        Validate.notNullOrEmpty(consolePort, "Console port to get emulator of is a null object or an empty string");
        Validate.notNullOrEmpty(avdName, "AVD name to get emulator of is a null object or an empty string");

        List<AndroidDevice> devices = androidBridge.get().getEmulators();

        for (AndroidDevice device : devices) {
            if (device.getConsolePort().equals(consolePort) && device.getAvdName().equals(avdName)) {
                return device;
            }
        }

        return null;

        // do not throw exception since we are going to use already existing avd to start an emulator or we create avd and
        // start emulator afterwards
    }

    private AndroidDevice getVirtualDeviceByConsolePort(String consolePort) throws AndroidExecutionException {
        Validate.notNullOrEmpty(consolePort, "Console port to get emulator of is a null object or an empty string");

        List<AndroidDevice> devices = androidBridge.get().getDevices();

        for (AndroidDevice device : devices) {
            String deviceConsolePort = device.getConsolePort();
            if (deviceConsolePort != null && deviceConsolePort.equals(consolePort)) {
                return device;
            }
        }

        throw new AndroidExecutionException("Unable to get Android device running on the console port " + consolePort);
    }

    private AndroidDevice getVirtualDeviceByAvdName(String avdName) throws AndroidExecutionException {
        Validate.notNullOrEmpty(avdName, "AVD name to get emulator of is a null object or an empty string");

        List<AndroidDevice> devices = androidBridge.get().getDevices();

        for (AndroidDevice device : devices) {
            String deviceAvdName = device.getAvdName();
            if (deviceAvdName != null && deviceAvdName.equals(avdName)) {
                return device;
            }
        }

        return null;

        // do not throw exception since we are going to use already existing avd to start an emulator or we create avd and
        // start emulator afterwards
    }

    private boolean isConnectingToPhysicalDevice() {
        String serialId = configuration.get().getSerialId();
        return serialId != null && !serialId.trim().isEmpty();
    }

    private AndroidDevice getPhysicalDevice() throws AndroidExecutionException {

        String serialId = configuration.get().getSerialId();

        List<AndroidDevice> devices = androidBridge.get().getDevices();

        for (AndroidDevice device : devices) {
            if (serialId.equals(device.getSerialNumber())) {
                logger.info("Detected physical device with serial ID " + serialId + ".");
                return device;
            }
        }

        throw new AndroidExecutionException("Unable to get device with serial ID " + serialId + ".");
    }

    private Set<String> getAvdDeviceNames(ProcessExecutor executor) throws AndroidExecutionException {

        final Pattern deviceName = Pattern.compile("[\\s]*Name: ([^\\s]+)[\\s]*");

        Set<String> names = new HashSet<String>();

        List<String> output;
        try {
            output = executor.execute(androidSDK.get().getAndroidPath(), "list", "avd");
        } catch (InterruptedException e) {
            throw new AndroidExecutionException("Unable to get list of available AVDs", e);
        } catch (ExecutionException e) {
            throw new AndroidExecutionException("Unable to get list of available AVDs", e);
        }
        for (String line : output) {
            Matcher m;
            if (line.trim().startsWith("Name: ") && (m = deviceName.matcher(line)).matches()) {
                String name = m.group(1);
                // skip a device which has no name
                if (name == null || name.trim().length() == 0) {
                    continue;
                }
                names.add(name);
                logger.info("Available Android Device: " + name);
            }
        }

        return names;
    }

    @SuppressWarnings("serial")
    private void createAVD(String avdName) throws AndroidExecutionException {

        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        String apiLevel = configuration.getApiLevel();
        String sdSize = configuration.getSdSize();
        String abi = configuration.getAbi();

        AndroidSDK androidSDK = this.androidSDK.get();
        ProcessExecutor executor = new ProcessExecutor();
        Validate.notNullOrEmpty(sdSize, "Memory SD card size must be defined");

        try {
            List<String> args = new ArrayList<String>(Arrays.asList(androidSDK.getAndroidPath(), "create", "avd", "-n",
                    avdName, "-t", "android-" + apiLevel, "-f", "-p", avdName, "-c", sdSize));
            if (abi != null) {
                args.add("--abi");
                args.add(abi);
            }
            logger.info("Creating new avd " + args.toString());
            String[] argsArrays = new String[args.size()];
            executor.execute(new HashMap<String, String>() {
                {
                    put("Do you wish to create a custom hardware profile [no]", "no\n");
                }
            }, args.toArray(argsArrays));
        } catch (InterruptedException e) {
            throw new AndroidExecutionException("Unable to create a new AVD Device", e);
        } catch (ExecutionException e) {
            throw new AndroidExecutionException("Unable to create a new AVD Device", e);
        }
    }

    /**
     * Finds out some random string in order to provide some name for AVD.
     *
     * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
     */
    private static final class AVDIdentifierGenerator {

        private static final int NUM_BITS = 130;

        private static final int RADIX = 30;

        private static final SecureRandom random = new SecureRandom();

        private static String getRandomAVDName() {
            return new BigInteger(NUM_BITS, random).toString(RADIX);
        }
    }
}
