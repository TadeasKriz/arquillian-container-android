/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.arquillian.extension.multiplecontainers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.container.impl.ContainerCreationException;
import org.jboss.arquillian.container.impl.ContainerImpl;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.core.spi.Validate;

/**
 * This class registers all adapters which are specified in the arquillian.xml.
 *
 * In the case there is only one adapter implementation on the classpath, it is not necessary to specify it in the
 * container configuration since it will be used automatically. You have to specify it only in the case you are going to
 * use more than one container.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Stefan Miklosovic <smikloso@redhat.com>
 */
public class MultipleLocalContainersRegistry implements ContainerRegistry {

    private List<Container> containers;

    private Injector injector;

    private static final Logger logger = Logger.getLogger(MultipleContainerRegistryCreator.class.getName());

    private static final String ADAPTER_IMPL_CONFIG_STRING = "adapterImplClass";

    public MultipleLocalContainersRegistry(Injector injector) {
        this.containers = new ArrayList<Container>();
        this.injector = injector;
    }

    @Override
    public Container create(ContainerDef definition, ServiceLoader loader) {
        Validate.notNull(definition, "Definition must be specified");

        try {
            logger.log(Level.INFO, "Registering container: {0}", definition.getContainerName());

            @SuppressWarnings("rawtypes")
            Collection<DeployableContainer> services = loader.all(DeployableContainer.class);

            DeployableContainer<?> dcService = null;

            if (services.size() == 0) {
                throw new ContainerAdapterNotFoundException("There are not any container adapters on the classpath");
            }

            if (services.size() == 1) {
                dcService = services.iterator().next();
            } else {
                Map<String, String> props = definition.getContainerProperties();
                if (!props.containsKey("adapterImplClass")) {
                    throw new ConfigurationException("Container adapter implementation class must be provided via "
                            + ADAPTER_IMPL_CONFIG_STRING + " property.");
                }

                Class<?> dcImplClass = Class.forName(props.get(ADAPTER_IMPL_CONFIG_STRING));

                for (DeployableContainer<?> dc : services) {
                    if (dcImplClass.isInstance(dc)) {
                        dcService = dc;
                        break;
                    }
                }
            }

            if (dcService == null) {
                throw new ContainerAdapterNotFoundException("No suitable container adapter implementation found");
            }

            // before a Container is added to a collection of containers, inject into its injection point
            return addContainer(injector.inject(new ContainerImpl(definition.getContainerName(), dcService, definition)));
        } catch (Exception e) {
            throw new ContainerCreationException("Could not create Container " + definition.getContainerName(), e);
        }
    }

    @Override
    public Container getContainer(String name) {
        return findMatchingContainer(name);
    }

    @Override
    public List<Container> getContainers() {
        return Collections.unmodifiableList(new ArrayList<Container>(containers));
    }

    @Override
    public Container getContainer(TargetDescription target) {
        Validate.notNull(target, "Target must be specified");
        if (TargetDescription.DEFAULT.equals(target)) {
            return findDefaultContainer();
        }
        return findMatchingContainer(target.getName());
    }

    private Container addContainer(Container contianer) {
        containers.add(contianer);
        return contianer;
    }

    private Container findDefaultContainer() {
        if (containers.size() == 1) {
            return containers.get(0);
        }
        for (Container container : containers) {
            if (container.getContainerConfiguration().isDefault()) {
                return container;
            }
        }
        return null;
    }

    private Container findMatchingContainer(String name) {
        for (Container container : containers) {
            if (container.getName().equals(name)) {
                return container;
            }
        }
        return null;
    }

}
