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

import java.lang.reflect.Method;
import java.util.List;

import org.jboss.arquillian.android.spi.event.AndroidBridgeInitialized;
import org.jboss.arquillian.android.spi.event.AndroidContainerStart;
import org.jboss.arquillian.container.android.api.AndroidBridge;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.managed.AbstractAndroidTestTestBase;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.test.spi.context.TestContext;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests connection of the containers to the {@link AndroidBridge}.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
@org.junit.Ignore
public class AndroidBridgeConnectorTestCase extends AbstractAndroidTestTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(AndroidBridgeConnector.class);
    }

    @Test
    public void testConnectTwoContainersToAndroidBridge() throws AndroidExecutionException, SecurityException,
            NoSuchMethodException {
        // container 1
        getManager().getContext(ContainerContext.class).activate("container1");

        Object instance = new DummyClass();
        Method testMethod = DummyClass.class.getMethod("testDummyMethod");

        getManager().getContext(TestContext.class).activate(instance);

        AndroidManagedContainerConfiguration configuration = new AndroidManagedContainerConfiguration();
        AndroidSDK androidSDK = new AndroidSDK(configuration);

        bind(ContainerScoped.class, AndroidManagedContainerConfiguration.class, configuration);
        bind(ContainerScoped.class, AndroidSDK.class, androidSDK);

        fire(new AndroidContainerStart());

        AndroidBridge bridge = getManager().getContext(ContainerContext.class).getObjectStore().get(AndroidBridge.class);
        assertNotNull("Android Bridge should be created but is null!", bridge);

        assertEventFired(AndroidBridgeInitialized.class, 1);

        fire(new BeforeSuite());

        fire(new BeforeClass(DummyClass.class));
        fire(new Before(instance, testMethod));

        fire(new After(instance, testMethod));
        fire(new AfterClass(DummyClass.class));

        fire(new AfterSuite());

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

        fire(new BeforeSuite());

        fire(new BeforeClass(DummyClass.class));
        fire(new Before(instance, testMethod));

        fire(new After(instance, testMethod));
        fire(new AfterClass(DummyClass.class));

        fire(new AfterSuite());

        assertEventFired(BeforeSuite.class, 2);
    }

    static class DummyClass {
        public void testDummyMethod() {
        }
    }
}
