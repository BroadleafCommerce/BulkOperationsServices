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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulk.v2.domain.SearchFilter;
import com.broadleafcommerce.bulk.v2.domain.SearchFilterRangeValue;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.domain.Product;
import com.broadleafcommerce.bulkoperations.domain.SearchResponse;
import com.broadleafcommerce.bulkoperations.exception.ProviderApiException;
import com.broadleafcommerce.bulkoperations.service.provider.SearchProvider;
import com.broadleafcommerce.bulkoperations.service.provider.utils.ProviderUtils;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class ExternalSearchProvider<I extends CatalogItem>
        implements SearchProvider<I> {

    @Getter(AccessLevel.PROTECTED)
    private final WebClient webClient;

    @Getter(AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    @Getter(AccessLevel.PROTECTED)
    private final ProviderUtils providerUtils;

    @Getter(AccessLevel.PROTECTED)
    private final ExternalSearchProperties properties;

    public ExternalSearchProvider(WebClient webClient,
            TypeFactory typeFactory,
            ProviderUtils providerUtils,
            ExternalSearchProperties properties) {
        this.webClient = webClient;
        this.typeFactory = typeFactory;
        this.providerUtils = providerUtils;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SearchResponse<I> getSearchResults(BulkOperationRequest request,
            BulkOperationResponse bulkOperationResponse,
            Pageable pageable,
            ContextInfo contextInfo) {
        final ParameterizedTypeReference<SearchResponse<? extends Product>> responseType =
                new ParameterizedTypeReference<>() {};

        MultiValueMap<String, String> searchParams = buildSearchParamsFromRequest(request);

        final SearchResponse<I> searchResponse = (SearchResponse<I>) getWebClient().get()
                .uri(fromHttpUrl(properties.getUrl())
                        .path(properties.getSearchUri())
                        .queryParams(searchParams)
                        .queryParam("size", String.valueOf(pageable.getPageSize()))
                        .queryParam("page", String.valueOf(pageable.getPageNumber()))
                        .queryParam("type", "PRODUCT")
                        .toUriString())
                .headers(headers -> headers.putAll(providerUtils.getHeaders(contextInfo)))
                .accept(MediaType.APPLICATION_JSON)
                .attributes(clientRegistrationId(getServiceClient()))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> response.createException().flatMap(
                                exception -> Mono.just(new ProviderApiException(exception))))
                .bodyToMono(responseType)
                .block();

        Assert.notNull(searchResponse, "Search response should not be null");

        return searchResponse;
    }

    protected MultiValueMap<String, String> buildSearchParamsFromRequest(
            BulkOperationRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        int filterIndex = 0;
        for (SearchFilter filter : request.getFilters()) {
            params.put("filters[" + filterIndex + "].name", List.of(filter.getName()));


            params.put("filters[" + filterIndex + "].values", filter.getValues());

            int rangeIndex = 0;
            for (SearchFilterRangeValue rangeValue : filter.getRanges()) {
                params.put("filters[" + filterIndex + "].ranges[" + rangeIndex + "].minValue",
                        List.of(rangeValue.getMinValue()));
                params.put("filters[" + filterIndex + "].ranges[" + rangeIndex + "].maxValue",
                        List.of(rangeValue.getMaxValue()));
                rangeIndex++;
            }

            filterIndex++;
        }

        if (StringUtils.isNotBlank(request.getQuery())) {
            params.put("query", List.of(request.getQuery()));
        }

        return params;
    }

    protected String getServiceClient() {
        return properties.getServiceClient();
    }
}
