package org.jboss.arquillian.container.android.managed.impl;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidBridgeTerminated;
import org.jboss.arquillian.android.spi.event.AndroidContainerStart;
import org.jboss.arquillian.android.spi.event.AndroidContainerStop;
import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class AndroidBridgeConnector {

    private static final Logger logger = Logger.getLogger(AndroidBridgeConnector.class.getSimpleName());

    @Inject
    @ContainerScoped
    private InstanceProducer<AndroidBridge> androidBridge;
    
    @Inject
    private Instance<AndroidSDK> androidSDK;
    
    @Inject
    private Instance<AndroidManagedContainerConfiguration> configuration;
    
    @Inject
    private Event<AndroidBridgeInitialized> adbInitialized;

    @Inject
    private Event<AndroidBridgeTerminated> adbTerminated;

    /**
     * Initializes Android Debug Bridge and fires {@link AndroidBridgeInitialized} event.
     *
     * @param event
     * @param sdk
     * @param configuration
     * @throws AndroidExecutionException
     */
    public void initAndroidDebugBridge(@Observes AndroidContainerStart event) throws AndroidExecutionException {
        
        long start = System.currentTimeMillis();
        logger.info("Initializing Android Debug Bridge");
        AndroidBridge bridge = new AndroidBridgeImpl(new File(androidSDK.get().getAdbPath()), configuration.get().isForce());
        bridge.connect();
        long delta = System.currentTimeMillis() - start;
        logger.info("Android debug Bridge was initialized in " + delta + "ms");
        androidBridge.set(bridge);
        adbInitialized.fire(new AndroidBridgeInitialized());
    }

    /**
     * Destroys Android Debug Bridge and fires {@link AndroidBridgeTerminated} event.
     *
     * @param event
     * @throws AndroidExecutionException
     */
    public void terminateAndroidDebugBridge(@Observes AndroidContainerStop event) throws AndroidExecutionException {
        logger.info("terminating of Android Debug Bridge");
        androidBridge.get().disconnect();
        adbTerminated.fire(new AndroidBridgeTerminated());
    }
    
    public void afterTerminateAndroidDebugBridge(@Observes AndroidBridgeTerminated event) {
        logger.info("Executing operations after destroying Android Debug Bridge");
    }
}
