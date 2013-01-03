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

import org.jboss.arquillian.container.android.managed.impl.AndroidBridgeConnector;
import org.jboss.arquillian.container.android.managed.impl.AndroidContainerConfigurator;
import org.jboss.arquillian.container.android.managed.impl.AndroidDeviceSelector;
import org.jboss.arquillian.container.android.managed.impl.AndroidEmulatorDelete;
import org.jboss.arquillian.container.android.managed.impl.AndroidEmulatorShutdown;
import org.jboss.arquillian.container.android.managed.impl.AndroidEmulatorStartup;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * <p>
 * Android Container Extension
 * </p>
 * 
 * This is the place where all other observers and services are registered.
 * 
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 */
public class AndroidManagedContainerExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, AndroidManagedDeployableContainer.class);
        builder.observer(AndroidContainerConfigurator.class);
        builder.observer(AndroidManagedDeployableContainer.class);
        builder.observer(AndroidDeviceSelector.class);
        builder.observer(AndroidBridgeConnector.class);
        builder.observer(AndroidEmulatorStartup.class);
        builder.observer(AndroidEmulatorShutdown.class);
        builder.observer(AndroidEmulatorDelete.class);
    }

}
