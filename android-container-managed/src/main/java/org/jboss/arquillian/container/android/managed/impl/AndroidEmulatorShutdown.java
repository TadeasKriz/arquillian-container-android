/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
 *
 * Copyright (C) 2009, 2010 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.android.managed.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidContainerStop;
import org.jboss.arquillian.android.spi.event.AndroidEmulatorShuttedDown;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceDelete;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.IDevice;

/**
 * Brings Android Emulator down. <br>
 * <br>
 * Observes:
 * <ul>
 * <li>{@link AndroidContainerStop}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidEmulatorShuttedDown}</li>
 * <li>{@link AndroidVirtualDeviceDelete}</li>
 * </ul>
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="manfred@simpligility.com">Manfred Moser</a>
 */
public class AndroidEmulatorShutdown {

    private static final Logger logger = Logger.getLogger(AndroidEmulatorShutdown.class.getName());

    @Inject
    private Instance<AndroidManagedContainerConfiguration> configuration;

    @Inject
    private Instance<AndroidDevice> androidDevice;

    @Inject
    private Instance<AndroidEmulator> androidEmulator;

    @Inject
    private Instance<ProcessExecutor> executor;

    @Inject
    private Event<AndroidEmulatorShuttedDown> androidEmulatorShuttedDown;

    @Inject
    private Event<AndroidVirtualDeviceDelete> androidVirtualDeviceDelete;

    public void shutdownEmulator(@Observes AndroidContainerStop event) throws AndroidExecutionException {

        AndroidEmulator emulator = androidEmulator.get();
        AndroidDevice device = androidDevice.get();
        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        if (emulator != null && device.isEmulator()) {
            final ProcessExecutor executor = this.executor.get();
            final Process p = emulator.getProcess();
            CountDownWatch countdown = new CountDownWatch(configuration.getEmulatorShutdownTimeoutInSeconds(),
                TimeUnit.SECONDS);
            logger.info("Waiting " + countdown.timeout() + " seconds for emulator " + device.getAvdName()
                + " to be disconnected and shutdown.");
            try {
                final DeviceDisconnectDiscovery listener = new DeviceDisconnectDiscovery(device);
                AndroidDebugBridge.addDeviceChangeListener(listener);
                stopEmulator(p, executor, device, countdown);
                waitUntilShutDownIsComplete(device, listener, executor, countdown);
                AndroidDebugBridge.removeDeviceChangeListener(listener);

                if (configuration.isAVDGenerated()) {
                    androidVirtualDeviceDelete.fire(new AndroidVirtualDeviceDelete());
                }

                androidEmulatorShuttedDown.fire(new AndroidEmulatorShuttedDown(device));
            } finally {
                executor.removeShutdownHook(p);
            }
        }
    }

    /**
     * Brings Android device down.
     *
     * @param device {@link AndroidDevice} to shut down
     * @param listener
     * @param executor
     * @param countdown
     * @throws AndroidExecutionException
     */
    private void waitUntilShutDownIsComplete(final AndroidDevice device, final DeviceDisconnectDiscovery listener,
        ProcessExecutor executor, CountDownWatch countdown) throws AndroidExecutionException {

        try {
            // wait until device is disconnected from bridge
            boolean isOffline = executor.scheduleUntilTrue(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return listener.isOffline();
                }
            }, countdown.timeLeft(), countdown.getTimeUnit().convert(1, TimeUnit.SECONDS), countdown.getTimeUnit());

            if (isOffline == false) {
                throw new AndroidExecutionException("Unable to disconnect AVD device {0} in given timeout {1} seconds",
                    device.getAvdName(), countdown.timeout());
            }

            logger.info("Device " + device.getAvdName() + " on port " + device.getConsolePort() + " was disconnected in "
                + countdown.timeElapsed() + " seconds.");
        } catch (InterruptedException e) {
            throw new AndroidExecutionException(e, "Unable to disconnect AVD device {0}", device.getAvdName());
        } catch (ExecutionException e) {
            throw new AndroidExecutionException(e, "Unable to disconnect AVD device {0}", device.getAvdName());
        }

    }

    /**
     * This method contains the code required to stop an emulator.
     *
     * @return {@code true} if stopped without errors, {@code false} otherwise
     * @param device The device to stop
     */
    private Boolean stopEmulator(final Process p, final ProcessExecutor executor, final AndroidDevice device,
        final CountDownWatch countdown) throws AndroidExecutionException {

        int devicePort = extractPortFromDevice(device);
        if (devicePort == -1) {
            logger.severe("Unable to retrieve port to stop emulator " + device.getSerialNumber() + ".");
            return false;
        } else {
            logger.info("Stopping emulator " + device.getSerialNumber() + " via port " + devicePort + ".");

            try {
                Boolean stopped = executor.submit(sendEmulatorCommand(devicePort, "kill")).get(countdown.timeLeft(),
                    countdown.getTimeUnit());

                // wait to retrieve finished process of emulator
                int retval = executor.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return p.waitFor();
                    }
                }).get(5, TimeUnit.SECONDS);

                return stopped && retval == 0;
            } catch (TimeoutException e) {
                p.destroy();
                logger.warning("Emulator process was forcibly destroyed, " + countdown.timeLeft()
                    + " seconds remaining to dispose the device");
                return false;
            } catch (InterruptedException e) {
                p.destroy();
                throw new AndroidExecutionException(e, "Unable to stop emulator {0}", device.getAvdName());
            } catch (ExecutionException e) {
                p.destroy();
                throw new AndroidExecutionException(e, "Unable to stop emulator {0}", device.getAvdName());
            }
        }

    }

    /**
     * This method extracts a port number from the serial number of a device. It assumes that the device name is of format
     * [xxxx-nnnn] where nnnn is the port number.
     *
     * @param device The device to extract the port number from.
     * @return Returns the port number of the device
     */
    private int extractPortFromDevice(AndroidDevice device) {
        String portStr = device.getSerialNumber().substring(device.getSerialNumber().lastIndexOf("-") + 1);
        if (portStr != null && portStr.length() > 0) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        // If the port is not available then return -1
        return -1;
    }

    /**
     * Sends a user command to the running emulator via its telnet interface.
     *
     * @param port The emulator's telnet port.
     * @param command The command to execute on the emulator's telnet interface.
     * @return Whether sending the command succeeded.
     */
    private Callable<Boolean> sendEmulatorCommand(final int port, final String command) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                Socket socket = null;
                BufferedReader in = null;
                PrintWriter out = null;

                try {
                    socket = new Socket("127.0.0.1", port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String telnetOutputString = null;

                    while ((telnetOutputString = in.readLine()) != null) {
                        if (telnetOutputString.equals("OK")) {
                            break;
                        }
                    }

                    out.write(command);
                    out.write("\n");
                    out.flush();
                } finally {
                    try {
                        out.close();
                        in.close();
                        socket.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }

                return true;
            }

        };
    }

    private static class DeviceDisconnectDiscovery implements IDeviceChangeListener {

        private boolean offline;

        private final AndroidDevice connectedDevice;

        public DeviceDisconnectDiscovery(AndroidDevice connectedDevice) {
            this.connectedDevice = connectedDevice;
        }

        @Override
        public void deviceChanged(IDevice device, int changeMask) {
        }

        @Override
        public void deviceDisconnected(IDevice device) {
            if (device.getAvdName().equals(connectedDevice.getAvdName())) {
                this.offline = true;
            }
            logger.fine("Discovered an emulator device id=" + device.getSerialNumber() + " disconnected from ADB bus");
        }

        public boolean isOffline() {
            return offline;
        }

        @Override
        public void deviceConnected(IDevice device) {
        }
    }
}
