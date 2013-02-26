/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.api.AndroidVirtualDeviceManager;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.managed.configuration.Validate;

/**
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidVirtualDeviceManagerImpl implements AndroidVirtualDeviceManager {

    private static final Logger logger = Logger.getLogger(AndroidVirtualDeviceManagerImpl.class.getName());

    AndroidManagedContainerConfiguration configuration;

    AndroidSDK androidSDK;

    public AndroidVirtualDeviceManagerImpl(AndroidManagedContainerConfiguration configuration, AndroidSDK androidSDK)
            throws IllegalArgumentException {
        Validate.notNulls(new Object[] { configuration, androidSDK },
                "At least one of arguments for AndroidVirtualDeviceManagerImpl is null");
        this.configuration = configuration;
        this.androidSDK = androidSDK;
    }

    @Override
    @SuppressWarnings("serial")
    public void createAndroidVirtualDevice(String avdName) throws AndroidExecutionException {

        String apiLevel = configuration.getApiLevel();
        String sdSize = configuration.getSdSize();
        String abi = configuration.getAbi();

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

    @Override
    public void deleteAndroidVirtualDevice(String avdName) throws AndroidExecutionException {
        // TODO: write me
    }

    @Override
    public boolean androidVirtualDeviceExists(String avdName) throws AndroidExecutionException {
        ProcessExecutor executor = new ProcessExecutor();
        Set<String> devices = getAndroidVirtualDeviceNames(executor);
        return devices.contains(avdName);
    }

    public Set<String> getAndroidVirtualDeviceNames(ProcessExecutor executor) throws AndroidExecutionException {

        final Pattern deviceName = Pattern.compile("[\\s]*Name: ([^\\s]+)[\\s]*");

        Set<String> names = new HashSet<String>();

        List<String> output;
        try {
            output = executor.execute(androidSDK.getAndroidPath(), "list", "avd");
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

    /**
     * Finds out some random string in order to provide some name for AVD.
     *
     * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
     */
    public static final class IdentifierGenerator {

        private static final int NUM_BITS = 130;

        private static final int RADIX = 30;

        private static final SecureRandom random = new SecureRandom();

        public static String getRandomAndroidVirtualDeviceName() {
            return new BigInteger(NUM_BITS, random).toString(RADIX);
        }
    }

}
