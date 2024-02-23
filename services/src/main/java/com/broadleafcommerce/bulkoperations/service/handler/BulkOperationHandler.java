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
package com.broadleafcommerce.bulkoperations.service.handler;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

/**
 * Handles the bulk operation request into Bulk Operation Services.
 *
 * Extensions of this handler will determine the service to call into for performing the provided
 * bulk operation.
 */
public interface BulkOperationHandler {

    /**
     * Determines if this handler can handle the bulk operation request.
     *
     * @param operationType the operation type of the bulk operation request
     * @param entityType the entity type being updated by the bulk operation request
     * @return true, if this handler is able to handle the bulk operation
     */
    boolean canHandle(String operationType, String entityType);

    /**
     * Handles this bulk operation request for the specified entity or operation type.
     *
     * @param bulkOperationRequest the bulk operation request DTO
     * @param contextInfo context information surrounding sandboxing/multitenant state
     * @return the bulk operation response DTO for the created bulk operation
     */
    BulkOperationResponse handle(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo);
}
