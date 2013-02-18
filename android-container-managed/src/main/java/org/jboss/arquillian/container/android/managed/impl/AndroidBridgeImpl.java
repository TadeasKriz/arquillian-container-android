package org.jboss.arquillian.container.android.managed.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

public class AndroidBridgeImpl implements AndroidBridge {

    private static final Logger logger = Logger.getLogger(AndroidBridgeImpl.class.getSimpleName());
    
    private AndroidDebugBridge delegate;
    
    private static final long ADB_TIMEOUT_MS = 60L * 1000;

    private File adbLocation;

    private boolean forceNewBridge;

    AndroidBridgeImpl(File adbLocation, boolean forceNewBridge) throws IllegalArgumentException {
        Validate.isReadable(adbLocation, "ADB location does not represent a readable file (" + adbLocation + ")");
        this.adbLocation = adbLocation;
        this.forceNewBridge = forceNewBridge;
    }    
    
    @Override
    public void connect() throws AndroidExecutionException {
        logger.info("Connecting to the Android Debug Bridge at " + adbLocation.getAbsolutePath() + " forceNewBridge = " + forceNewBridge);
        this.delegate = AndroidDebugBridge.getBridge();
        if (delegate == null) {
            AndroidDebugBridge.init(false);
            this.delegate = AndroidDebugBridge.createBridge(adbLocation.getAbsolutePath(), forceNewBridge);
            waitUntilConnected();
            waitForInitialDeviceList();
        }
    }


    @Override
    public boolean isConnected() {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");
        return delegate.isConnected();
    }

    @Override
    public void disconnect() throws AndroidExecutionException {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");

        logger.info("Disconnecting Android Debug Bridge at " + adbLocation.getAbsolutePath());
        
        if (isConnected()) {
            System.out.println("\t Android Debug Bridge is connected");
            if (!hasDevices()) {
                System.out.println("\t\t Android Debug Bridge does't have devices");
                AndroidDebugBridge.disconnectBridge();
                AndroidDebugBridge.terminate();
            } else {
                System.out.println("\t\t Android Debug Bridge has devices");
                logger.info("There are still some devices on the Android Debug Bridge bridge will not be disconnected until none are connected.");
            }
        } else {
            System.out.println("\t Android Debug Bridge is not connected");
            logger.info("Android Debug Bridge is already disconnected");
        }
    }

    @Override
    public List<AndroidDevice> getDevices() {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");

        IDevice[] idevices = delegate.getDevices();
        
        List<AndroidDevice> devices = new ArrayList<AndroidDevice>(idevices.length);
        for (IDevice d : idevices) {
            devices.add(new AndroidDeviceImpl(d));
        }

        return devices;
    }
    
    @Override
    public boolean hasDevices() {
        Validate.stateNotNull(delegate, "Android debug bridge must be set. Please call connect() method before execution");
        return delegate.getDevices().length != 0;
    }

    /**
     * Run a wait loop until adb is connected or trials run out. This method seems to work more reliably then using a
     * listener.
     *
     * @param adb
     */
    private void waitUntilConnected() {
        int trials = 10;
        while (trials > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isConnected()) {
                break;
            }
            trials--;
        }
    }

    /**
     * Wait for the Android Debug Bridge to return an initial device list.
     *
     * @param adb
     */
    private void waitForInitialDeviceList() {
        if (!delegate.hasInitialDeviceList()) {
            logger.fine("Waiting for initial device list from the Android Debug Bridge");
            long limitTime = System.currentTimeMillis() + ADB_TIMEOUT_MS;
            while (!delegate.hasInitialDeviceList() && (System.currentTimeMillis() < limitTime)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(
                        "Interrupted while waiting for initial device list from Android Debug Bridge");
                }
            }
            if (!delegate.hasInitialDeviceList()) {
                logger.severe("Did not receive initial device list from the Android Debug Bridge.");
            }
        }
    }
}
