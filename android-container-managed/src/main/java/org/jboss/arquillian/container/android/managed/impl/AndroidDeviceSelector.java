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
import org.jboss.arquillian.container.android.managed.configuration.AndroidContainerConfigurationException;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;

/**
 * Select either a real device or virtual device for execution. If a real device is 
 * specified via its serial number, it will check if it is connected, 
 * otherwise it will use an virtual device.
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
 *
 */
public class AndroidDeviceSelector {
    private static Logger logger = Logger.getLogger(AndroidDeviceSelector.class.getName());

    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidDevice> androidDevice;

    @Inject
    private Event<AndroidVirtualDeviceCreated> avdCreated;

    @Inject
    private Event<AndroidVirtualDeviceAvailable> avdAvailable;

    @Inject
    private Event<AndroidDeviceReady> androidDeviceReady;

    public void getOrCreateAndroidDevice(
    		@Observes AndroidBridgeInitialized event,
    		ProcessExecutor executor,
            AndroidManagedContainerConfiguration configuration,
            AndroidSDK androidSDK)
            		throws AndroidExecutionException {

        String avdName = configuration.getAvdName();
        String serialId = configuration.getSerialId();
        AndroidBridge bridge = event.getBridge();

        AndroidDevice device = null;
        
        // get priority for device specified by serialId if such device is connected
        
        if ((device = getRealDeviceIfConnected(bridge, serialId)) != null) {
            androidDevice.set(device);
            androidDeviceReady.fire(new AndroidDeviceReady(device));
            logger.log(Level.INFO, "Using connected physical device with serial ID of {0}.", serialId);
            return;
        }
        
        // then try to use connected AVD
        
        if ((device = getVirtualDeviceIfConnected(bridge, avdName)) != null) {
        	androidDevice.set(device);
        	androidDeviceReady.fire(new AndroidDeviceReady(device));
        	logger.log(Level.INFO, "Using already started emulator of AVD name {0} and serial ID {1}",
        			new Object[] {avdName, device.getSerialNumber()});
        	return;
        }

        Set<String> devices = getAvdDeviceNames(executor, androidSDK);
        
        // we failed in both, so check out AVD availability and if it not exits, create it
        if (!devices.contains(avdName) || configuration.isForce()) {
        	logger.log(Level.INFO, "Creating an Android virtual device named " + avdName);

        	createAVD(configuration, androidSDK, executor);
        	
        	logger.info("Android virtual device " + configuration.getAvdName() + " was created");
            configuration.setAVDIsGenerated(true);
            avdCreated.fire(new AndroidVirtualDeviceCreated(avdName));
        } else {
            logger.info("Android virtual device " + avdName + " already exists, will be reused in tests");
            avdAvailable.fire(new AndroidVirtualDeviceAvailable(avdName));
        }
    }

    @SuppressWarnings("serial")
	public void createAVD(
    		AndroidManagedContainerConfiguration configuration,
    		AndroidSDK androidSDK,
    		ProcessExecutor executor)
    				throws AndroidExecutionException {
        Validate.notNullOrEmpty(configuration.getSdSize(), "Memory SD card size must be defined");

        try {
            List<String> args = new ArrayList<String>(Arrays.asList(androidSDK.getAndroidPath(),
            		"create",
            		"avd",
            		"-n", configuration.getAvdName(),
            		"-t", "android-" + configuration.getApiLevel(),
            		"-f",
                    "-p", configuration.getAvdName(),
                    "-c", configuration.getSdSize()
                    ));
            if (configuration.getAbi() != null) {
                args.add("--abi");
                args.add(configuration.getAbi());
            }
            logger.log(Level.INFO,"creating new avd -> {0}", args.toString());
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
        } catch(AndroidContainerConfigurationException ex) {
        	throw new AndroidExecutionException("Unable to determine proper configuration for " +
        			"creation of AVD device, check if memory SD card size is defined in the " +
        			"container configuration.");
        }
        
    }
    
    private AndroidDevice getVirtualDeviceIfConnected(AndroidBridge bridge, String avdName) {
    	// no avdName was specified
    	if (avdName == null || avdName.trim().isEmpty()) {
    		return null;
    	}

        for (AndroidDevice device : bridge.getDevices()) {
            if (equalsIgnoreNulls(avdName, device.getAvdName())) {
                return device;
            }
        }
        
        return null;
	}

	private AndroidDevice getRealDeviceIfConnected(AndroidBridge bridge, String serialId) {
        // no serialId was specified
        if (serialId == null || serialId.trim().isEmpty()) {
            return null;
        }

        for (AndroidDevice device : bridge.getDevices()) {
            if (serialId.equals(device.getSerialNumber())) {
                return device;
            }
        }

        logger.warning("SerialId " + serialId + " was specified, however no such device was " +
        		"connected. Trying to connect to an emulator instead.");

        return null;
    }

    private Set<String> getAvdDeviceNames(ProcessExecutor executor, AndroidSDK sdk) throws AndroidExecutionException {

        final Pattern deviceName = Pattern.compile("[\\s]*Name: ([^\\s]+)[\\s]*");

        Set<String> names = new HashSet<String>();

        List<String> output;
        try {
            output = executor.execute(sdk.getAndroidPath(), "list", "avd");
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
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Available Android Device: " + name);
                }
            }
        }

        return names;
    }
    
    private boolean equalsIgnoreNulls(String current, String other) {
        if (current == null && other == null) {
            return false;
        } else if (current == null && other != null) {
            return false;
        } else if (current != null && other == null) {
            return false;
        }

        logger.info(current + " " + other);
        
        return current.equals(other);
    }
}
