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
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.android.spi.event.AndroidSDCardCreate;
import org.jboss.arquillian.android.spi.event.AndroidSDCardDelete;
import org.jboss.arquillian.container.android.api.AndroidExecutionException;
import org.jboss.arquillian.container.android.api.AndroidSDCardManager;
import org.jboss.arquillian.container.android.managed.configuration.AndroidManagedContainerConfiguration;
import org.jboss.arquillian.container.android.managed.configuration.AndroidSDK;
import org.jboss.arquillian.container.android.managed.configuration.Validate;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class AndroidSDCardManagerImpl implements AndroidSDCardManager {

    private static final Logger logger = Logger.getLogger(AndroidSDCardManagerImpl.class.getName());

    @Inject
    Instance<AndroidManagedContainerConfiguration> configuration;

    @Inject
    Instance<AndroidSDK> androidSDK;

    private static final String SD_CARD_DEFAULT_DIR_PATH = "/tmp/";

    private static final String SD_CARD_DEFAULT_SIZE = "128M";

    public void createSDCard(@Observes AndroidSDCardCreate event) {

        logger.log(Level.INFO, "before isUsingSystemCard()");

        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        if (!isUsingSystemSDCard()) {
            if (generateSDCard()) {
                logger.log(Level.INFO, "after generateSDCard()");
                configuration.setSdCard(SD_CARD_DEFAULT_DIR_PATH + IdentifierGenerator.getRandomSDCardName());
                configuration.setSdCardFileNameGenerated(true);
            }
            createSDCard();
            logger.log(Level.INFO, "after createSDCard()");
        }
        logger.log(Level.INFO, "after isUsingSystemCard()");
    }

    public void deleteSDCard(@Observes AndroidSDCardDelete event) {
        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        if (configuration.getSdCard() != null && configuration.isSdCardFileNameGenerated()) {
            if (new File(configuration.getSdCard()).delete()) {
                logger.log(Level.INFO, "Android SD card labelled {0} located at {1} with size of {2} was deleted",
                        new Object[] { configuration.getSdCardLabel(), configuration.getSdCard(), configuration.getSdSize() });
            } else {
                logger.log(Level.INFO, "Unable to delete android SD card labelled {0} located at {1} with size of {2}.",
                        new Object[] { configuration.getSdCardLabel(), configuration.getSdCard(), configuration.getSdSize() });
            }
        }
    }

    @Override
    public void createSDCard() {

        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        try {

            if (configuration.getSdCard() == null) {
                configuration.setSdCard(SD_CARD_DEFAULT_DIR_PATH + IdentifierGenerator.getRandomSDCardName());
                configuration.setSdCardFileNameGenerated(true);
            }

            validateSdCard(configuration.getSdCard());

            if (configuration.getSdCardLabel() == null) {
                configuration.setSdCardLabel(new File(configuration.getSdCard()).getName().split(".")[0]);
            }

            if (configuration.getSdSize() == null) {
                configuration.setSdSize(SD_CARD_DEFAULT_SIZE);
            }

            ProcessExecutor executor = new ProcessExecutor();
            Process sdCardProcess = constructCreateSdCardProcess(executor);
            if (createSDCard(sdCardProcess, executor) == 0) {
                logger.log(Level.INFO, "Android SD card labelled {0} located at {1} with size of {2} was created.",
                        new Object[] { configuration.getSdCardLabel(), configuration.getSdCard(), configuration.getSdSize() });
            } else {
                logger.log(Level.INFO, "Unable to create SD card labelled {0} located at {1} with size of {2}.",
                        new Object[] { configuration.getSdCardLabel(), configuration.getSdCard(), configuration.getSdSize() });
            }
        } catch (AndroidExecutionException e) {
            logger.log(Level.INFO, "Unable to create SD card", e);
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

    private void validateSdCard(String sdCard) {

        AndroidManagedContainerConfiguration configuration = this.configuration.get();

        File sdCardFile = new File(sdCard);
        Validate.isReadableDirectory(sdCardFile.getParentFile(),
                "Directory of the SD card for '" + configuration.getAvdName() + "' is not readable.");
        Validate.isWritable(sdCardFile, "Location of the SD card for the Android container '" + configuration.getAvdName()
                + "' is not writable.");
        Validate.sdCardFileName(sdCardFile.getName(), "File name of SD card to create '" + sdCardFile.getName()
                + "' does not have '.img' suffix.");
    }

    private Process constructCreateSdCardProcess(ProcessExecutor executor) throws AndroidExecutionException {

        AndroidManagedContainerConfiguration configuration = this.configuration.get();
        AndroidSDK sdk = this.androidSDK.get();

        List<String> createSdCardCommand = new ArrayList<String>(Arrays.asList(
                sdk.getMakeSdCardPath(),
                "-l",
                configuration.getSdCardLabel(),
                configuration.getSdSize(),
                configuration.getSdCard()));

        try {
            return executor.spawn(createSdCardCommand);
        } catch (InterruptedException e) {
            throw new AndroidExecutionException();
        } catch (ExecutionException e) {
            throw new AndroidExecutionException();
        }
    }

    @Override
    public void deleteSDCard() {
        // TODO Auto-generated method stub

    }

    private boolean generateSDCard() {
        return configuration.get().getGenerateSDCard();
    }

    private boolean isUsingSystemSDCard() {
        return configuration.get().getSdCard() == null;
    }

    /**
     * Finds out some random string in order to provide some name for SD card.
     *
     * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
     */
    private static final class IdentifierGenerator {

        private static final int NUM_BITS = 130;

        private static final int RADIX = 30;

        private static final SecureRandom random = new SecureRandom();

        private static String getRandomSDCardName() {
            return new BigInteger(NUM_BITS, random).toString(RADIX).concat(".img");
        }
    }

}
