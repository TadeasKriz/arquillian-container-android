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
package org.jboss.arquillian.container.android.managed.configuration;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * A {@link org.jboss.arquillian.spi.client.container.ContainerConfiguration} implementation for the Android containers.
 *
 * @author <a href="mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 */
public class AndroidManagedContainerConfiguration implements ContainerConfiguration {

    private static final Logger logger = Logger.getLogger(AndroidManagedContainerConfiguration.class.getName());

    private boolean skip;

    private boolean forceNewBridge;

    private String serialId;

    private String avdName;

    private String generatedAvdPath = "/tmp/";

    private String emulatorOptions;

    private String sdSize = "128M";

    private String sdCard;

    private String sdCardLabel;

    private boolean generateSDCard;

    private String abi;

    private long emulatorBootupTimeoutInSeconds = 120L;

    private long emulatorShutdownTimeoutInSeconds = 60L;

    private String home = System.getenv("ANDROID_HOME");

    private boolean avdGenerated;

    private boolean SdCardFileNameGenerated;

    private String consolePort;

    private String adbPort;

    // Android 2.3.3 is the default
    private String apiLevel = "10";

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public String getAvdName() {
        return avdName;
    }

    public void setAvdName(String avdName) {
        this.avdName = avdName;
    }

    public void setGeneratedAvdPath(String generatedAvdPath) {
        this.generatedAvdPath = generatedAvdPath;
    }

    public String getGeneratedAvdPath() {
        return this.generatedAvdPath;
    }

    public String getSerialId() {
        return serialId;
    }

    public void setSerialId(String serialId) {
        this.serialId = serialId;
    }

    public String getEmulatorOptions() {
        return emulatorOptions;
    }

    public void setEmulatorOptions(String emulatorOptions) {
        this.emulatorOptions = emulatorOptions;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public boolean isForceNewBridge() {
        return forceNewBridge;
    }

    public void setForceNewBridge(boolean force) {
        this.forceNewBridge = force;
    }

    public long getEmulatorBootupTimeoutInSeconds() {
        return emulatorBootupTimeoutInSeconds;
    }

    public void setEmulatorBootupTimeoutInSeconds(long emulatorBootupTimeoutInSeconds) {
        this.emulatorBootupTimeoutInSeconds = emulatorBootupTimeoutInSeconds;
    }

    public String getApiLevel() {
        return apiLevel;
    }

    public void setApiLevel(String apiLevel) {
        this.apiLevel = apiLevel;
    }

    public String getSdSize() {
        return sdSize;
    }

    public void setSdSize(String sdSize) {
        this.sdSize = sdSize;
    }

    public String getSdCard() {
        return sdCard;
    }

    public String getSdCardLabel() {
        return sdCardLabel;
    }

    public void setSdCardLabel(String sdCardLabel) {
        this.sdCardLabel = sdCardLabel;
    }

    public boolean getGenerateSDCard() {
        return this.generateSDCard;
    }

    public void setGenerateSDCard(boolean generate) {
        this.generateSDCard = generate;
    }

    public void setSdCard(String sdCard) {
        this.sdCard = sdCard;
    }

    public long getEmulatorShutdownTimeoutInSeconds() {
        return emulatorShutdownTimeoutInSeconds;
    }

    public void setEmulatorShutdownTimeoutInSeconds(long emulatorShutdownTimeoutInSeconds) {
        this.emulatorShutdownTimeoutInSeconds = emulatorShutdownTimeoutInSeconds;
    }

    public String getAbi() {
        return abi;
    }

    public void setAbi(String abi) {
        this.abi = abi;
    }

    public boolean isAVDGenerated() {
        return avdGenerated;
    }

    public void setAvdGenerated(boolean generated) {
        this.avdGenerated = generated;
    }

    public boolean getAvdGenerated() {
        return this.avdGenerated;
    }

    public boolean isSdCardFileNameGenerated() {
        return SdCardFileNameGenerated;
    }

    public void setSdCardFileNameGenerated(boolean SdCardFileNameGenerated) {
        this.SdCardFileNameGenerated = SdCardFileNameGenerated;
    }

    public String getConsolePort() {
        return consolePort;
    }

    public void setConsolePort(String consolePort) {
        this.consolePort = consolePort;
    }

    public String getAdbPort() {
        return adbPort;
    }

    public void setAdbPort(String adbPort) {
        this.adbPort = adbPort;
    }

    @Override
    public void validate() throws AndroidContainerConfigurationException {
        Validate.isReadableDirectory(home,
                "You must provide Android SDK home directory. The value you've provided is not valid ("
                        + (home == null ? "" : home)
                        + "). You can either set it via an environment variable ANDROID_HOME or via"
                        + " a property called \"home\" in Arquillian configuration.");

        if (avdName != null && serialId != null) {
            logger.warning("Both \"avdName\" and \"serialId\" properties are defined, the device "
                    + "specified by \"serialId\" will get priority if connected.");
        }

        if (avdName == null && serialId == null && consolePort == null) {
            logger.severe("All \"avdName\", \"serialId\" and \"consolePort\" are not defined.");
            throw new AndroidContainerConfigurationException(
                    "All \"avdName\", \"serialId\" and \"consolePort\" are not defined.");
        }

        if (generatedAvdPath != null) {
            Validate.isWritable(new File(generatedAvdPath), "Path you want to store generated AVD is not writable!");
        }

        if (consolePort != null) {
            Validate.isConsolePortValid(consolePort);
        }

        if (adbPort != null) {
            Validate.isAdbPortValid(adbPort);
        }

        if (sdCard != null) {
            File sdCardFile = new File(sdCard);
            Validate.isReadableDirectory(sdCardFile.getParentFile(),
                    "Directory of the sd card for '" + avdName + "' is not readable.");
            Validate.isWritable(sdCardFile, "Location of the SD card for the Android container '" + avdName
                    + "' is not writable.");
            Validate.sdCardFileName(sdCardFile.getName(), "File name of sd card to create '" + sdCardFile.getName()
                    + "' does not have '.img' suffix.");
        }

        if (sdCardLabel != null) {
            Validate.notNullOrEmpty(sdCardLabel, "SD card label can not be the empty string");
        }

        if (sdSize != null) {
            Validate.sdSize(sdSize, "Check you did specify your sdSize property in arquillian.xml properly.");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\navdName\t\t\t:").append(this.avdName).append("\n");
        sb.append("generatedAvdPath\t:").append(this.generatedAvdPath).append("\n");
        sb.append("apiLevel\t\t:").append(this.apiLevel).append("\n");
        sb.append("serialId\t\t:").append(this.serialId).append("\n");
        sb.append("force\t\t\t:").append(this.forceNewBridge).append("\n");
        sb.append("skip\t\t\t:").append(this.skip).append("\n");
        sb.append("sdCard\t\t\t:").append(this.sdCard).append("\n");
        sb.append("sdSize\t\t\t:").append(this.sdSize).append("\n");
        sb.append("generateSD\t\t:").append(this.generateSDCard).append("\n");
        sb.append("abi\t\t\t:").append(this.abi).append("\n");
        sb.append("emuBoot\t\t\t:").append(this.emulatorBootupTimeoutInSeconds).append("\n");
        sb.append("emuShut\t\t\t:").append(this.emulatorShutdownTimeoutInSeconds).append("\n");
        sb.append("emuOpts\t\t\t:").append(this.emulatorOptions).append("\n");
        sb.append("home\t\t\t:").append(this.home).append("\n");
        return sb.toString();
    }

}
