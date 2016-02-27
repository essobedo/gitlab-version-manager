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

import java.util.Comparator;

/**
 * The configuration needed to access to a gitlab repository.
 *
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
public interface ConnectionConfiguration {
    /**
     * Gives the login of gitlab to use to access to the repository.
     * @return the login.
     */
    String login();
    /**
     * Gives the password of gitlab to use to access to the repository.
     * @return the password.
     */
    String password();
    /**
     * Gives the owner of the gitlab project to which we want to access.
     * @return the owner of the gitlab project.
     */
    String projectOwner();
    /**
     * Gives the id of the gitlab project to which we want to access.
     * @return the id of the gitlab project.
     */
    String projectId();
    /**
     * Gives the name of the gitlab project to which we want to access.
     * @return the name of the gitlab project.
     */
    String projectName();
    /**
     * Gives the name of the branch in which the releases are stored.
     * @return the name of the branch.
     */
    String branch();
    /**
     * Gives the name of the file that will be considered as the patch.
     * @return the name of the patch.
     */
    String patchFileName();
    /**
     * Gives the comparator of version ids to use to be able to identify the latest version.
     * @return the comparator of version ids.
     */
    default Comparator<String> versionComparator() {
        return (version1, version2) -> {
            if (version1.startsWith(version2) && version1.endsWith("-SNAPSHOT")) {
                return -1;
            } else if (version2.startsWith(version1) && version2.endsWith("-SNAPSHOT")) {
                return 1;
            }
            return version1.compareTo(version2);
        };
    }
}
