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
package com.broadleafcommerce.bulkoperations.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.broadleafcommerce.common.error.ApiError;
import com.broadleafcommerce.common.error.validation.web.FrameworkExceptionAdvisor;

import lombok.extern.slf4j.Slf4j;

/**
 * General exception handler registry for handlers not exclusive to a single controller.
 */
@Slf4j
@FrameworkExceptionAdvisor
@RestControllerAdvice(annotations = ResponseBody.class)
public class BulkOperationsExceptionAdvisor {

    @ExceptionHandler(BulkOperationHandlerNotFoundException.class)
    public ResponseEntity<ApiError> handleBulkOpsHandlerNotFoundException(Exception ex,
            WebRequest request) {
        logDebug(ex, request);
        return new ApiError("BULK_OPS_HANDLER_NOT_FOUND", ex.getMessage(), HttpStatus.BAD_REQUEST)
                .toResponseEntity();
    }

    protected void logDebug(Exception ex, WebRequest request) {
        if (log.isDebugEnabled()) {
            String requestURL =
                    ((ServletWebRequest) request).getRequest().getRequestURL().toString();
            log.debug("Request to {} raised an exception", requestURL, ex);
        }
    }
}
