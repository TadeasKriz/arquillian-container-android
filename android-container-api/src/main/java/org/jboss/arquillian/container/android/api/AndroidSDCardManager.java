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
package org.jboss.arquillian.container.android.api;

/**
 * Manages creation and deleting of an Android SD card.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 */
public interface AndroidSDCardManager {

    /**
     * Creates Android SD card.
     *
     * @param sdCard sdCard to create
     * @throws AndroidExecutionException
     */
    void createSDCard(SDCard sdCard) throws AndroidExecutionException;

    /**
     * Deletes Android SD Card.
     *
     * @param sdCard sdCard to delete
     */
    void deleteSDCard(SDCard sdCard);
}
