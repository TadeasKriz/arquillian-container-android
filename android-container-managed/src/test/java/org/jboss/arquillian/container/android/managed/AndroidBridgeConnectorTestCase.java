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

package org.jboss.arquillian.container.android.managed;

import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.container.android.managed.AndroidManagedDeployableContainer;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.AbstractContainerTestBase;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AndroidBridgeConnectorTestCase extends AbstractContainerTestBase {

    @Inject
    private Instance<Injector> injector;

    @Mock
    private ServiceLoader serviceLoader;

    @Mock
    private AndroidManagedDeployableContainer deployableContainer;

    @Before
    public void setup() throws Exception {
        Mockito.when(serviceLoader.onlyOne(Mockito.same(DeployableContainer.class))).thenReturn(deployableContainer);
        Mockito.when(deployableContainer.getConfigurationClass()).thenReturn(AndroidManagedContainerConfiguration.class);
    }

    @Test
    public void test01() {
        assertTrue(true);
    }
}
