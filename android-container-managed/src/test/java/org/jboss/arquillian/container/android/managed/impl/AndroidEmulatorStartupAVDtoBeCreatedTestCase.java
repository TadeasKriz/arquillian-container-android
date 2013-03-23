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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidContainerStart;
import org.jboss.arquillian.android.spi.event.AndroidContainerStop;
import org.jboss.arquillian.android.spi.event.AndroidEmulatorShuttedDown;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceAvailable;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceCreate;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceDelete;
import org.jboss.arquillian.android.spi.event.AndroidVirtualDeviceDeleted;
import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.api.IdentifierGenerator;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.utils.IdentifierType;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.test.AbstractContainerTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests creating of AVD name from scratch and staring of emulator
 * of newly created AVD.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
@org.junit.Ignore
public class AndroidEmulatorStartupAVDtoBeCreatedTestCase extends AbstractContainerTestBase {

    private String AVD_GENERATED_NAME = "ab1be336-d30f-4d3c-90de-56bdaf198a3e";

    private AndroidManagedContainerConfiguration configuration;

    private AndroidSDK androidSDK;

    @Mock
    private IdentifierGenerator idGenerator;

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(AndroidBridgeConnector.class);
        extensions.add(AndroidDeviceSelectorImpl.class);
        extensions.add(AndroidEmulatorStartup.class);
        extensions.add(AndroidEmulatorShutdown.class);
        extensions.add(AndroidVirtualDeviceManager.class);
    }

    @Before
    public void setup() {
        configuration = new AndroidManagedContainerConfiguration();
        androidSDK = new AndroidSDK(configuration);

        getManager().getContext(ContainerContext.class).activate("doesnotmatter");

        Mockito.when(idGenerator.getIdentifier(IdentifierType.AVD.getClass())).thenReturn(AVD_GENERATED_NAME);

        bind(ContainerScoped.class, AndroidManagedContainerConfiguration.class, configuration);
        bind(ContainerScoped.class, AndroidSDK.class, androidSDK);
        bind(ContainerScoped.class, IdentifierGenerator.class, idGenerator);
    }

    @After
    public void disposeMocks() throws AndroidExecutionException {
        AndroidBridge bridge = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        bridge.disconnect();
    }

    @Test
    public void testCreateAVDandStartEmulator() {
        fire(new AndroidContainerStart());

        AndroidBridge bridge = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        bind(ContainerScoped.class, AndroidBridge.class, bridge);

        AndroidDevice runningDevice = getManager().getContext(ContainerContext.class)
                .getObjectStore().get(AndroidDevice.class);
        assertNotNull("Android device is null!", runningDevice);
        bind(ContainerScoped.class, AndroidDevice.class, runningDevice);

        AndroidEmulator emulator = getManager().getContext(ContainerContext.class)
                .getObjectStore().get(AndroidEmulator.class);
        assertNotNull("Android emulator is null!", emulator);
        bind(ContainerScoped.class, AndroidEmulator.class, emulator);

        assertTrue(configuration.isAVDGenerated());
        assertEquals(AVD_GENERATED_NAME, runningDevice.getAvdName());

        fire(new AndroidContainerStop());

        assertEventFired(AndroidContainerStart.class, 1);
        assertEventFired(AndroidBridgeInitialized.class, 1);
        assertEventFired(AndroidVirtualDeviceCreate.class, 1);
        assertEventFired(AndroidVirtualDeviceAvailable.class, 1);
        assertEventFired(AndroidContainerStop.class, 1);
        assertEventFired(AndroidEmulatorShuttedDown.class, 1);
        assertEventFired(AndroidVirtualDeviceDelete.class, 1);
        assertEventFired(AndroidVirtualDeviceDeleted.class, 1);

        assertEventFiredInContext(AndroidContainerStart.class, ContainerContext.class);
        assertEventFiredInContext(AndroidBridgeInitialized.class, ContainerContext.class);
        assertEventFiredInContext(AndroidVirtualDeviceCreate.class, ContainerContext.class);
        assertEventFiredInContext(AndroidVirtualDeviceAvailable.class, ContainerContext.class);
        assertEventFiredInContext(AndroidContainerStop.class, ContainerContext.class);
        assertEventFiredInContext(AndroidEmulatorShuttedDown.class, ContainerContext.class);
        assertEventFiredInContext(AndroidVirtualDeviceDelete.class, ContainerContext.class);
        assertEventFiredInContext(AndroidVirtualDeviceDeleted.class, ContainerContext.class);
    }
}
