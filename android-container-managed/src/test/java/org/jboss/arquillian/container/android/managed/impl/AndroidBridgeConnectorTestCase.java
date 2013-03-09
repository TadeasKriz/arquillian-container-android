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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidContainerStart;
import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.test.AbstractContainerTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests connection of the container to the {@link AndroidBridge}.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AndroidBridgeConnectorTestCase extends AbstractContainerTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(AndroidBridgeConnector.class);
    }

    @Test
    public void testConnectTwoContainersToAndroidBridge() throws AndroidExecutionException {
        // container 1
        getManager().getContext(ContainerContext.class).activate("container1");

        AndroidManagedContainerConfiguration configuration = new AndroidManagedContainerConfiguration();
        AndroidSDK androidSDK = new AndroidSDK(configuration);

        bind(ContainerScoped.class, AndroidManagedContainerConfiguration.class, configuration);
        bind(ContainerScoped.class, AndroidSDK.class, androidSDK);

        fire(new AndroidContainerStart());

        AndroidBridge bridge = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        assertNotNull("Android Bridge should be created but is null!", bridge);

        bridge.disconnect();

        assertEventFired(AndroidBridgeInitialized.class, 1);

        getManager().getContext(ContainerContext.class).getObjectStore().clear();
        getManager().getContext(ContainerContext.class).deactivate();

        // container 2
        getManager().getContext(ContainerContext.class).activate("container2");

        AndroidManagedContainerConfiguration configuration2 = new AndroidManagedContainerConfiguration();
        AndroidSDK androidSDK2 = new AndroidSDK(configuration2);

        bind(ContainerScoped.class, AndroidManagedContainerConfiguration.class, configuration2);
        bind(ContainerScoped.class, AndroidSDK.class, androidSDK2);

        fire(new AndroidContainerStart());

        AndroidBridge bridge2 = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        assertNotNull("Android Bridge should be created but is null!", bridge2);

        bridge2.disconnect();

        assertEventFired(AndroidBridgeInitialized.class, 2);
    }

}
