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
package com.broadleafcommerce.bulkoperations.domain.types;

import org.apache.commons.lang3.StringUtils;

public enum BulkOperationSubstatusType {
    /**
     * The bulk operation has been created, but hasn't started initializing or processing yet.
     */
    PENDING,

    /**
     * The bulk operation is currently initializing items.
     */
    INITIALIZING_ITEMS,

    /**
     * The bulk operation is currently processing items.
     */
    PROCESSING,

    /**
     * The bulk operation was completed successfully.
     */
    SUCCESS,

    /**
     * The bulk operation was cancelled before completion.
     */
    CANCELED,

    /**
     * The bulk operation failed to process.
     */
    FAILURE;

    public static boolean isPending(String subStatus) {
        return StringUtils.equals(subStatus, BulkOperationSubstatusType.PENDING.name());
    }

    public static boolean isInitializingItems(String subStatus) {
        return StringUtils.equals(subStatus, BulkOperationSubstatusType.INITIALIZING_ITEMS.name());
    }

    public static boolean isProcessing(String subStatus) {
        return StringUtils.equals(subStatus, BulkOperationSubstatusType.PROCESSING.name());
    }

    public static boolean isSuccess(String subStatus) {
        return StringUtils.equals(subStatus, BulkOperationSubstatusType.SUCCESS.name());
    }

    public static boolean isCanceled(String subStatus) {
        return StringUtils.equals(subStatus, BulkOperationSubstatusType.CANCELED.name());
    }

    public static boolean isFailure(String subStatus) {
        return StringUtils.equals(subStatus, BulkOperationSubstatusType.FAILURE.name());
    }
}
