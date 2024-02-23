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
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulk.v2.domain.InitializeItemResponse;
import com.broadleafcommerce.bulk.v2.domain.SupportedBulkOperation;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.domain.SearchResponse;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

import java.util.List;

/**
 * Provider for interfacing with operations around catalog. Typically utilizes {@link WebClient} to
 * make requests to an external REST API.
 *
 * @param <I> The type of the results expected in the {@link SearchResponse}. Should extend
 *        {@link CatalogItem}.
 */
public interface CatalogProvider<I extends CatalogItem> {

    /**
     * Creates a bulk operation with the supplied {@link BulkOperationRequest}.
     *
     * @param bulkOperationRequest the {@link BulkOperationRequest} to create
     * @param contextInfo context information surrounding sandboxing/multitenant state
     * @return the created bulk operation
     */
    BulkOperationResponse createBulkOperation(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo);

    /**
     * Retrieves the list of supported bulk operations for the given operation type in catalog
     * services.
     *
     * @param operationType the operation type to check
     * @param entityType the entity type to check
     * @return the list of supported bulk operations
     */
    List<SupportedBulkOperation> getSupportedBulkOperations(String operationType,
            @Nullable String entityType);

    /**
     * Initializes the bulk operation items from the entities returned in the
     * {@link SearchResponse}.
     *
     * @param catalogItems the list of catalog items from the search service
     * @param bulkOperationRequest the {@link BulkOperationRequest} for this bulk operation
     * @param bulkOperationResponse the bulk operation created
     * @param pageable the current page information
     * @param contextInfo context information surrounding sandboxing/multitenant state
     * @return the {@link InitializeItemResponse} containing the items created for the bulk
     *         operation
     */
    InitializeItemResponse initializeItems(SearchResponse<I> catalogItems,
            BulkOperationRequest bulkOperationRequest,
            BulkOperationResponse bulkOperationResponse,
            Pageable pageable,
            ContextInfo contextInfo);

    /**
     * Updates the bulk operation to set the total record count after item initialization.
     *
     * @param totalRecordCount the total number of bulk operation item records
     * @param bulkOperationResponse the bulk operation
     * @param contextInfo context information surrounding sandboxing/multitenant state
     * @return the updated {@link BulkOperationResponse bulk operation}
     */
    BulkOperationResponse updateBulkOperationTotalRecordCount(long totalRecordCount,
            BulkOperationResponse bulkOperationResponse,
            ContextInfo contextInfo);
}
