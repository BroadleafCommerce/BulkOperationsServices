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
package com.broadleafcommerce.bulkoperations.service.provider.external;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationTotalRecordCountRequest;
import com.broadleafcommerce.bulk.v2.domain.InitializeItemRequest;
import com.broadleafcommerce.bulk.v2.domain.InitializeItemResponse;
import com.broadleafcommerce.bulk.v2.domain.SupportedBulkOperation;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.domain.SearchResponse;
import com.broadleafcommerce.bulkoperations.exception.ProviderApiException;
import com.broadleafcommerce.bulkoperations.service.provider.CatalogProvider;
import com.broadleafcommerce.bulkoperations.service.provider.utils.ProviderUtils;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class ExternalCatalogProvider<I extends CatalogItem> implements CatalogProvider<I> {

    @Getter(AccessLevel.PROTECTED)
    private final WebClient webClient;

    @Getter(AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @Getter(AccessLevel.PROTECTED)
    private final ProviderUtils providerUtils;

    @Getter(AccessLevel.PROTECTED)
    private final ExternalCatalogProperties properties;

    public ExternalCatalogProvider(WebClient webClient,
            TypeFactory typeFactory,
            ProviderUtils providerUtils, ExternalCatalogProperties properties) {
        this.webClient = webClient;
        this.typeFactory = typeFactory;
        this.providerUtils = providerUtils;
        this.properties = properties;
    }

    @Override
    public BulkOperationResponse createBulkOperation(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo) {
        final String createBulkOperationUrl = getCreateBulkOperationUrl(contextInfo);

        return providerUtils.executeRequest(() -> getWebClient()
                .post()
                .uri(createBulkOperationUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bulkOperationRequest)
                .headers(httpHeaders -> httpHeaders.putAll(providerUtils.getHeaders(contextInfo)))
                .attributes(clientRegistrationId(getServiceClient()))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.createException().flatMap(
                                exception -> Mono.just(new ProviderApiException(exception))))
                .bodyToMono(BulkOperationResponse.class)
                .block());
    }

    @Override
    public List<SupportedBulkOperation> getSupportedBulkOperations(String operationType,
            @Nullable String entityType) {
        final String supportedBulkOpsUrl = getSupportedBulkOpsUrl(operationType, entityType);

        return providerUtils.executeRequest(() -> getWebClient()
                .get()
                .uri(supportedBulkOpsUrl)
                .accept(MediaType.APPLICATION_JSON)
                .attributes(clientRegistrationId(getServiceClient()))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.createException().flatMap(
                                exception -> Mono.just(new ProviderApiException(exception))))
                .bodyToMono(new ParameterizedTypeReference<List<SupportedBulkOperation>>() {})
                .block());
    }

    @Override
    public InitializeItemResponse initializeItems(
            SearchResponse<I> searchResponse,
            BulkOperationRequest bulkOperationRequest,
            BulkOperationResponse bulkOperationResponse,
            Pageable pageable,
            ContextInfo contextInfo) {
        final String initializeBulkOperationItemsUrl =
                getInitializeBulkOperationItemsUrl(bulkOperationResponse.getId(), pageable,
                        contextInfo);

        List<String> catalogItemIds = searchResponse.getContent().stream()
                .map(CatalogItem::getId)
                .filter(id -> !bulkOperationRequest.getExclusions().contains(id))
                .collect(Collectors.toList());
        InitializeItemRequest itemRequest = getTypeFactory().get(InitializeItemRequest.class);
        itemRequest.setEntityContextIds(catalogItemIds);

        return providerUtils.executeRequest(() -> getWebClient()
                .post()
                .uri(initializeBulkOperationItemsUrl)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(itemRequest)
                .headers(httpHeaders -> httpHeaders.putAll(providerUtils.getHeaders(contextInfo)))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .attributes(clientRegistrationId(getServiceClient()))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.createException().flatMap(
                                exception -> Mono.just(new ProviderApiException(exception))))
                .bodyToMono(InitializeItemResponse.class)
                .block());
    }

    @Override
    public BulkOperationResponse updateBulkOperationTotalRecordCount(long totalRecordCount,
            BulkOperationResponse bulkOperationResponse,
            ContextInfo contextInfo) {
        final String updateBulkOperationTotalRecordCountUrl =
                getUpdateBulkOperationTotalRecordCountUrl(bulkOperationResponse);

        BulkOperationTotalRecordCountRequest request =
                typeFactory.get(BulkOperationTotalRecordCountRequest.class);
        request.setTotalRecordCount(totalRecordCount);

        return providerUtils.executeRequest(() -> getWebClient()
                .patch()
                .uri(updateBulkOperationTotalRecordCountUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .headers(httpHeaders -> httpHeaders.putAll(providerUtils.getHeaders(contextInfo)))
                .attributes(clientRegistrationId(getServiceClient()))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.createException().flatMap(
                                exception -> Mono.just(new ProviderApiException(exception))))
                .bodyToMono(BulkOperationResponse.class)
                .block());
    }


    protected String getCreateBulkOperationUrl(@Nullable ContextInfo contextInfo) {
        return fromHttpUrl(properties.getUrl())
                .path(properties.getBulkOperationUri())
                .toUriString();
    }

    protected String getSupportedBulkOpsUrl(String operationType, @Nullable String entityType) {
        UriComponentsBuilder uriComponentsBuilder = fromHttpUrl(properties.getUrl())
                .path(properties.getSupportedBulkOpsUri())
                .queryParam("operationType", operationType);

        if (StringUtils.isNotBlank(entityType)) {
            uriComponentsBuilder.queryParam("entityType", entityType);
        }

        return uriComponentsBuilder.toUriString();
    }

    protected String getInitializeBulkOperationItemsUrl(String bulkOperationId,
            Pageable pageable,
            @Nullable ContextInfo contextInfo) {
        return fromHttpUrl(properties.getUrl())
                .path(properties.getBulkOperationUri())
                .pathSegment(bulkOperationId)
                .path(properties.getBulkOperationItemsUri())
                .queryParams(providerUtils.pageableToParams(pageable))
                .toUriString();
    }

    protected String getUpdateBulkOperationTotalRecordCountUrl(
            BulkOperationResponse bulkOperationResponse) {
        return fromHttpUrl(properties.getUrl())
                .path(properties.getBulkOperationUri())
                .pathSegment(bulkOperationResponse.getId())
                .path(properties.getBulkOperationTotalRecordsUri())
                .toUriString();
    }

    protected String getServiceClient() {
        return properties.getServiceClient();
    }
}
