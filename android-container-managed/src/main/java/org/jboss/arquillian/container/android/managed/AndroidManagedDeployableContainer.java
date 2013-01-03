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

import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidContainerConfigured;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.managed.impl.ProcessExecutor;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * <p>Android Managed container for the Arquillian project</p>
 * 
 * Deployable Android Container class with the whole lifecycle.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 */
public class AndroidManagedDeployableContainer implements DeployableContainer<AndroidManagedContainerConfiguration> {
    
    private static final String LIFE_CYCLE_SETUP_MISSING_CONFIG_EXCEPTION_MSG =
    		"Configuration for an Arquillan Andorid Container must not be null";

    private static final Logger logger =
    		Logger.getLogger(AndroidManagedDeployableContainer.class.getName());    
    
    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidManagedContainerConfiguration> configuration;
    
    @Inject
    @SuiteScoped
    private InstanceProducer<AndroidSDK> androidSDK;
    
    @Inject
    @SuiteScoped
    private InstanceProducer<ProcessExecutor> executor;
    
    @Inject
    private Event<AndroidContainerConfigured> afterConfiguration;
    
    ////////////////////////////
    // (1) GET CONFIGURATION CLASS
    ////////////////////////////

    @Override
    public Class<AndroidManagedContainerConfiguration> getConfigurationClass() {
        logger.info("ANDROID MANAGED CONTAINER GET CONFIGURATION CLASS");
        return AndroidManagedContainerConfiguration.class;
    }

    ////////////////////////////
    // (2) SETUP
    ////////////////////////////

    @Override
    public void setup(AndroidManagedContainerConfiguration configuration) {
    	logger.info("ANDROID MANAGED CONTAINER SETUP");
        if (configuration == null) {
            throw new IllegalArgumentException(LIFE_CYCLE_SETUP_MISSING_CONFIG_EXCEPTION_MSG);
        }
        this.configuration.set(configuration);
        executor.set(new ProcessExecutor());
        androidSDK.set(new AndroidSDK(configuration));
        logger.info(configuration.toString());
    }

    ////////////////////////////
    // (3) START
    ////////////////////////////

    @Override
    public void start() throws LifecycleException {
        logger.info("ANDROID MANAGED CONTAINER START");
        afterConfiguration.fire(new AndroidContainerConfigured());
        logger.info("ANDROID MANAGED CONTAINER AFTER FIRING CONFIGURATION EVENT");
    }

    ////////////////////////////
    // (4) DEPLOY ARCHIVE
    ////////////////////////////

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        logger.info("ANDROID MANAGED CONTAINER DEPLOY ARCHIVE");
        return new ProtocolMetaData();
    }

    ////////////////////////////
    // (5) GET DEFAULT PROTOCOL
    ////////////////////////////

    @Override
    public ProtocolDescription getDefaultProtocol() {
        logger.info("ANDROID MANAGED CONTAINER GET DEFAULT PROTOCOL");
        return new ProtocolDescription("Servlet 3.0");
    }

    ////////////////////////////
    // (6) UPDEPLOY ARCHIVE
    ////////////////////////////

    @Override
    public void undeploy(Archive<?> arg0) throws DeploymentException {
        logger.info("ANDROID MANAGED CONTAINER UNDEPLOY ARCHIVE");
    }

    ////////////////////////////
    // (7) STOP
    ////////////////////////////

    @Override
    public void stop() throws LifecycleException {
        logger.info("ANDROID MANAGED CONTAINER STOP");
    }

    // =======================================================================================

    ////////////////////////////
    // UNDEPLOY DESCRIPTOR
    ////////////////////////////

    @Override
    public void undeploy(Descriptor arg0) throws DeploymentException {
        logger.info("ANDROID MANAGED CONTAINER UNDEPLOY DESCRIPTOR");
    }

    ////////////////////////////
    // DEPLOY DESCRIPTOR
    ////////////////////////////

    @Override
    public void deploy(Descriptor arg0) throws DeploymentException {
        logger.info("ANDROID MANAGED CONTAINER DEPLOY DESCRIPTOR");
    }
}
