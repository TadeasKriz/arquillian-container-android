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
package org.jboss.arquillian.android.spi.event;

import org.jboss.arquillian.container.android.api.AndroidDevice;

/**
 * Event representing there is an Android device ready to be used
 *
 * @author <a href="kpiwko@redhat.com">Karel Piwko</a>
 *
 */
public class AndroidDeviceReady {

    private AndroidDevice device;

    public AndroidDeviceReady(AndroidDevice device) {
        this.device = device;
    }

    public AndroidDevice getDevice() {
        return device;
    }
}
