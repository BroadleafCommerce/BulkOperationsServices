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
package com.broadleafcommerce.bulkoperations.oauth2.client.endpoint;

import static org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors.oauth2AccessTokenResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

/**
 * Alternative to {@link DefaultClientCredentialsTokenResponseClient} that uses {@link WebClient}
 * and allows for access token response that contain additional non-String parameters.
 *
 * <p>
 * This is necessary if the access token response contains any non-String properties, as the default
 * implementation {@link DefaultClientCredentialsTokenResponseClient} relies on
 * {@link OAuth2AccessTokenResponseHttpMessageConverter} which restricts the access token response
 * to a {@link Map} where the value type is {@link String} instead of {@link Object}. This
 * implementation properly allows non-String properties within the access token response.
 *
 * <p>
 * Since {@link RestTemplate} is expected to be later deprecated, this implementation utilizes a
 * {@link WebClient} instead for a more consistent experience.
 *
 * <p>
 * This implementation also takes a lot of inspiration from
 * {@link WebClientReactiveClientCredentialsTokenResponseClient}, which is the reactive version of
 * this component.
 *
 *
 * TODO: move this to a broadleaf oauth2 client dependency
 *
 * @author Nick Crum (ncrum)
 */
@RequiredArgsConstructor
public class OAuth2ClientCredentialsAccessTokenResponseClient implements
        OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {
    private final WebClient webClient;

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(
            OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest) {
        Assert.notNull(clientCredentialsGrantRequest,
                "clientCredentialsGrantRequest cannot be null");

        BodyInserters.FormInserter<String> body = body(clientCredentialsGrantRequest);

        ClientRegistration clientRegistration =
                clientCredentialsGrantRequest.getClientRegistration();
        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        return webClient.post()
                .uri(tokenUri)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers(clientRegistration))
                .body(body)
                .exchange()
                .flatMap(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        // extract the contents of this into a method named
                        // oauth2AccessTokenResponse but has an argument for the response
                        throw WebClientResponseException.create(response.rawStatusCode(),
                                "Cannot get token, expected 2xx HTTP Status code",
                                null,
                                null,
                                null);
                    }
                    return response.body(oauth2AccessTokenResponse());
                })
                .map(response -> {
                    if (response.getAccessToken().getScopes().isEmpty()) {
                        response = OAuth2AccessTokenResponse.withResponse(response)
                                .scopes(clientRegistration.getScopes())
                                .build();
                    }
                    return response;
                })
                .block();
    }

    private Consumer<HttpHeaders> headers(ClientRegistration clientRegistration) {
        return headers -> {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                    .equals(clientRegistration.getClientAuthenticationMethod())) {
                headers.setBasicAuth(clientRegistration.getClientId(),
                        clientRegistration.getClientSecret());
            }
        };
    }

    private static BodyInserters.FormInserter<String> body(
            OAuth2ClientCredentialsGrantRequest authorizationGrantRequest) {
        ClientRegistration clientRegistration = authorizationGrantRequest.getClientRegistration();
        BodyInserters.FormInserter<String> body = BodyInserters
                .fromFormData(
                        OAuth2ParameterNames.GRANT_TYPE,
                        authorizationGrantRequest.getGrantType().getValue());
        Set<String> scopes = clientRegistration.getScopes();
        if (!CollectionUtils.isEmpty(scopes)) {
            String scope = StringUtils.collectionToDelimitedString(scopes, " ");
            body.with(OAuth2ParameterNames.SCOPE, scope);
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST
                .equals(clientRegistration.getClientAuthenticationMethod())) {
            body.with(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
            body.with(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
        }
        return body;
    }
}
