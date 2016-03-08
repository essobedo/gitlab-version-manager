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

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
public class TestConnectionConfiguration {

    @Test
    public void testVersionComparator() {
        ConnectionConfiguration config = new ConnectionConfiguration() {

            @Override
            public String branch() {
                throw new UnsupportedOperationException("#branch()");
            }

            @Override
            public String password() {
                throw new UnsupportedOperationException("#password()");
            }

            @Override
            public String login() {
                throw new UnsupportedOperationException("#login()");
            }

            @Override
            public String projectId() {
                throw new UnsupportedOperationException("#projectId()");
            }

            @Override
            public String patchFileName() {
                throw new UnsupportedOperationException("#patchFileName()");
            }

            @Override
            public String projectName() {
                throw new UnsupportedOperationException("#projectName()");
            }

            @Override
            public String projectOwner() {
                throw new UnsupportedOperationException("#projectOwner()");
            }
        };
        assertTrue(config.versionComparator().compare("1.0", "1.0") == 0);

        assertTrue(config.versionComparator().compare("1.0", "1.0.1") < 0);
        assertTrue(config.versionComparator().compare("1.0", "1.0.2") < 0);
        assertTrue(config.versionComparator().compare("1.0.1", "1.0.2") < 0);
        assertTrue(config.versionComparator().compare("1.0.1.1", "1.0.2") < 0);
        assertTrue(config.versionComparator().compare("1.0.1.1", "1.0.1.2") < 0);
        assertTrue(config.versionComparator().compare("1.0.1-SNAPSHOT", "1.0.2") < 0);
        assertTrue(config.versionComparator().compare("1.0.4", "1.0.5-SNAPSHOT") < 0);
        assertTrue(config.versionComparator().compare("1.0.5-SNAPSHOT", "1.0.6-SNAPSHOT") < 0);
        assertTrue(config.versionComparator().compare("1.0.5-SNAPSHOT", "1.0.5") < 0);
        assertTrue(config.versionComparator().compare("1.0.9", "1.0.10") < 0);
        assertTrue(config.versionComparator().compare("1.0.9", "1.0.10-SNAPSHOT") < 0);
        assertTrue(config.versionComparator().compare("1.9.9", "1.10.9") < 0);
        assertTrue(config.versionComparator().compare("1.0.5-SNAPSHOT", "1.0.5.1-SNAPSHOT") < 0);
        assertTrue(config.versionComparator().compare("1.0.5-SNAPSHOT", "1.0.5.1") < 0);
        assertTrue(config.versionComparator().compare("1.0-SNAPSHOT", "1.0.1") < 0);
        assertTrue(config.versionComparator().compare("1.0-SNAPSHOT", "1.0.1-SNAPSHOT") < 0);

        assertTrue(config.versionComparator().compare("1.0.1", "1.0") > 0);
        assertTrue(config.versionComparator().compare("1.0.2", "1.0") > 0);
        assertTrue(config.versionComparator().compare("1.0.2", "1.0.1") > 0);
        assertTrue(config.versionComparator().compare("1.0.2", "1.0.1.1") > 0);
        assertTrue(config.versionComparator().compare("1.0.1.2", "1.0.1.1") > 0);
        assertTrue(config.versionComparator().compare("1.0.2", "1.0.1-SNAPSHOT") > 0);
        assertTrue(config.versionComparator().compare("1.0.5-SNAPSHOT", "1.0.4") > 0);
        assertTrue(config.versionComparator().compare("1.0.6-SNAPSHOT", "1.0.5-SNAPSHOT") > 0);
        assertTrue(config.versionComparator().compare("1.0.5", "1.0.5-SNAPSHOT") > 0);
        assertTrue(config.versionComparator().compare("1.0.10", "1.0.9") > 0);
        assertTrue(config.versionComparator().compare("1.0.10-SNAPSHOT", "1.0.9") > 0);
        assertTrue(config.versionComparator().compare("1.10.9", "1.9.9") > 0);
        assertTrue(config.versionComparator().compare("1.0.5.1-SNAPSHOT", "1.0.5-SNAPSHOT") > 0);
        assertTrue(config.versionComparator().compare("1.0.5.1", "1.0.5-SNAPSHOT") > 0);
        assertTrue(config.versionComparator().compare("1.0.1", "1.0-SNAPSHOT") > 0);
        assertTrue(config.versionComparator().compare("1.0.1-SNAPSHOT", "1.0-SNAPSHOT") > 0);
    }
}
