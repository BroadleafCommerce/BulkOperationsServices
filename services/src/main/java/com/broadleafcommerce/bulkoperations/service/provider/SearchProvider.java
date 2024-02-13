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
package com.broadleafcommerce.bulkoperations.service.provider;

import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.domain.SearchResponse;
import com.broadleafcommerce.bulkoperations.exception.ProviderApiException;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

/**
 * Provider for interfacing with operations around catalog search. Typically utilizes
 * {@link WebClient} to make requests to an external REST API.
 *
 * @param <I> The type of the results expected in the {@link SearchResponse}. Should extend
 *        {@link CatalogItem}.
 */
public interface SearchProvider<I extends CatalogItem> {

    /**
     * Performs a search for the provided request.
     *
     * @param request All of the relevant information to be used to retrieve search results.
     *
     * @return The {@link SearchResponse} with results for the request.
     * @throws ProviderApiException if the provider encounters an error with the request.
     */
    SearchResponse<I> getSearchResults(BulkOperationRequest request,
            BulkOperationResponse bulkOperationResponse,
            Pageable pageable,
            ContextInfo contextInfo);
}
