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

import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This component implements {@link OAuth2AuthorizedClientManager} and internally instantiates and
 * delegates to {@link AuthorizedClientServiceOAuth2AuthorizedClientManager}.
 *
 * The only difference between this and {@link AuthorizedClientServiceOAuth2AuthorizedClientManager}
 * is that this attempts to serialize on the
 * {@link OAuth2AuthorizeRequest#getClientRegistrationId()}. The reason is that these clients are
 * normally shared clients, with shared access tokens. As a result, only one thread needs to call
 * the auth server when the token is unavailable or otherwise expired. The token is then stored in
 * {@link InMemoryOAuth2AuthorizedClientService} for re-use across threads.
 *
 * This helps prevent a race condition where multiple threads are trying to fetch the same token at
 * the same time via a network call.
 *
 * @author Kelly Tisdell (ktisdell)
 */
public class SynchronizedDelegatingOAuth2AuthorizedClientManager
        implements OAuth2AuthorizedClientManager {

    private final Map<String, Object> MUTEX_MAP = Collections.synchronizedMap(new HashMap<>());
    private final AuthorizedClientServiceOAuth2AuthorizedClientManager delegate;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SynchronizedDelegatingOAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository));
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public OAuth2AuthorizedClient authorize(OAuth2AuthorizeRequest authorizeRequest) {
        Assert.notNull(authorizeRequest, "authorizeRequest cannot be null");

        final String registrationId = authorizeRequest.getClientRegistrationId();
        if (registrationId != null) {
            if (this.clientRegistrationRepository.findByRegistrationId(registrationId) != null) {
                // Use the registrationId to fetch a shared mutex object and synchronize on that.
                // This essentially puts a gate in place to synchronize on a particular
                // registrationId.
                final Object mutex = MUTEX_MAP.computeIfAbsent(registrationId, k -> new Object());
                synchronized (mutex) {
                    // Most of the time this will use in-memory data and will be very fast.
                    // In cases where the token is expired, this will gate the call to the auth
                    // server to 1 thread per clientRegistrationId per node.
                    return delegate.authorize(authorizeRequest);
                }
            }
        }

        // If there is no ClientRegistration, then we'll defer to the default functionality
        // without synchronization.
        return delegate.authorize(authorizeRequest);
    }

    /**
     * @see AuthorizedClientServiceOAuth2AuthorizedClientManager#setAuthorizedClientProvider(OAuth2AuthorizedClientProvider)
     */
    public void setAuthorizedClientProvider(
            OAuth2AuthorizedClientProvider authorizedClientProvider) {
        delegate.setAuthorizedClientProvider(authorizedClientProvider);
    }

    /**
     * @see AuthorizedClientServiceOAuth2AuthorizedClientManager#setContextAttributesMapper(Function)
     */
    public void setContextAttributesMapper(
            Function<OAuth2AuthorizeRequest, Map<String, Object>> contextAttributesMapper) {
        delegate.setContextAttributesMapper(contextAttributesMapper);
    }

    /**
     * @see AuthorizedClientServiceOAuth2AuthorizedClientManager#setAuthorizationSuccessHandler(OAuth2AuthorizationSuccessHandler)
     */
    public void setAuthorizationSuccessHandler(
            OAuth2AuthorizationSuccessHandler authorizationSuccessHandler) {
        delegate.setAuthorizationSuccessHandler(authorizationSuccessHandler);
    }

    /**
     * @see AuthorizedClientServiceOAuth2AuthorizedClientManager#setAuthorizationFailureHandler(OAuth2AuthorizationFailureHandler)
     */
    public void setAuthorizationFailureHandler(
            OAuth2AuthorizationFailureHandler authorizationFailureHandler) {
        delegate.setAuthorizationFailureHandler(authorizationFailureHandler);
    }
}
