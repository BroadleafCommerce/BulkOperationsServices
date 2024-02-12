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

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("broadleaf.bulkoperations.catalogprovider")
public class ExternalCatalogProperties {

    /**
     * The base url for an external catalog service: {@code https://localhost:8447/catalog}.
     */
    @Getter
    @Setter
    private String url;

    /**
     * The context path to the bulk operation endpoint
     */
    @Getter
    @Setter
    private String bulkOperationUri;

    /**
     * The context path to the bulk operation items
     */
    @Getter
    @Setter
    private String bulkOperationItemsUri;

    @Getter
    @Setter
    private String supportedBulkOpsUri;

    /**
     * The service client to use when calling search. Default is "bulkopsclient"
     */
    @Getter
    @Setter
    private String serviceClient = "bulkopsclient";
}
