/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.arquillian.container.android.managed.configuration;

import java.io.File;
import java.io.IOException;

/**
 * Simple validation utility
 *
 * @author <a href="@mailto:kpiwko@redhat.com">Karel Piwko</a>
 * @author <a href="@mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class Validate {

    /**
     * Minimal number of console port.
     */
    private static final long CONSOLE_PORT_MIN = 5554;

    /**
     * Maximal number of console port.
     */
    private static final long CONSOLE_PORT_MAX = 5584;

    /**
     * Minimal number of adb port.
     */
    private static final long ADB_PORT_MIN = 5555;

    /**
     * Maximal number of adb port.
     */
    private static final long ADB_PORT_MAX = 5585;

    /**
     *
     * @param object object to check against nullity
     * @param message the exception message
     * @throws IllegalStateException if object is null
     */
    public static void stateNotNull(Object object, String message) throws IllegalStateException {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Checks that object is not null, throws exception if it is.
     *
     * @param obj
     *        The object to check
     * @param message
     *        The exception message
     * @throws IllegalArgumentException
     *         Thrown if obj is null
     */
    public static void notNull(final Object obj, final String message) throws IllegalArgumentException {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that the specified String is not null or empty, throws exception if it is.
     *
     * @param string
     *        The object to check
     * @param message
     *        The exception message
     * @throws IllegalArgumentException
     *         Thrown if string is null
     */
    public static void notNullOrEmpty(final String string, final String message) throws IllegalArgumentException {
        if (string == null || string.length() == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that at least one of specified String is not empty
     *
     * @param strings
     *        The array of strings to be checked
     * @param message
     *        The exception message
     * @throws AndroidConfigurationException
     *         Throws if all strings are null or empty
     */
    public static void notAllNullsOrEmpty(final String[] strings, final String message) throws IllegalArgumentException {
        for (String string : strings) {
            if (string != null && string.trim().length() != 0) {
                return;
            }
        }

        throw new IllegalArgumentException(message);
    }

    /**
     * Checks that the specified String is not null or empty and represents a readable file, throws exception if it is
     * empty or null and does not represent a path to a file.
     *
     * @param path
     *        The path to check
     * @param message
     *        The exception message
     * @throws IllegalArgumentException
     *         Thrown if path is empty, null or invalid
     */
    public static void isReadable(final String path, String message) throws IllegalArgumentException {
        notNullOrEmpty(path, message);
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that the specified String is not null or empty and represents a readable directory, throws exception if it
     * is empty or null and does not represent a path to a directory.
     *
     * @param path
     *        The path to check
     * @param message
     *        The exception message
     * @throws IllegalArgumentException
     *         Thrown if path is empty, null or invalid
     */
    public static void isReadableDirectory(final String path, String message) throws IllegalArgumentException {
        notNullOrEmpty(path, message);
        File file = new File(path);
        isReadableDirectory(file, message);
    }

    /**
     * Checks that the specified File is not null or empty and represents a readable file, throws exception if it is
     * empty or null and does not represent a path to a file.
     *
     * @param file
     *        The file to check
     * @param message
     *        The exception message
     * @throws IllegalArgumentException
     *         Thrown if file is null or invalid
     */
    public static void isReadable(final File file, String message) throws IllegalArgumentException {
        if (file == null) {
            throw new IllegalArgumentException(message);
        }
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that the specified file is not null and represents a readable directory, throws exception if it is empty
     * or null and does not represent a directory.
     *
     * @param file
     *        The path to check
     * @param message
     *        The exception message
     * @throws IllegalArgumentException
     *         Thrown if file is null or invalid
     */
    public static void isReadableDirectory(final File file, String message) throws IllegalArgumentException {
        if (file == null) {
            throw new IllegalArgumentException(message);
        }
        if (!file.exists() || !file.isDirectory() || !file.canRead() || !file.canExecute()) {
            throw new IllegalArgumentException(message);
        }

    }

    /**
     * Checks if user set size of SD card in the propper format.
     *
     * SD card size has to be between 9M (9126K) and 1023G. Everything out of this range is considered to be invalid. This
     * method follows size recommendation of mksdcard tool from the Android tools distribution.
     *
     * @param sdSize
     *        size of sd card
     * @param message
     *        The exception message
     * @throws AndroidContainerConfigurationException when sdSize is invalid
     */
    public static void sdSize(String sdSize, String message) throws AndroidContainerConfigurationException {
        if (sdSize == null || sdSize.trim().equals("") || !(sdSize.trim().length() >= 2) || !sdSize.matches("^[1-9]{1}[0-9]*[KGM]{1}$")) {
            throw new AndroidContainerConfigurationException(message);
        }

        String sizeString = sdSize.substring(0, sdSize.length()-1);
        String sizeUnit = sdSize.substring(sdSize.length()-1);

        long size;

        try {
            size = Long.parseLong(sizeString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse '" + sizeString + "' to number.");
        }

        if ((size < 9126 && sizeUnit.equals("K")) || (size < 9 && sizeUnit.equals("M"))) {
            throw new AndroidContainerConfigurationException(message);
        }

    }

    /**
     * Checks if a file is writable
     *
     * @param file file to check against writability
     * @param message The exception message
     */
    public static void isWritable(File file, String message) throws AndroidContainerConfigurationException {
        if (file == null) {
            throw new IllegalArgumentException(message);
        }

        if (file.exists()) {
            if (file.canWrite()) {
                return;
            }
            else {
                throw new AndroidContainerConfigurationException(message);
            }
        }

        try {
            file.createNewFile();
            file.delete();
        } catch (IOException e) {
            throw new AndroidContainerConfigurationException(message);
        }
    }

    /**
     * Checks if console port is in valid range
     *
     * @param consolePort console port to check validity of
     * @throws AndroidContainerConfigurationException if console port is null or not a number or not valid
     */
    public static void isConsolePortValid(String consolePort) throws AndroidContainerConfigurationException {
        if (consolePort == null) {
            throw new AndroidContainerConfigurationException("Console port is null!");
        }

        try {
            long port = Long.parseLong(consolePort);
            if (!(port >= CONSOLE_PORT_MIN && port <= CONSOLE_PORT_MAX && port % 2 == 0)) {
                throw new AndroidContainerConfigurationException(
                        "Console port is not in the right range or it is not an even number. It has to be in the range "
                                + CONSOLE_PORT_MIN
                                + "-" + CONSOLE_PORT_MAX + ".");
            }
        } catch (NumberFormatException e) {
            throw new AndroidContainerConfigurationException(
                    "Unable to get console port from port number '" + consolePort + "'.");
        }
    }

    /**
     * Checks if adb port is in valid range
     *
     * @param adb adb port to check validity of
     * @throws AndroidContainerConfigurationException if adb port is null or not a number or not valid
     */
    public static void isAdbPortValid(String adbPort) throws AndroidContainerConfigurationException {
        if (adbPort == null) {
            throw new AndroidContainerConfigurationException("Adb port is null!");
        }

        try {
            long port = Long.parseLong(adbPort);
            if (!(port >= ADB_PORT_MIN && port <= ADB_PORT_MAX && port % 2 == 1)) {
                throw new AndroidContainerConfigurationException(
                        "Adb port is not in the right range or it is not an odd number. It has to be in the range "
                                + ADB_PORT_MIN
                                + "-" + ADB_PORT_MAX + ".");
            }
        } catch (NumberFormatException e) {
            throw new AndroidContainerConfigurationException(
                    "Unable to get adb port from port number '" + adbPort + "'.");
        }
    }

    /**
     * Checks if file name of the SD card is valid which means it has to have suffix of ".img".
     *
     * @param fileName name of the file to check validity of
     * @param message
     */
    public static void sdCardFileName(String fileName, String message) throws AndroidContainerConfigurationException {
        String[] tokens = fileName.split(".");
        if (tokens.length != 2 || !tokens[1].equals("img")) {
            throw new AndroidContainerConfigurationException(message);
        }
    }

}
