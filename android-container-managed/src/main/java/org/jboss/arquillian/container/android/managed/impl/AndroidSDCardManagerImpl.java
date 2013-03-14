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
package org.jboss.arquillian.container.android.managed.impl;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidSDCardCreate;
import org.jboss.arquillian.android.spi.event.AndroidSDCardCreated;
import org.jboss.arquillian.android.spi.event.AndroidSDCardDelete;
import org.jboss.arquillian.android.spi.event.AndroidSDCardDeleted;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.api.AndroidSDCardManager;
import org.jboss.arquillian.container.android.api.SDCard;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.managed.configuration.Command;
import org.jboss.arquillian.container.android.utils.IdentifierType;
import org.jboss.arquillian.container.api.IdentifierGenerator;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Implementation class of creation and deletion of Android SD card used for an emulator. <br>
 * <br>
 * Observes:
 * <ul>
 * <li>{@link AndroidSDCardCreate}</li>
 * <li>{@link AndroidSDCardDelete}</li>
 * </ul>
 *
 * Fires:
 * <ul>
 * <li>{@link AndroidSDCardCreated}</li>
 * <li>{@link AndroidSDCardDeleted}</li>
 * </ul>
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidSDCardManagerImpl implements AndroidSDCardManager {

    private static final Logger logger = Logger.getLogger(AndroidSDCardManagerImpl.class.getName());

    @Inject
    Instance<AndroidManagedContainerConfiguration> configuration;

    @Inject
    Instance<AndroidSDK> androidSDK;

    @Inject
    Instance<IdentifierGenerator> idGenerator;

    @Inject
    Event<AndroidSDCardCreated> androidSDCardCreated;

    @Inject
    Event<AndroidSDCardDeleted> androidSDCardDeleted;

    private static final String SD_CARD_DEFAULT_DIR_PATH = "/tmp/";

    private static final String SD_CARD_DEFAULT_SIZE = "128M";

    public void createSDCard(@Observes AndroidSDCardCreate event) throws AndroidExecutionException {

        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        AndroidSDCard sdCard = new AndroidSDCard();
        sdCard.setFileName(configuration.getSdCard());
        sdCard.setGenerated(configuration.getGenerateSDCard());
        sdCard.setLabel(configuration.getSdCardLabel());
        sdCard.setSize(configuration.getSdSize());

        if (sdCard.getLabel() == null) {
            String sdCardLabel = idGenerator.get().getIdentifier(IdentifierType.SD_CARD_LABEL.getClass());
            sdCard.setLabel(sdCardLabel);
        }

        if (sdCard.getSize() == null) {
            sdCard.setSize(SD_CARD_DEFAULT_SIZE);
        }

        if (sdCard.isGenerated()) {
            if (sdCard.getFileName() == null) {
                String sdCardName = SD_CARD_DEFAULT_DIR_PATH + idGenerator.get().getIdentifier(IdentifierType.SD_CARD.getClass());
                sdCard.setFileName(sdCardName);
                configuration.setSdCard(sdCardName);
                createSDCard(sdCard);
                androidSDCardCreated.fire(new AndroidSDCardCreated());
            } else {
                if (new File(sdCard.getFileName()).exists()) {
                    configuration.setGenerateSDCard(false);
                    sdCard.setGenerated(false);
                } else {
                    createSDCard(sdCard);
                    androidSDCardCreated.fire(new AndroidSDCardCreated());
                }
            }
        } else {
            if (sdCard.getFileName() == null) {
                // use default sd card for android emulator
            } else {
                if (new File(sdCard.getFileName()).exists()) {
                    logger.log(Level.INFO, "Using SD card at " + sdCard.getFileName());
                } else {
                    // use default sd card for android emulator but notice user that sd card
                    // he specified does not exist
                    logger.log(Level.INFO, "SD card you specified does not exist (" + sdCard.getFileName() + ") and its " +
                    		"generation is set to false. Default system SD card for Android emulator will be used.");
                }
            }
        }
    }

    public void deleteSDCard(@Observes AndroidSDCardDelete event) {
        AndroidSDCard sdCard = new AndroidSDCard();
        sdCard.setFileName(configuration.get().getSdCard());
        sdCard.setGenerated(configuration.get().getGenerateSDCard());
        try {
            if (sdCard.getFileName() != null && sdCard.isGenerated()) {
                deleteSDCard(sdCard);
                androidSDCardDeleted.fire(new AndroidSDCardDeleted());
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to delete SD card", e);
        }
    }

    @Override
    public void createSDCard(SDCard sdCard) throws AndroidExecutionException {

        AndroidSDCard androidSDCard = (AndroidSDCard) sdCard;

        ProcessExecutor executor = new ProcessExecutor();
        Process sdCardProcess = constructCreateSdCardProcess(executor, sdCard);
        if (createSDCard(sdCardProcess, executor) == 0) {
            logger.log(Level.INFO, "Android SD card labelled {0} located at {1} with size of {2} was created.",
                    new Object[] { androidSDCard.getLabel(), androidSDCard.getFileName(), androidSDCard.getSize() });
        } else {
            logger.log(Level.INFO, "Unable to create SD card labelled {0} located at {1} with size of {2}.",
                    new Object[] { androidSDCard.getLabel(), androidSDCard.getFileName(), androidSDCard.getSize() });
        }

    }

    @Override
    public void deleteSDCard(SDCard sdCard) {

        AndroidSDCard androidSdCard = (AndroidSDCard) sdCard;

        if (androidSdCard.getFileName() != null && androidSdCard.isGenerated()) {
            if (new File(androidSdCard.getFileName()).delete()) {
                logger.log(Level.INFO, "Android SD card labelled {0} located at {1} was deleted",
                        new Object[] { androidSdCard.getLabel(), androidSdCard.getFileName() });
            } else {
                logger.log(Level.INFO, "Unable to delete android SD card labelled {0} located at {1}.",
                        new Object[] { androidSdCard.getLabel(), androidSdCard.getFileName() });
            }
        }
    }

    private int createSDCard(final Process sdCardProcess, final ProcessExecutor executor) throws AndroidExecutionException {
        try {
            int created = executor.submit(new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    return sdCardProcess.waitFor();
                }
            }).get();

            return created;
        } catch (Exception ex) {
            throw new AndroidExecutionException(ex);
        }
    }

    private Process constructCreateSdCardProcess(ProcessExecutor executor, SDCard sdCard) throws AndroidExecutionException {

        AndroidSDCard androidSDCard = (AndroidSDCard) sdCard;

        Command command = new Command();
        command.add(this.androidSDK.get().getMakeSdCardPath())
                .add("-l")
                .add(androidSDCard.getLabel())
                .add(androidSDCard.getSize())
                .add(androidSDCard.getFileName());

        try {
            return executor.spawn(command.get());
        } catch (InterruptedException e) {
            throw new AndroidExecutionException();
        } catch (ExecutionException e) {
            throw new AndroidExecutionException();
        }
    }
}
