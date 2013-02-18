/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.arquillian.container.android.managed;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidContainerStart;
import org.jboss.arquillian.android.spi.event.AndroidContainerStop;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceAvailable;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * <p>
 * Android Managed container for the Arquillian project
 * </p>
 * 
 * Deployable Android Container class with the whole lifecycle.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 */
public class AndroidManagedDeployableContainer implements DeployableContainer<AndroidManagedContainerConfiguration> {

    private static final Logger logger = Logger.getLogger(AndroidManagedDeployableContainer.class.getSimpleName());

    @Inject
    @ContainerScoped
    private InstanceProducer<AndroidManagedContainerConfiguration> configuration;
    
    @Inject
    @ContainerScoped
    private InstanceProducer<AndroidSDK> androidSDK;

    @Inject
    private Event<AndroidContainerStart> androidContainerStartEvent;
    
    @Inject
    private Event<AndroidContainerStop> androidContainerStopEvent;
    
    @Inject
    private Event<AndroidVirtualDeviceAvailable> androidVirtualDeviceAvailable;
    
    @Override
    public Class<AndroidManagedContainerConfiguration> getConfigurationClass() {
        return AndroidManagedContainerConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("android protocol");
    }

    @Override
    public void setup(AndroidManagedContainerConfiguration configuration) {
        this.configuration.set(configuration);
        this.androidSDK.set(new AndroidSDK(this.configuration.get()));
    }

    @Override
    public void start() throws LifecycleException {
        logger.log(Level.INFO, "Starting the container {0}", configuration.get().getAvdName());
        this.androidContainerStartEvent.fire(new AndroidContainerStart());
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> arg0) throws DeploymentException {
        logger.log(Level.INFO, "Deploying an archive to the container {0}", configuration.get().getAvdName());
        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> arg0) throws DeploymentException {
        logger.log(Level.INFO, "Undeploying an archive from the container {0}", configuration.get().getAvdName());
    }

    @Override
    public void stop() throws LifecycleException {
        logger.log(Level.INFO, "Stopping container {0}", configuration.get().getAvdName());
        this.androidContainerStopEvent.fire(new AndroidContainerStop());
    }

    @Override
    public void undeploy(Descriptor arg0) throws DeploymentException {
        throw new UnsupportedOperationException("Undeployment of a descriptor is not supported.");
    }

    @Override
    public void deploy(Descriptor arg0) throws DeploymentException {
        throw new UnsupportedOperationException("Deployment of a descriptor is not supported");
    }

}
