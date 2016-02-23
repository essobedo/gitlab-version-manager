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
package com.gitlab.essobedo.gitlabvm;

import java.util.Comparator;

/**
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
public interface ConnectionConfiguration {
    String branch();
    String password();
    String login();
    default Comparator<String> versionComparator() {
        return (s1, s2) -> {
            if (s1.startsWith(s2) && s1.endsWith("-SNAPSHOT")) {
                return -1;
            } else if (s2.startsWith(s1) && s2.endsWith("-SNAPSHOT")) {
                return 1;
            }
            return s1.compareTo(s2);
        };
    }
    String projectId();
    String patchFileName();
    String projectName();
    String projectOwner();
}
