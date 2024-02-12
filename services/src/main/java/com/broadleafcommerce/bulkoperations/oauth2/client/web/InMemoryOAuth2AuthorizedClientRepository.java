/*
 * Copyright (C) 2009 - 2020 Broadleaf Commerce
 *
 * Licensed under the Broadleaf End User License Agreement (EULA), Version 1.1 (the
 * "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt).
 *
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the
 * "Custom License") between you and Broadleaf Commerce. You may not use this file except in
 * compliance with the applicable license.
 *
 * NOTICE: All information contained herein is, and remains the property of Broadleaf Commerce, LLC
 * The intellectual and technical concepts contained herein are proprietary to Broadleaf Commerce,
 * LLC and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained from Broadleaf Commerce, LLC.
 */
package com.broadleafcommerce.bulkoperations.oauth2.client.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * In-memory implementation of {@link OAuth2AuthorizedClientRepository} that provides support for
 * storing {@link OAuth2AuthorizedClient} for service-to-service communications.
 *
 * <p>
 * This stores the {@link OAuth2AuthorizedClient} by the client registration ID irrespective of any
 * {@link Authentication} or {@link HttpServletRequest}. This should not be used when it is
 * essential to tie the stored {@link OAuth2AuthorizedClient} to the user or session.
 *
 * @deprecated Please use {@link SynchronizedDelegatingOAuth2AuthorizedClientManager} or
 *             {@link org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService},
 *             which is used internally by
 *             {@link SynchronizedDelegatingOAuth2AuthorizedClientManager}.
 *
 * @author Nick Crum (ncrum)
 */
@Deprecated
public class InMemoryOAuth2AuthorizedClientRepository implements
        OAuth2AuthorizedClientRepository {
    private final Map<String, OAuth2AuthorizedClient> clientRegistrationIdToAuthorizedClient =
            new ConcurrentHashMap<>();

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
            Authentication authentication,
            HttpServletRequest request) {
        Assert.notNull(clientRegistrationId, "clientRegistrationId cannot be null");
        return (T) this.clientRegistrationIdToAuthorizedClient.get(clientRegistrationId);
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(authorizedClient, "authorizedClient cannot be null");
        String clientRegistrationId = authorizedClient.getClientRegistration().getRegistrationId();
        this.clientRegistrationIdToAuthorizedClient.put(clientRegistrationId, authorizedClient);
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(clientRegistrationId, "clientRegistrationId cannot be null");
        this.clientRegistrationIdToAuthorizedClient.remove(clientRegistrationId);
    }
}
