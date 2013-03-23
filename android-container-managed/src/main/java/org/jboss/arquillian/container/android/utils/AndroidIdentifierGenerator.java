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

package org.jboss.arquillian.container.android.utils;

import java.util.UUID;

import org.jboss.arquillian.container.android.api.IdentifierGenerator;
import org.jboss.arquillian.container.android.api.IdentifierGeneratorException;

/**
 * Generates random identifier.
 *
 * @author <a href="smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class AndroidIdentifierGenerator implements IdentifierGenerator {

    private String sdCardSuffix = ".img";

    @Override
    public String getIdentifier(Class<?> identifierType) {
        String uuid = UUID.randomUUID().toString();

        if (identifierType.isInstance(IdentifierType.AVD)) {
            return uuid;
        }
        if (identifierType.isInstance(IdentifierType.SD_CARD)) {
            return uuid + sdCardSuffix;
        }
        if (identifierType.isInstance(IdentifierType.SD_CARD_LABEL)) {
            return uuid;
        }
        throw new IdentifierGeneratorException("Not possible to generate any identifier of type " + identifierType.getName());
    }

    /**
     * Sets suffix of SD card file name.
     *
     * @param suffix suffix of SD card file
     * @return instance of this {@link AndroidIdentifierGenerator}
     */
    public AndroidIdentifierGenerator setSdCardSuffix(String suffix) {
        if (suffix == null || suffix.trim().equals("")) {
            return this;
        }

        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }

        sdCardSuffix = suffix;

        return this;
    }

}
