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
package com.broadleafcommerce.bulkoperations.messaging;

import static com.broadleafcommerce.bulkoperations.service.environment.RouteConstants.Persistence.BULK_OPS_ROUTE_KEY;
import static com.broadleafcommerce.common.messaging.service.DefaultMessageLockService.MESSAGE_IDEMPOTENCY_KEY;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.data.domain.Pageable;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.broadleafcommerce.bulk.v2.domain.InitializeItemResponse;
import com.broadleafcommerce.bulk.v2.messaging.BulkOpsInitializeItemsRequest;
import com.broadleafcommerce.bulk.v2.messaging.BulkOpsProcessRequest;
import com.broadleafcommerce.bulk.v2.messaging.BulkOpsProcessRequestProducer;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.domain.SearchResponse;
import com.broadleafcommerce.bulkoperations.service.environment.BulkOperationsProviderProperties;
import com.broadleafcommerce.bulkoperations.service.environment.RouteConstants;
import com.broadleafcommerce.bulkoperations.service.provider.CatalogProvider;
import com.broadleafcommerce.bulkoperations.service.provider.SearchProvider;
import com.broadleafcommerce.common.extension.data.DataRouteByKey;
import com.broadleafcommerce.common.messaging.notification.DetachedDurableMessageSender;
import com.broadleafcommerce.common.messaging.service.IdempotentMessageConsumptionService;
import com.broadleafcommerce.data.tracking.core.filtering.DefaultPageRequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A listener that accepts a {@link BulkOpsInitializeItemsRequest} in order to initialize bulk
 * operation items.
 */
@Slf4j
@RequiredArgsConstructor
@DataRouteByKey(RouteConstants.Persistence.BULK_OPS_ROUTE_KEY)
public class InitializeBulkOperationItemsListener<CI extends CatalogItem> {

    @Getter(value = AccessLevel.PROTECTED)
    private final InitializeBulkOperationItemsProperties initializeBulkOperationItemsProperties;

    @Getter(AccessLevel.PROTECTED)
    private final CatalogProvider<CI> catalogProvider;

    @Getter(AccessLevel.PROTECTED)
    private final SearchProvider<CI> searchProvider;

    @Getter(AccessLevel.PROTECTED)
    private final IdempotentMessageConsumptionService idempotentConsumptionService;

    @Getter(value = AccessLevel.PROTECTED)
    private final DetachedDurableMessageSender sender;

    @Getter(AccessLevel.PROTECTED)
    private final BulkOpsProcessRequestProducer processRequestProducer;

    @Getter(AccessLevel.PROTECTED)
    private final BulkOperationsProviderProperties bulkOperationsProviderProperties;


    @StreamListener(BulkOpsInitializeItemsConsumer.CHANNEL)
    public void listen(Message<BulkOpsInitializeItemsRequest> message) {
        idempotentConsumptionService.consumeMessage(message,
                InitializeBulkOperationItemsListener.class.getSimpleName(), this::processMessage);
    }

    protected void processMessage(@lombok.NonNull Message<BulkOpsInitializeItemsRequest> message) {
        int batchSize = initializeBulkOperationItemsProperties.getBatchSize();
        int currentBatchCount;
        int currentPageNumber = 0;
        long totalItemRecords = 0;

        BulkOpsInitializeItemsRequest request = message.getPayload();

        do {
            Pageable pageable = new DefaultPageRequest(currentPageNumber, batchSize);

            SearchResponse<CI> searchResponse =
                    searchProvider.getSearchResults(request.getBulkOperationRequest(),
                            request.getBulkOperationResponse(),
                            pageable,
                            request.getContextInfo());

            if (!searchResponse.getContent().isEmpty()) {
                InitializeItemResponse response = catalogProvider.initializeItems(searchResponse,
                        request.getBulkOperationRequest(),
                        request.getBulkOperationResponse(),
                        pageable,
                        request.getContextInfo());
                totalItemRecords += response.getItemResponses().size();
            }

            currentPageNumber++;
            currentBatchCount = searchResponse.getContent().size();
        } while (currentBatchCount == batchSize);

        catalogProvider.updateBulkOperationTotalRecordCount(totalItemRecords,
                request.getBulkOperationResponse(), request.getContextInfo());

        // finally, send process message
        sendProcessBulkOperationRequest(request);
    }

    private void sendProcessBulkOperationRequest(BulkOpsInitializeItemsRequest request) {
        String bulkOpsId = request.getBulkOperationResponse().getId();
        BulkOpsProcessRequest processRequest = new BulkOpsProcessRequest(bulkOpsId,
                request.getBulkOperationRequest().getOperationType());

        if (bulkOperationsProviderProperties != null
                && !"none".equals(bulkOperationsProviderProperties.getProvider())
                && sender != null) {
            sender.send(processRequest, BulkOpsProcessRequestProducer.TYPE,
                    bulkOpsId, BULK_OPS_ROUTE_KEY);
        } else {
            log.debug("Durable sending for bulk operations is disabled. " +
                    "This occurs if the database provider is set to 'none' or if the sender does not exist.");

            Message<BulkOpsProcessRequest> processRequestMessage =
                    MessageBuilder.withPayload(processRequest)
                            .setHeaderIfAbsent(MESSAGE_IDEMPOTENCY_KEY, bulkOpsId)
                            .build();

            processRequestProducer.processBulkOperationRequestOutput().send(processRequestMessage);
        }
    }
}
