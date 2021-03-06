/**
 * blackduck-docker-inspector
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.dockerinspector.httpclient;

import com.synopsys.integration.rest.client.IntHttpClient;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.synopsys.integration.blackduck.dockerinspector.httpclient.response.SimpleResponse;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpMethod;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.request.Response;

@Component
public class HttpRequestor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SimpleResponse executeGetBdioRequest(final IntHttpClient httpClient, final URI imageInspectorUri, final String containerPathToTarfile,
            final String givenImageRepo, final String givenImageTag, final String containerPathToContainerFileSystemFile,
        final boolean organizeComponentsByLayer,
        final boolean includeRemovedComponents,
        final boolean cleanup,
        final String platformTopLayerId)
            throws IntegrationException {
        final String url = new ImageInspectorUrlBuilder()
                .imageInspectorUri(imageInspectorUri)
                .containerPathToTarfile(containerPathToTarfile)
                .givenImageRepo(givenImageRepo)
                .givenImageTag(givenImageTag)
                .containerPathToContainerFileSystemFile(containerPathToContainerFileSystemFile)
                               .organizeComponentsByLayer(organizeComponentsByLayer)
                               .includeRemovedComponents(includeRemovedComponents)
                .cleanup(cleanup)
                               .platformTopLayerId(platformTopLayerId)
                .build();
        logger.debug(String.format("Doing a getBdio request on %s", url));
        final Request request = new Request.Builder(url).method(HttpMethod.GET).build();
        try (Response response = httpClient.execute(request)) {
            logger.debug(String.format("Response: HTTP status: %d", response.getStatusCode()));
            return new SimpleResponse(response.getStatusCode(), response.getHeaders(), getResponseBody(response));
        } catch (final IntegrationException ie) {
            if ((ie.getCause() != null) && (ie.getCause() instanceof java.net.SocketTimeoutException)) {
                logger.error(String.format("getBdio request on %s failed: The request to the image inspector service timed out. You might need to increase the value of the service timeout property.", url));
            } else {
                logger.error(String.format("getBdio request on %s failed: %s", url, ie.getMessage()));
            }
            throw ie;
        } catch (final Exception e) {
            logger.error(String.format("getBdio request on %s failed: %s", url, e.getMessage()));
            throw new IntegrationException(e);
        }
    }

    public String executeSimpleGetRequest(final IntHttpClient httpClient, final URI imageInspectorUri, String endpoint)
            throws IntegrationException {
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        final String url = String.format("%s/%s", imageInspectorUri.toString(), endpoint);
        logger.debug(String.format("Doing a GET on %s", url));
        final Request request = new Request.Builder(url).method(HttpMethod.GET).build();
        try (Response response = httpClient.execute(request)) {
            logger.debug(String.format("Response: HTTP status: %d", response.getStatusCode()));
            return getResponseBody(response);
        } catch (final Exception e) {
            logger.debug(String.format("GET on %s failed: %s", url, e.getMessage()));
            throw new IntegrationException(e);
        }
    }

    private String getResponseBody(final Response response) throws IntegrationException {
        final String responseBody = response.getContentString();
        logger.trace(String.format("Response: body: %s", responseBody));
        return responseBody;
    }
}
