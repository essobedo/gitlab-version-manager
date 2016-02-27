/*
 * Copyright (C) 2016 essobedo.
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
package com.github.essobedo.gitlabvm;

/**
 * The class that manages the internationalization of the message to show to the end-users.
 *
 * @author <a href="mailto:nicolas.filotto@gmail.com">Nicolas Filotto</a>
 * @version $Id$
 */
final class Localization {

    /**
     * A singleton containing all the messages of the application.
     */
    private static final com.github.essobedo.appma.i18n.Localization INSTANCE = new
        com.github.essobedo.appma.i18n.Localization("gitlabvm.i18n.messages");

    /**
     * Default constructor.
     */
    private Localization() {
    }

    /**
     * Gives the messages corresponding to the specified key using the given parameters.
     *
     * @param key The key of the message to retrieve.
     * @param params The parameters to use to construct the message.
     * @return The message internationalized.
     */
    public static String getMessage(final String key, final Object... params) {
        return INSTANCE.getLocalizedMessage(key, params);
    }
}
