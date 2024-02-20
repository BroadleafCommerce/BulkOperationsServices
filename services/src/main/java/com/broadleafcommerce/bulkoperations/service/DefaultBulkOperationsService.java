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
package com.broadleafcommerce.bulkoperations.service;

import static org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace;

import org.springframework.lang.Nullable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.broadleafcommerce.bulk.v2.domain.BulkOperationRequest;
import com.broadleafcommerce.common.error.validation.ValidationException;
import com.broadleafcommerce.data.tracking.core.context.ContextInfo;

public class DefaultBulkOperationsService implements BulkOperationsService {

    public void validateBulkOperationRequest(BulkOperationRequest bulkOperationRequest,
            @Nullable ContextInfo contextInfo) {
        Errors errors =
                new BeanPropertyBindingResult(bulkOperationRequest, "bulkOperationRequest");

        rejectIfEmptyOrWhitespace(errors, "operationType", "missingOperationType",
                "Operation type is required");

        if (errors.hasErrors()) {
            throw new ValidationException(errors);
        }
    }
}
