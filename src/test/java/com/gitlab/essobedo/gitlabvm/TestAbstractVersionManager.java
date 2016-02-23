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

import com.gitlab.essobedo.appma.core.Configuration;
import com.gitlab.essobedo.appma.exception.ApplicationException;
import com.gitlab.essobedo.appma.spi.Manageable;
import com.gitlab.essobedo.appma.task.Task;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.Properties;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
public class TestAbstractVersionManager {

    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8880";

    private HttpServer server;
    private Properties properties;
    private VersionManager versionManager;

    @Before
    public void init() throws Exception {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig().packages("com.gitlab.essobedo.gitlabvm");
        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
        server.start();
        this.properties = new Properties();
        properties.put("branch", "branch1");
        properties.put("projectId", "123456");
        properties.put("projectName", "project3");
        properties.put("file", "patch.properties");
        properties.put("owner", "owner2");
        this.versionManager = new VersionManager(properties);
    }

    @After
    public void end() {
        server.shutdown();
    }

    @Test
    public void testCheck() throws Exception {
        Manageable application = mock(Manageable.class);
        when(application.version()).thenReturn("1.0");
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }

        properties.put("login", "esso/bedo");
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("password", "foo");
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }

        properties.put("password", ":\\/");
        properties.put("login", "esso/bedo3");
        this.versionManager = new VersionManager(properties);
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("login", "esso/bedo");
        this.versionManager = new VersionManager(properties);
        assertEquals("1.0.2", versionManager.check(application).execute());
        when(application.version()).thenReturn("1.0.2");
        assertNull(versionManager.check(application).execute());
        properties.put("login", "foo");
        assertNull(versionManager.check(application).execute());
        properties.put("projectId", "1");
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("projectId", "123456");
        assertNull(versionManager.check(application).execute());
        properties.put("branch", "branch2");
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("branch", "branch1");
        versionManager.check(application).execute();
        properties.put("login", "esso/bedo");
        this.versionManager = new VersionManager(properties);
        assertNull(versionManager.check(application).execute());
        when(application.version()).thenReturn("1.0.5-SNAPSHOT");
        assertNull(versionManager.check(application).execute());
        properties.put("login", "esso/bedo2");
        assertNull(versionManager.check(application).execute());
        this.versionManager = new VersionManager(properties);
        try {
            versionManager.check(application).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
    }

    @Test
    public void testStore() throws Exception {
        properties.put("login", "esso/bedo");
        properties.put("password", ":\\/");
        Manageable application = mock(Manageable.class);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        versionManager.store(application, byteArrayOutputStream).execute();
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        assertEquals("value1", p.getProperty("key1"));
        assertEquals("value2", p.getProperty("key2"));
        assertEquals("value3", p.getProperty("key3"));
        assertEquals(3, p.size());
        properties.put("owner", "foo");
        try {
            versionManager.store(application, byteArrayOutputStream).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("owner", "owner2");
        versionManager.store(application, byteArrayOutputStream).execute();
        properties.put("projectName", "foo");
        try {
            versionManager.store(application, byteArrayOutputStream).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("projectName", "project3");
        versionManager.store(application, byteArrayOutputStream).execute();
        properties.put("branch", "foo");
        try {
            versionManager.store(application, byteArrayOutputStream).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("branch", "branch1");
        versionManager.store(application, byteArrayOutputStream).execute();
        properties.put("file", "foo");
        try {
            versionManager.store(application, byteArrayOutputStream).execute();
            fail("An ApplicationException was expected");
        } catch (ApplicationException e) {
            // expected
        }
        properties.put("file", "patch.properties");
        versionManager.store(application, byteArrayOutputStream).execute();
    }

    @Path("/")
    public static class EndPoints {
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Path("api/v3/session")
        public Response getToken(@QueryParam("login") String login, @QueryParam("password") String password)
                        throws Exception {
            if (":\\/".equals(password)) {
                if ("esso/bedo".equals(login)) {
                    return Response.ok(getContent("/tokenOK.json")).build();
                } else if ("esso/bedo2".equals(login)) {
                    return Response.ok(getContent("/tokenOK2.json")).build();
                } else if ("esso/bedo3".equals(login)) {
                    return Response.ok(getContent("/tokenOK3.json")).build();
                }
            }
            return Response.status(401).entity(getContent("/tokenKO.json")).build();
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("api/v3/projects/{project-id}/repository/tree")
        public Response getVersions(@PathParam("project-id") String projectId,
                                    @QueryParam("private_token") String token,
                                    @QueryParam("ref_name") String branch)  throws Exception {
            if (!"123456".equals(projectId)) {
                return Response.status(404).entity(getContent("/versionsKOProject.json")).build();
            } else if (!"kaC25JPG1Evrpbdy3EGy".equals(token)) {
                return Response.status(401).entity(getContent("/versionsKOToken.json")).build();
            } else if (!"branch1".equals(branch)) {
                return Response.status(404).entity(getContent("/versionsKOBranch.json")).build();
            }
            return Response.ok(getContent("/versionsOK.json")).build();
        }

        @GET
        @Path("{owner}/{project}/raw/{branch}/{version}/{file:.*}")
        public Response getPatch(@PathParam("owner") String owner, @PathParam("project") String project,
                                 @PathParam("branch") String branch, @PathParam("version") String version,
                                 @PathParam("file") String file, @QueryParam("private_token") String token)
                    throws Exception {
            if (!"owner2".equals(owner) || !"project3".equals(project) || !"kaC25JPG1Evrpbdy3EGy".equals(token)) {
                return Response.status(302).type(MediaType.TEXT_HTML_TYPE).entity(getContent("/patchKOToken.html"))
                    .build();
            } else if (!"branch1".equals(branch) || !"1.0.2".equals(version) || !"patch.properties".equals(file)) {
                return Response.status(404).type(MediaType.TEXT_HTML_TYPE).entity(getContent("/patchKOPath.html"))
                    .build();
            }
            return Response.ok(getContent("/patch.properties"), MediaType.TEXT_PLAIN_TYPE).build();
        }

        private static byte[] getContent(String path) throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (InputStream is = EndPoints.class.getResourceAsStream(path)) {
                byte[] buffer = new byte[512];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
            }
            return result.toByteArray();
        }
    }

    private static class VersionManager extends AbstractVersionManager {

        private final Properties properties;

        private VersionManager(final Properties properties) {
            super(BASE_URI);
            this.properties = properties;
        }

        @Override
        public Task<Configuration> upgrade(File upgradeRoot, File appRoot, String oldVersion) throws ApplicationException {
            throw new UnsupportedOperationException("#upgrade()");
        }

        @Override
        protected ConnectionConfiguration createConfiguration(final Manageable manageable) {
            return new ConnectionConfiguration() {
                @Override
                public String branch() {
                    return properties.getProperty("branch");
                }

                @Override
                public String password() {
                    return properties.getProperty("password");
                }

                @Override
                public String login() {
                    return properties.getProperty("login");
                }

                @Override
                public Comparator<String> versionComparator() {
                    return String::compareTo;
                }

                @Override
                public String projectId() {
                    return properties.getProperty("projectId");
                }

                @Override
                public String patchFileName() {
                    return properties.getProperty("file");
                }

                @Override
                public String projectName() {
                    return properties.getProperty("projectName");
                }

                @Override
                public String projectOwner() {
                    return properties.getProperty("owner");
                }
            };
        }
    }
}
