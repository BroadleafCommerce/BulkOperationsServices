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
package com.broadleafcommerce.bulkoperations.provider.jpa.autoconfigure;

import static com.broadleafcommerce.bulkoperations.service.environment.RouteConstants.Persistence.BULK_OPS_ROUTE_KEY;
import static com.broadleafcommerce.bulkoperations.service.environment.RouteConstants.Persistence.BULK_OPS_ROUTE_PACKAGE;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.broadleafcommerce.bulk.v2.messaging.BulkOpsInitializeItemsRequestProducer;
import com.broadleafcommerce.bulk.v2.messaging.BulkOpsProcessRequestProducer;
import com.broadleafcommerce.bulk.v2.messaging.sandbox.CreateSandboxRequestProducer;
import com.broadleafcommerce.bulkoperations.service.environment.BulkOperationsJpaProperties;
import com.broadleafcommerce.bulkoperations.service.environment.BulkOperationsProviderProperties;
import com.broadleafcommerce.common.jpa.data.JpaDataRoute;
import com.broadleafcommerce.common.messaging.data.MessagingDataRouteSupporting;
import com.broadleafcommerce.data.tracking.core.data.TrackingDataRouteSupporting;
import com.broadleafcommerce.data.tracking.jpa.messaging.DurableProducer;

@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(BulkOperationsProviderProperties.class)
public class BulkOperationsJpaAutoConfiguration {

    @ConditionalOnProperty(
            name = {"broadleaf.database.provider", "broadleaf.bulkoperations.database.provider"},
            havingValue = "jpa")
    @EnableConfigurationProperties(BulkOperationsJpaProperties.class)
    @JpaDataRoute(boundPropertiesType = BulkOperationsJpaProperties.class,
            routePackage = BULK_OPS_ROUTE_PACKAGE, routeKey = BULK_OPS_ROUTE_KEY,
            supportingRouteTypes = {TrackingDataRouteSupporting.class,
                    MessagingDataRouteSupporting.class})
    @Import({EnabledGranularOrFlex.CreateSandboxConfig.class,
            EnabledGranularOrFlex.InitializeItemsConfig.class,
            EnabledGranularOrFlex.ProcessBulkOpsConfig.class})
    public static class EnabledGranularOrFlex {
        @DurableProducer(output = CreateSandboxRequestProducer.class,
                name = CreateSandboxRequestProducer.TYPE,
                configurationPrefix = "broadleaf.create-sandbox",
                componentPrefix = "createSandbox")
        public static class CreateSandboxConfig {}

        @DurableProducer(output = BulkOpsInitializeItemsRequestProducer.class,
                name = BulkOpsInitializeItemsRequestProducer.TYPE,
                configurationPrefix = "broadleaf.initialize-items",
                componentPrefix = "initializeItems")
        public static class InitializeItemsConfig {}

        @DurableProducer(output = BulkOpsProcessRequestProducer.class,
                name = BulkOpsProcessRequestProducer.TYPE,
                configurationPrefix = "broadleaf.process-bulkops",
                componentPrefix = "processBulkOps")
        public static class ProcessBulkOpsConfig {}
    }
}
