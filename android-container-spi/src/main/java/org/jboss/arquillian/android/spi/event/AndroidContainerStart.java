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
package org.jboss.arquillian.android.spi.event;

/**
 * This class serves as the event which is fired after overall configurations of Android Container and all extensions
 * are done. This class is different from {@link AndroidContainerStop}. That class is for noticing that Android
 * container as such was configured and it is different from {@link AndroidDroneEvent} - this class is for noticing that
 * Android extensions were configured. {@code AndroidConfigurationDone} merges these two events together, saying that
 * container and extensions were fully configured.
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 */
public class AndroidContainerStart {

}
