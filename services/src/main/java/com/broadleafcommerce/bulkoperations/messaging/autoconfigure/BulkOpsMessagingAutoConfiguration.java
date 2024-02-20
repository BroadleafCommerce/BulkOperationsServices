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
package com.broadleafcommerce.bulkoperations.messaging.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.broadleafcommerce.bulk.v2.messaging.BulkOpsProcessRequestProducer;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.messaging.BulkOpsInitializeItemsConsumer;
import com.broadleafcommerce.bulkoperations.messaging.InitializeBulkOperationItemsListener;
import com.broadleafcommerce.bulkoperations.messaging.InitializeBulkOperationItemsProperties;
import com.broadleafcommerce.bulkoperations.service.environment.BulkOperationsProviderProperties;
import com.broadleafcommerce.bulkoperations.service.provider.CatalogProvider;
import com.broadleafcommerce.bulkoperations.service.provider.SearchProvider;
import com.broadleafcommerce.common.extension.ConditionalOnPropertyOrGroup;
import com.broadleafcommerce.common.messaging.notification.DetachedDurableMessageSender;
import com.broadleafcommerce.common.messaging.service.IdempotentMessageConsumptionService;

@Configuration
@ConditionalOnPropertyOrGroup(
        name = "broadleaf.bulkoperations.messaging.active",
        group = "broadleaf.basic.messaging.enabled",
        matchIfMissing = true)
@EnableConfigurationProperties(InitializeBulkOperationItemsProperties.class)
@EnableBinding({BulkOpsInitializeItemsConsumer.class})
public class BulkOpsMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    InitializeBulkOperationItemsListener initializeBulkOperationItemsListener(
            InitializeBulkOperationItemsProperties initializeBulkOperationItemsProperties,
            CatalogProvider<? extends CatalogItem> catalogProvider,
            SearchProvider<? extends CatalogItem> searchProvider,
            IdempotentMessageConsumptionService idempotentConsumptionService,
            DetachedDurableMessageSender sender,
            BulkOpsProcessRequestProducer processRequestProducer,
            BulkOperationsProviderProperties bulkOperationsProviderProperties) {
        return new InitializeBulkOperationItemsListener(initializeBulkOperationItemsProperties,
                catalogProvider,
                searchProvider,
                idempotentConsumptionService,
                sender,
                processRequestProducer,
                bulkOperationsProviderProperties);
    }
}
