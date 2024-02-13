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

import com.broadleafcommerce.common.extension.RequestView;
import com.broadleafcommerce.common.extension.ResponseView;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * DTO representing the results from the search service request.
 *
 * @param <T> the type of which the response results consist.
 *
 * @author Nathan Moore (nathandmoore)
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonView({RequestView.class, ResponseView.class})
public class SearchResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The actual results for the search request.
     *
     * @param content The actual results for the search request.
     *
     * @return The actual results for the search request.
     */
    private List<T> content = new ArrayList<>();

    /**
     * Map holding any additional attributes passed in the request not matching any defined
     * properties.
     */
    @JsonIgnore
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Takes in any additional attributes passed in the request not matching any defined properties.
     *
     * @param name Name of the additional attribute
     *
     * @param value Value of the additional attribute
     */
    @JsonAnySetter
    public void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Return any additional attributes passed in the request not matching any defined properties.
     *
     * @return any additional attributes passed in the request not matching any defined properties.
     */
    @JsonAnyGetter
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
}
