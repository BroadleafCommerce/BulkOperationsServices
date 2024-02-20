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
package com.broadleafcommerce.bulkoperations.web.endpoint;

import static com.broadleafcommerce.bulkoperations.web.endpoint.BulkOperationsEndpoint.BASE_URI;

import org.broadleafcommerce.frameworkmapping.annotation.FrameworkMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkPostMapping;
import org.broadleafcommerce.frameworkmapping.annotation.FrameworkRestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulkoperations.service.BulkOperationsService;
import com.broadleafcommerce.bulkoperations.service.handler.BulkOperationHandler;
import com.broadleafcommerce.bulkoperations.web.exception.BulkOperationHandlerNotFoundException;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;
import com.broadleafcommerce.data.tracking.core.context.ContextOperation;
import com.broadleafcommerce.data.tracking.core.policy.Policy;
import com.broadleafcommerce.data.tracking.core.type.OperationType;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@FrameworkRestController
@FrameworkMapping(BASE_URI)
@RequiredArgsConstructor
@Slf4j
public class BulkOperationsEndpoint {

    public static final String BASE_URI = "/bulk-operations";

    @Getter(AccessLevel.PROTECTED)
    private final BulkOperationsService bulkOperationsService;

    @Getter(AccessLevel.PROTECTED)
    private final List<BulkOperationHandler> bulkOperationHandlers;

    @FrameworkPostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Policy(permissionRoots = "BULK_OPERATION")
    public BulkOperationResponse createBulkOperation(HttpServletRequest request,
            @ContextOperation(value = OperationType.CREATE) ContextInfo context,
            @RequestBody BulkOperationRequest bulkOperationRequest) {
        bulkOperationsService.validateBulkOperationRequest(bulkOperationRequest, context);

        for (BulkOperationHandler bulkOperationHandler : bulkOperationHandlers) {
            if (bulkOperationHandler.canHandle(bulkOperationRequest.getOperationType(),
                    bulkOperationRequest.getEntityType())) {
                return bulkOperationHandler.handle(bulkOperationRequest, context);
            }
        }

        log.warn("No handler was found for operation type {} and entity type {}",
                bulkOperationRequest.getOperationType(),
                bulkOperationRequest.getEntityType());

        throw new BulkOperationHandlerNotFoundException(
                String.format(
                        "No handler was found for operation type %s and entity type %s",
                        bulkOperationRequest.getOperationType(),
                        bulkOperationRequest.getEntityType()));
    }
}
