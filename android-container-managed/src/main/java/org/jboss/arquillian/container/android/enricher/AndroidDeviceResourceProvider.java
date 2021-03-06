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
package org.jboss.arquillian.container.android.enricher;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import org.jboss.arquillian.container.android.api.AndroidDevice;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * Resource provider which allows to get control of an AVD device.
 *
 * User can use this to install an APK, do forwardning or execute an arbitrary command on the device manually.
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @see AndroidDevice
 */
public class AndroidDeviceResourceProvider implements ResourceProvider {

    private static final Logger log = Logger.getLogger(AndroidDeviceResourceProvider.class.getName());

    @Inject
    Instance<AndroidDevice> androidDevice;

    @Override
    public boolean canProvide(Class<?> type) {
        return AndroidDevice.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        AndroidDevice device = androidDevice.get();
        if (device == null) {
            log.severe("Unable to Inject Android Device controller into test");
            throw new IllegalStateException("Unable to Inject Android Device controller into test");
        }

        return device;
    }

}