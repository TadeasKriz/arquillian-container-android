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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Select either a real device or virtual device for execution. If a real device is specified via its serial number, it
 * will check if it is connected, otherwise it will use an virtual device.
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
 * <li>{@link AndroidVirtualDeviceCreated}</li>
 * <li>{@link AndroidVirtualDeviceAvailable}</li>
 * <li>{@link AndroidDeviceReady}</li>
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
    private Event<AndroidVirtualDeviceCreated> androidVirtualDeviceCreated;

    public void selectDevice(@Observes AndroidBridgeInitialized event) throws AndroidExecutionException {
        try {
            if (isPhysicalDeviceAvailable()) {
                try {
                    AndroidDevice device = getPhysicalDevice();
                    androidDevice.set(device);
                    return;
                } catch (AndroidExecutionException ex) {
                    logger.info("Unable to select Android physical device, let's try virtual device");
                }
            }
            if (isVirtualDeviceAvailable()) {
                try {
                    AndroidDevice device = getVirtualDevice();
                    androidDevice.set(device);
                    return;
                } catch (AndroidExecutionException ex) {
                    logger.info("Unable to select running Android virtual device, let's see if there is AVD we want");
                }
            }
            if (!isAVDCreated() || configuration.get().isForce()) {
                logger.info("Android virtual device " + configuration.get().getAvdName() + " is not present in the system, let's create it");
                try {
                    createAVD();
                    configuration.get().setAVDGenerated(true);
                    logger.info("Android virtual device of name " + configuration.get().getAvdName() + " was successfuly created");
                    androidVirtualDeviceCreated.fire(new AndroidVirtualDeviceCreated(configuration.get().getAvdName()));
                    return;
                } catch (AndroidExecutionException e) {
                    logger.info("Unable to create AVD");
                }
            }
            else {
                logger.info("Android virtual device " + configuration.get().getAvdName() + " already exists, will be reused in tests");
                androidVirtualDeviceAvailable.fire(new AndroidVirtualDeviceAvailable(configuration.get().getAvdName()));
                logger.info("After FIRE !!!!!!!!!!!!!");
                //return;
            }
        } catch(AndroidExecutionException ex) {
            throw new AndroidExecutionException("Unable to get Android Device", ex.getMessage());
        }
    }

    private boolean isAVDCreated() throws AndroidExecutionException {
        ProcessExecutor executor = new ProcessExecutor();
        Set<String> devices = getAvdDeviceNames(executor);
        return devices.contains(configuration.get().getAvdName());
    }

    private boolean isVirtualDeviceAvailable() throws AndroidExecutionException {
        if (configuration.get().getAvdName() == null || configuration.get().getAvdName().trim().isEmpty()) {
            return false;
        }
        
        for (AndroidDevice device : androidBridge.get().getDevices()) {
            if (equalsIgnoreNulls(configuration.get().getAvdName(), device.getAvdName())) {
                return true;
            }
        }
        return false;
    }

    private AndroidDevice getVirtualDevice() throws AndroidExecutionException {
        for (AndroidDevice device : androidBridge.get().getDevices()) {
            if (equalsIgnoreNulls(configuration.get().getAvdName(), device.getAvdName())) {
                return device;
            }
        }
        throw new AndroidExecutionException("Unable to get virtual device of avd name "
            + configuration.get().getAvdName());
    }

    private boolean isPhysicalDeviceAvailable() throws AndroidExecutionException {
        if (configuration.get().getSerialId() == null || configuration.get().getSerialId().trim().isEmpty()) {
            return false;
        }

        for (AndroidDevice device : androidBridge.get().getDevices()) {
            if (configuration.get().getSerialId().equals(device.getSerialNumber())) {
                return true;
            }
        }
        return false;
    }

    private AndroidDevice getPhysicalDevice() throws AndroidExecutionException {
        for (AndroidDevice device : androidBridge.get().getDevices()) {
            if (configuration.get().getSerialId().equals(device.getSerialNumber())) {
                return device;
            }
        }
        throw new AndroidExecutionException("Unable to get psysical device of serial ID "
            + configuration.get().getSerialId());
    }

    private boolean equalsIgnoreNulls(String current, String other) {
        if (current == null && other == null) {
            return false;
        } else if (current == null && other != null) {
            return false;
        } else if (current != null && other == null) {
            return false;
        }

        return current.equals(other);
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
    public void createAVD() throws AndroidExecutionException {
        AndroidManagedContainerConfiguration configuration = this.configuration.get();
        AndroidSDK androidSDK = this.androidSDK.get();
        ProcessExecutor executor = new ProcessExecutor();
        Validate.notNullOrEmpty(configuration.getSdSize(), "Memory SD card size must be defined");

        try {
            List<String> args = new ArrayList<String>(Arrays.asList(androidSDK.getAndroidPath(), "create", "avd", "-n",
                configuration.getAvdName(), "-t", "android-" + configuration.getApiLevel(), "-f", "-p",
                configuration.getAvdName(), "-c", configuration.getSdSize()));
            if (configuration.getAbi() != null) {
                args.add("--abi");
                args.add(configuration.getAbi());
            }
            logger.info("creating new avd " + args.toString());
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
}
