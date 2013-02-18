package org.jboss.arquillian.container.android.api;

import java.util.List;

public interface AndroidBridge {

    /**
     * Lists all devices currently available
     * 
     * @return List of available devices
     */
    List<AndroidDevice> getDevices();

    /**
     * Connects to the bridge
     * 
     * @throws AndroidExecutionException
     */
    void connect() throws AndroidExecutionException;

    /**
     * Checks if bridge is connected
     * 
     * @return {@code true} if connected, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Disconnects bridge and disposes connection
     * 
     * @throws AndroidExecutionException
     */
    void disconnect() throws AndroidExecutionException;
    
    /**
     * Checks if there are some devices on the bridge.
     * @return true if there are some devices on the bridge, false otherwise
     */
    boolean hasDevices();
}
