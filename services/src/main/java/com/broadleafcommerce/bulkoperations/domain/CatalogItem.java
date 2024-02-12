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
package com.broadleafcommerce.bulkoperations.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;


/**
 * A reference to a catalog item, typically used as a holder for information communicated from an
 * external service.
 */
public interface CatalogItem {

    /**
     * The ID of the CatalogItem.
     *
     * @return The ID of the CatalogItem.
     */
    String getId();

    /**
     * Takes in any additional attributes passed in the request not matching any defined properties.
     *
     * @param name Name of the additional attribute
     * @param value Value of the additional attribute
     */
    @JsonAnySetter
    void addAttribute(String name, Object value);

    /**
     * Return any additional attributes passed in the request not matching any defined properties.
     *
     * @return any additional attributes passed in the request not matching any defined properties.
     */
    @JsonAnyGetter
    Map<String, Object> getAttributes();

}
