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

import com.gitlab.essobedo.appma.exception.ApplicationException;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
final class Repository {

    private static final String ENCODING = "UTF-8";

    private String token;

    private final Webb webb;
    private final ConnectionConfiguration configuration;

    Repository(final String endpoint, final ConnectionConfiguration configuration)
        throws ApplicationException {
        if (configuration.login() == null || configuration.login().isEmpty() ||
            configuration.password() == null || configuration.password().isEmpty()) {
            throw new ApplicationException("The login and/or password cannot be empty");
        }
        this.configuration = configuration;
        this.webb = Webb.create();
        webb.setBaseUri(endpoint);
    }

    private boolean hasToken() {
        return token != null;
    }

    private String findToken() throws ApplicationException {
        if (hasToken()) {
            return token;
        }

        final Response<JSONObject> response;
        try {
            response = webb
                 .post(String.format("/api/v3/session?login=%s&password=%s",
                     URLEncoder.encode(configuration.login(), Repository.ENCODING),
                     URLEncoder.encode(configuration.password(), Repository.ENCODING)))
                 .asJsonObject();
        } catch (UnsupportedEncodingException e) {
            throw new ApplicationException("Could not get the private token", e);
        }

        final JSONObject body = response.getBody();
        try {
            if (response.getStatusCode() >= 400) {
                throw new ApplicationException(String.format("Could not connect to the server due to the error: %s",
                    response.getResponseMessage()));
            }
            this.token = body.getString("private_token");
        } catch (JSONException e) {
            throw new ApplicationException("Could not extract the private token", e);
        }
        if (!hasToken()) {
            throw new ApplicationException("No private token could be found");
        }
        return token;
    }

    SortedSet<String> getVersions() throws ApplicationException {
        final String token = findToken();
        final Response<JSONArray> response = webb
            .get(String.format("/api/v3/projects/%s/repository/tree", configuration.projectId()))
            .param("private_token", token)
            .param("ref_name", configuration.branch())
            .asJsonArray();
        if (response.getStatusCode() >= 400) {
            throw new ApplicationException(String.format(
                "Could not find the versions of the project '%s' in the branch '%s' due to the error: %s",
                configuration.projectId(),
                configuration.branch(),
                response.getResponseMessage()));
        }
        final JSONArray body = response.getBody();
        final Comparator<String> comparator = configuration.versionComparator();
        final SortedSet<String> result = new TreeSet<>(comparator);
        for (int i = 0; i < body.length(); i++) {
            try {
                final JSONObject jsonObject = body.getJSONObject(i);
                result.add(jsonObject.getString("name"));
            } catch (JSONException e) {
                throw new ApplicationException("Could not extract the versions", e);
            }
        }
        return result;
    }

    InputStream getPatch(final String version) throws ApplicationException {
        final String token = findToken();
        final URL url;
        try {
            url = new URL(String.format("%s/%s/%s/raw/%s/%s/%s?private_token=%s",
                webb.getBaseUri(),
                URLEncoder.encode(configuration.projectOwner(), Repository.ENCODING),
                URLEncoder.encode(configuration.projectName(), Repository.ENCODING),
                URLEncoder.encode(configuration.branch(), Repository.ENCODING),
                URLEncoder.encode(version, Repository.ENCODING),
                URLEncoder.encode(configuration.patchFileName(), Repository.ENCODING),
                URLEncoder.encode(token, Repository.ENCODING)));
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            throw new ApplicationException(String.format("Could not access to the file '%s",
                configuration.patchFileName()), e);
        }
        try {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != 200) {
                throw new ApplicationException(String.format(
                    "Could not access to the file '%s due to the error: %s",
                    configuration.patchFileName(), connection.getResponseMessage()));
            }
            return connection.getInputStream();
        } catch (IOException e) {
            throw new ApplicationException(String.format("Could not read to the file '%s",
                configuration.patchFileName()), e);
        }
    }
}
