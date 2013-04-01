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

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidSDCardCreate;
import org.jboss.arquillian.android.spi.event.AndroidSDCardDelete;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceAvailable;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceCreate;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceDelete;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceDeleted;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.managed.configuration.Command;
import org.jboss.arquillian.container.android.managed.configuration.Validate;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Deletes and creates Android virtual devices and initiates deletion of SD card as well.
 *
 * Observes:
 * <ul>
 * <li>{@link AndroidVirtualDeviceDelete}</li>
 * <li>{@link AndroidVirtualDeviceCreate}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidVirtualDeviceAvailable}</li>
 * <li>{@link AndroidVirtualDeviceDeleted}</li>
 * <li>{@link AndroidSDCardDelete}</li>
 * <li>{@link AndroidSDCardCreate}</li>
 * </ul>
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidVirtualDeviceManager {

    private static final Logger logger = Logger.getLogger(AndroidVirtualDeviceManager.class.getName());

    @Inject
    Instance<AndroidManagedContainerConfiguration> configuration;

    @Inject
    Instance<AndroidSDK> androidSDK;

    @Inject
    Event<AndroidVirtualDeviceAvailable> androidVirtualDeviceAvailable;

    @Inject
    Event<AndroidVirtualDeviceDeleted> androidVirtualDeviceDeleted;

    @Inject
    Event<AndroidSDCardDelete> androidSDCardDelete;

    @Inject
    Event<AndroidSDCardCreate> androidSDCardCreate;

    public void deleteAndroidVirtualDevice(@Observes AndroidVirtualDeviceDelete event) {

        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        try {
            ProcessExecutor executor = new ProcessExecutor();
            Process android = constructDeleteProcess(executor, androidSDK.get(), configuration.getAvdName());
            if (deleteAVD(android, executor) == 0) {
                logger.info("Android Virtual Device " + configuration.getAvdName() + " deleted.");
            } else {
                logger.info("Unable to delete Android Virtual Device " + configuration.getAvdName() + ".");
            }
        } catch (AndroidExecutionException ex) {
            logger.info("Unable to delete AVD - " + ex.getMessage());
        }

        androidSDCardDelete.fire(new AndroidSDCardDelete());
        androidVirtualDeviceDeleted.fire(new AndroidVirtualDeviceDeleted(configuration.getAvdName()));
    }

    @SuppressWarnings("serial")
    public void createAndroidVirtualDevice(@Observes AndroidVirtualDeviceCreate event) throws AndroidExecutionException {
        Validate.notNulls(new Object[] { configuration.get(), androidSDK.get() },
            "container configuration injection or Android SDK injection is null");

        androidSDCardCreate.fire(new AndroidSDCardCreate());

        AndroidManagedContainerConfiguration configuration = this.configuration.get();
        AndroidSDK sdk = this.androidSDK.get();
        Validate.notNullOrEmpty(configuration.getSdSize(), "Memory SD card size must be defined");

        ProcessExecutor executor = new ProcessExecutor();

        try {
            Command command = new Command();
            command.add(sdk.getAndroidPath()).add("create").add("avd").add("-n").add(configuration.getAvdName())
                .add("-t").add("android-" + configuration.getApiLevel()).add("-f")
                .add("-p").add(configuration.getGeneratedAvdPath() + configuration.getAvdName());
            if (configuration.getSdCard() != null && new File(configuration.getSdCard()).exists()) {
                command.add("-c").add(configuration.getSdCard());
            } else {
                command.add("-c").add(configuration.getSdSize());
            }
            if (configuration.getAbi() != null) {
                command.add("--abi").add(configuration.getAbi());
            }

            logger.info("Creating new avd " + command);
            String[] argsArrays = new String[command.size()];
            executor.execute(new HashMap<String, String>() {
                {
                    put("Do you wish to create a custom hardware profile [no]", "no\n");
                }
            }, command.getAsList().toArray(argsArrays));

            androidVirtualDeviceAvailable.fire(new AndroidVirtualDeviceAvailable(configuration.getAvdName()));
        } catch (InterruptedException e) {
            throw new AndroidExecutionException("Unable to create a new AVD Device", e);
        } catch (ExecutionException e) {
            throw new AndroidExecutionException("Unable to create a new AVD Device", e);
        }
    }

    private Process constructDeleteProcess(ProcessExecutor executor, AndroidSDK androidSDK, String avdName)
        throws AndroidExecutionException {

        Command command = new Command();
        command.add(androidSDK.getAndroidPath()).add("delete").add("avd").add("-n").add(avdName);

        try {
            return executor.spawn(command.getAsList());
        } catch (InterruptedException e) {
            throw new AndroidExecutionException(e, "Unable to delete AVD {0}.", avdName);
        } catch (ExecutionException e) {
            throw new AndroidExecutionException(e, "Unable to delete AVD {0}.", avdName);
        }
    }

    private int deleteAVD(final Process android, final ProcessExecutor executor) throws AndroidExecutionException {
        try {
            int deleted = executor.submit(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    return android.waitFor();
                }
            }).get();

            return deleted;
        } catch (Exception ex) {
            throw new AndroidExecutionException(ex);
        }
    }

}
