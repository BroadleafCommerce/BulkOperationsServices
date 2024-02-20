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

import static com.broadleafcommerce.bulkoperations.service.environment.RouteConstants.Persistence.BULK_OPS_ROUTE_KEY;
import static com.broadleafcommerce.common.messaging.service.DefaultMessageLockService.MESSAGE_IDEMPOTENCY_KEY;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.bulk.v2.domain.BulkOperationResponse;
import com.broadleafcommerce.bulk.v2.domain.SupportedBulkOperation;
import com.broadleafcommerce.bulk.v2.messaging.BulkOpsInitializeItemsRequest;
import com.broadleafcommerce.bulk.v2.messaging.BulkOpsInitializeItemsRequestProducer;
import com.broadleafcommerce.bulk.v2.messaging.sandbox.CreateSandboxRequest;
import com.broadleafcommerce.bulk.v2.messaging.sandbox.CreateSandboxRequestProducer;
import com.broadleafcommerce.bulkoperations.service.environment.BulkOperationsProviderProperties;
import com.broadleafcommerce.bulkoperations.service.provider.CatalogProvider;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.common.messaging.notification.DetachedDurableMessageSender;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import io.azam.ulidj.ULID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class CatalogBulkOperationHandler implements BulkOperationHandler {

    @Getter(AccessLevel.PROTECTED)
    private final CatalogProvider catalogProvider;

    @Getter(value = AccessLevel.PROTECTED)
    private final DetachedDurableMessageSender sender;

    @Getter(AccessLevel.PROTECTED)
    @Setter(onMethod_ = {@Autowired})
    private BulkOperationsProviderProperties properties;

    @Getter(AccessLevel.PROTECTED)
    private final CreateSandboxRequestProducer createSandboxRequestProducer;

    @Getter(AccessLevel.PROTECTED)
    private final BulkOpsInitializeItemsRequestProducer bulkOpsInitializeItemsRequestProducer;

    @Getter(value = AccessLevel.PROTECTED)
    private final MessageSource messageSource;

    protected static final Random SECURE_RANDOM = new SecureRandom();

    @Getter(AccessLevel.PROTECTED)
    private final TypeFactory typeFactory;

    public CatalogBulkOperationHandler(CatalogProvider catalogProvider,
            DetachedDurableMessageSender sender,
            CreateSandboxRequestProducer createSandboxRequestProducer,
            BulkOpsInitializeItemsRequestProducer bulkOpsInitializeItemsRequestProducer,
            MessageSource messageSource,
            TypeFactory typeFactory) {
        this.catalogProvider = catalogProvider;
        this.sender = sender;
        this.createSandboxRequestProducer = createSandboxRequestProducer;
        this.bulkOpsInitializeItemsRequestProducer = bulkOpsInitializeItemsRequestProducer;
        this.messageSource = messageSource;
        this.typeFactory = typeFactory;
    }

    @Override
    public boolean canHandle(String operationType, @Nullable String entityType) {
        List<SupportedBulkOperation> supportedBulkOperation =
                catalogProvider.getSupportedBulkOperations(operationType, entityType);
        return supportedBulkOperation.stream()
                .map(SupportedBulkOperation::getOperationType)
                .anyMatch(opType -> StringUtils.equalsIgnoreCase(opType, operationType));
    }

    @Override
    public BulkOperationResponse handle(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo) {
        BulkOperationResponse response;

        String sandboxId = ULID.random(SECURE_RANDOM);
        createSandboxForBulkOperation(sandboxId, bulkOperationRequest, contextInfo);
        bulkOperationRequest.setSandboxId(sandboxId);
        bulkOperationRequest.setAuthor(contextInfo.getAuthor());

        response = catalogProvider.createBulkOperation(bulkOperationRequest, contextInfo);

        if (CollectionUtils.isEmpty(bulkOperationRequest.getInclusions())) {
            initializeItems(response, bulkOperationRequest, contextInfo);
        }

        return response;
    }

    /**
     * Sends the {@link CreateSandboxRequest} message to create a sandbox to contain the catalog
     * changes from this bulk operation.
     *
     * @param sandboxId the ID of the sandbox to create
     * @param bulkOperationRequest the bulk operation request DTO
     * @param contextInfo context information surrounding sandboxing/multitenant state
     */
    protected void createSandboxForBulkOperation(String sandboxId,
            BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo) {

        String sandboxName = getSandboxName(bulkOperationRequest, contextInfo);
        String sandboxDescription = getSandboxDescription(bulkOperationRequest, contextInfo);

        CreateSandboxRequest createSandboxRequest =
                new CreateSandboxRequest(sandboxDescription,
                        generateRandomHexColor(),
                        sandboxId,
                        sandboxName,
                        contextInfo.getContextRequest().getApplicationId(),
                        contextInfo.getContextRequest().getTenantId());

        String idempotencyKey = DigestUtils.md5Hex(createSandboxRequest.toString()).toUpperCase();

        if (properties != null && !"none".equals(properties.getProvider())
                && sender != null) {
            sender.send(createSandboxRequest, CreateSandboxRequestProducer.TYPE,
                    idempotencyKey, BULK_OPS_ROUTE_KEY);
        } else {
            Message<CreateSandboxRequest> message =
                    MessageBuilder.withPayload(createSandboxRequest)
                            .setHeaderIfAbsent(MESSAGE_IDEMPOTENCY_KEY, idempotencyKey)
                            .build();

            createSandboxRequestProducer.createSandboxRequestOutput().send(message);
        }
    }

    protected String generateRandomHexColor() {
        int randomWithinHexBound = SECURE_RANDOM.nextInt(0xffffff + 1);
        return String.format("#%06x", randomWithinHexBound);
    }

    protected String getSandboxName(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo) {
        return messageSource.getMessage("bulk-operations.sandbox.name",
                new String[] {bulkOperationRequest.getName()},
                "Bulk Operation - {0}",
                Optional.ofNullable(contextInfo)
                        .map(ContextInfo::getLocale)
                        .orElse(LocaleContextHolder.getLocale()));
    }

    protected String getSandboxDescription(BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo) {
        return messageSource.getMessage("bulk-operations.sandbox.description",
                new String[] {bulkOperationRequest.getName()},
                "Sandbox for BulkOperation - {0}",
                Optional.ofNullable(contextInfo)
                        .map(ContextInfo::getLocale)
                        .orElse(LocaleContextHolder.getLocale()));
    }

    /**
     * Sends the {@link BulkOpsInitializeItemsRequest} message to initialize bulk operation items.
     *
     * @param bulkOperationResponse the response DTO for the bulk operation creation
     * @param bulkOperationRequest the request DTO containing search and operation information
     * @param contextInfo context information surrounding sandboxing/multitenant state
     */
    protected void initializeItems(BulkOperationResponse bulkOperationResponse,
            BulkOperationRequest bulkOperationRequest,
            ContextInfo contextInfo) {
        BulkOpsInitializeItemsRequest bulkOpsInitializeItemsRequest =
                new BulkOpsInitializeItemsRequest(bulkOperationRequest,
                        bulkOperationResponse,
                        contextInfo);

        String idempotencyKey = bulkOperationResponse.getId();

        if (properties != null && !"none".equals(properties.getProvider())
                && sender != null) {
            sender.send(bulkOpsInitializeItemsRequest, BulkOpsInitializeItemsRequestProducer.TYPE,
                    idempotencyKey, BULK_OPS_ROUTE_KEY);
        } else {
            Message<BulkOpsInitializeItemsRequest> message =
                    MessageBuilder.withPayload(bulkOpsInitializeItemsRequest)
                            .setHeaderIfAbsent(MESSAGE_IDEMPOTENCY_KEY, idempotencyKey)
                            .build();

            bulkOpsInitializeItemsRequestProducer.initializeItemsRequestOutput().send(message);
        }
    }

}
