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

import com.github.essobedo.appma.exception.ApplicationException;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.goebl.david.WebbException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The class allowing to access to gitlab using the Gitlab API.
 *
 * @author Nicolas Filotto (nicolas.filotto@gmail.com)
 * @version $Id$
 * @since 1.0
 */
final class Repository {

    /**
     * The HTTP code in case of an OK.
     */
    private static final int OK_CODE = 200;
    /**
     * The min value of the HTTP codes from which we consider the response as a success.
     */
    private static final int MIN_SUCCESS_CODE = 400;
    /**
     * The encoding used to URL encode the parameters.
     */
    private static final String ENCODING = "UTF-8";
    /**
     * The private token.
     */
    private String token;
    /**
     * The {@link Webb} instance allowing to access to gitlab thanks to the http/https protocol.
     */
    private final Webb webb;
    /**
     * The configuration to use to access to gitlab.
     */
    private final ConnectionConfiguration configuration;

    /**
     * Constructs a {@code Repository} with the specified end point and configuration.
     * @param endpoint The end point of the gitlab repository to access.
     * @param configuration the configuration to use to access to gitlab.
     * @throws ApplicationException in case the configuration is not valid.
     */
    Repository(final String endpoint, final ConnectionConfiguration configuration)
        throws ApplicationException {
        if (configuration.login() == null || configuration.login().isEmpty()
            || configuration.password() == null || configuration.password().isEmpty()) {
            throw new ApplicationException("The login and/or password cannot be empty");
        }
        this.configuration = configuration;
        this.webb = Webb.create();
        webb.setBaseUri(endpoint);
    }

    /**
     * Gives the comparator of version ids to use to be able to identify the latest version.
     * @return the comparator of version ids.
     */
    Comparator<String> versionComparator() {
        return this.configuration.versionComparator();
    }

    /**
     * Checks if the private token has already been retrieved.
     * @return {@code true} if the private token has already been retrieved, {@code false} otherwise.
     */
    private boolean hasToken() {
        return token != null;
    }

    /**
     * Retrieves the private token to use to acces to the gitlab repository.
     * @return the private token to use to acces to the gitlab repository.
     * @throws ApplicationException if the private token could not be retrieved.
     */
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
        } catch (UnsupportedEncodingException | WebbException e) {
            throw new ApplicationException("Could not get the private token", e);
        }

        final JSONObject body = response.getBody();
        try {
            if (response.getStatusCode() >= MIN_SUCCESS_CODE) {
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

    /**
     * Gives the list of versions available in the repository ordered using version ids comparator.
     * @return the of versions available.
     * @throws ApplicationException if the list of versions could not be retrieved.
     */
    SortedSet<String> getVersions() throws ApplicationException {
        final String token = findToken();
        final Response<JSONArray> response;
        try {
            response = webb
                .get(String.format("/api/v3/projects/%s/repository/tree", configuration.projectId()))
                .param("private_token", token)
                .param("ref_name", configuration.branch())
                .asJsonArray();
        } catch (WebbException e) {
            throw new ApplicationException(String.format("Could not access to the versions of the project '%s",
                configuration.projectId()), e);
        }
        if (response.getStatusCode() >= MIN_SUCCESS_CODE) {
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

    /**
     * Gets the content of the patch for the specified version id.
     * @param version the version id for which we want the content of the patch.
     * @return the content of the patch.
     * @throws ApplicationException if the content of the patch could not be found.
     */
    InputStream getPatch(final String version) throws ApplicationException {
        final String token = findToken();
        final Response<InputStream> response;
        try {
            response = webb
                .get(String.format("/%s/%s/raw/%s/%s/%s",
                    URLEncoder.encode(configuration.projectOwner(), Repository.ENCODING),
                    URLEncoder.encode(configuration.projectName(), Repository.ENCODING),
                    URLEncoder.encode(configuration.branch(), Repository.ENCODING),
                    URLEncoder.encode(version, Repository.ENCODING),
                    URLEncoder.encode(configuration.patchFileName(), Repository.ENCODING)))
                .param("private_token", token)
                .followRedirects(false)
                .asStream();
        } catch (UnsupportedEncodingException | WebbException e) {
            throw new ApplicationException(String.format("Could not access to the file '%s",
                configuration.patchFileName()), e);
        }
        if (response.getStatusCode() != OK_CODE) {
            throw new ApplicationException(String.format(
                "Could not access to the file '%s due to the error: %s",
                configuration.patchFileName(), response.getResponseMessage()));
        }
        return response.getBody();
    }
}
