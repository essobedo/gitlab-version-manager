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
    @SuppressWarnings("PMD.NPathComplexity")
    default Comparator<String> versionComparator() {
        return (version1, version2) -> {
            final String[] versions1 = version1.split("\\.");
            final String[] versions2 = version2.split("\\.");
            final String defaultValue = "*";
            final String snapshot = "-SNAPSHOT";
            int result = 0;
            for (int i = 0, length = Math.max(versions1.length, versions2.length); result == 0 && i < length; i++) {
                String vElement1 = i < versions1.length ? versions1[i] : defaultValue;
                String vElement2 = i < versions2.length ? versions2[i] : defaultValue;
                final boolean snapshot1 = vElement1.endsWith(snapshot);
                if (snapshot1) {
                    vElement1 = vElement1.substring(0, vElement1.length() - snapshot.length() + 1);
                } else {
                    vElement1 += "_";
                }
                final boolean snapshot2 = vElement2.endsWith(snapshot);
                if (snapshot2) {
                    vElement2 = vElement2.substring(0, vElement2.length() - snapshot.length() + 1);
                } else {
                    vElement2 += "_";
                }
                if (vElement1.length() != vElement2.length()) {
                    final int targetLength = Math.max(vElement1.length(), vElement2.length());
                    for (int j = vElement1.length(); j < targetLength; j++) {
                        vElement1 = defaultValue + vElement1;
                    }
                    for (int j = vElement2.length(); j < targetLength; j++) {
                        vElement2 = defaultValue + vElement2;
                    }
                }
                result = vElement1.compareTo(vElement2);
            }
            return result;
        };
    }
}
