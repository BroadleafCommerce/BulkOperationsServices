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

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulk.v2.domain.InitializeItemResponse;
import com.broadleafcommerce.bulk.v2.domain.SupportedBulkOp;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.domain.SearchResponse;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

import java.util.List;

public interface CatalogProvider<I extends CatalogItem> {

    /**
     *
     * @param bulkOperationRequest
     * @param contextInfo
     * @return
     */
    BulkOperationResponse createBulkOperation(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo);

    /**
     *
     * @param operationType
     * @param entityType
     * @return
     */
    List<SupportedBulkOp> getSupportedBulkOperations(String operationType,
            @Nullable String entityType);

    /**
     *
     * @return
     */
    InitializeItemResponse initializeItems(SearchResponse<? extends CatalogItem> catalogItems,
            BulkOperationRequest bulkOperationRequest,
            BulkOperationResponse bulkOperationResponse,
            Pageable pageable,
            ContextInfo contextInfo);
}
