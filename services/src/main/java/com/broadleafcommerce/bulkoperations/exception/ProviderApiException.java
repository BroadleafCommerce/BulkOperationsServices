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
package com.broadleafcommerce.bulkoperations.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.broadleafcommerce.bulkoperations.service.provider.CatalogProvider;

import lombok.Getter;

/**
 * Defines a generic error that can be thrown in response to receiving an error from an API that one
 * of the providers (e.g., {@link CatalogProvider}) received. This allows the error to bubble up to
 * the local endpoint and be returned.
 */
public class ProviderApiException extends RuntimeException {

    /**
     * The received error.
     *
     * @return The received error.
     */
    @Getter
    private final WebClientResponseException receivedException;

    public ProviderApiException(WebClientResponseException receivedException) {
        super(receivedException);
        this.receivedException = receivedException;
    }

    public ProviderApiException(String message,
            WebClientResponseException receivedException) {
        super(message);
        this.receivedException = receivedException;
    }

    public ProviderApiException(String message,
            Throwable cause,
            WebClientResponseException receivedException) {
        super(message, cause);
        this.receivedException = receivedException;
    }

    public ProviderApiException(Throwable cause,
            WebClientResponseException receivedException) {
        super(cause);
        this.receivedException = receivedException;
    }
}
