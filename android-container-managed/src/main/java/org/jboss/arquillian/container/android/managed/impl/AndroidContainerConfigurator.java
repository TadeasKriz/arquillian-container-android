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

import java.util.logging.Logger;

import org.jboss.arquillian.android.configuration.ConfigurationMapper;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.android.managed.AndroidManagedDeployableContainer;
import org.jboss.arquillian.container.android.managed.configuration.AndroidContainerConfigurationException;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

/**
 * Configurator of Android container for Arquillian. Note that the configuration method observes 
 * {@link BeforeSuite} event with higher precedence so it will be executed before configuration 
 * is parsed for {@link AndroidManagedDeployableContainer} itself.
 * 
 * Observes:
 * <ul>
 * <li>{@link BeforeSuite}</li>
 * </ul>
 * 
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidContainerConfigurator {
	
    private static Logger logger = Logger.getLogger(AndroidContainerConfigurator.class.getName());

    public static final String ANDROID_EXTENSION_NAME = "android-container";

    public void configureAndroidSdk(
    		@Observes(precedence = 10) BeforeSuite event,
    		ArquillianDescriptor descriptor)
    				throws AndroidContainerConfigurationException {

    	AndroidManagedContainerConfiguration configuration = new AndroidManagedContainerConfiguration();

        for (ContainerDef containerDef : descriptor.getContainers()) {
            if (ANDROID_EXTENSION_NAME.equals(containerDef.getContainerName())) {
            	try {
            		ConfigurationMapper.fromArquillianDescriptor(
            				descriptor, configuration, containerDef.getContainerProperties());
            	} catch(RuntimeException ex) {
            		logger.info("Unable to parse configuration from Arquillian configuration file");
            		throw new AndroidContainerConfigurationException(ex);
            	}
                logger.info("Configured Android extension from Arquillian configuration file");
            }
        }
    }
}
