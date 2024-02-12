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
package com.broadleafcommerce.bulkoperations;


import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.MariaUtilityProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.MariaVerificationProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.MySqlUtilityProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.MySqlVerificationProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.OracleUtilityProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.OracleVerificationProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.PostgresUtilityProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.PostgresVerificationProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.YugabyteUtilityProvider;
import static com.broadleafcommerce.common.jpa.schema.SchemaCompatibiltyUtility.YugabyteVerificationProvider;

import org.junit.jupiter.api.Nested;
import org.springframework.test.context.TestPropertySource;

/**
 * Verify that we can start up against RDBMS using the known schema configuration. The
 * {@code Utility} test class is intended for use by developers to keep JPA entity changes in sync
 * with the Liquibase change logs. {@code VerificationIT} is intended only for use by the CI
 * pipeline during a release to validate the JPA entity state and Liquibase change log state are in
 * sync.
 *
 * @author Jeff Fischer
 */
public class UtilitiesIT {

    // @formatter:off
    /**
     *  #####                               #     #
     * #     # #####   ##   #####  #####    #     # ###### #####  ######
     * #         #    #  #  #    #   #      #     # #      #    # #
     *  #####    #   #    # #    #   #      ####### #####  #    # #####
     *       #   #   ###### #####    #      #     # #      #####  #
     * #     #   #   #    # #   #    #      #     # #      #   #  #
     *  #####    #   #    # #    #   #      #     # ###### #    # ######
     *
     * Execute these utility tests directly from the IDE in order to update the liquibase
     * change logs for each supported RDBMS platform based on the current JPA entity state. Updated
     * Liquibase change logs are emitted at src/main/resources/db/changelog.
     */
    // @formatter:on
    public static class AllUtilities {

        @TestPropertySource(properties = {"service.key=bulkoperations",
                "broadleaf.bulkoperations.database.provider=jpa"})
        @Nested
        public class MariaUtility extends MariaUtilityProvider {}

        @TestPropertySource(properties = {"service.key=bulkoperations",
                "broadleaf.bulkoperations.database.provider=jpa"})
        @Nested
        public class PostgresUtility extends PostgresUtilityProvider {}

        @TestPropertySource(properties = {"service.key=bulkoperations",
                "broadleaf.bulkoperations.database.provider=jpa"})
        @Nested
        public class OracleUtility extends OracleUtilityProvider {}

        @TestPropertySource(properties = {"service.key=bulkoperations",
                "broadleaf.bulkoperations.database.provider=jpa"})
        @Nested
        public class MySqlUtility extends MySqlUtilityProvider {}

        @TestPropertySource(properties = {"service.key=bulkoperations",
                "broadleaf.bulkoperations.database.provider=jpa"})
        @Nested
        public class YugabyteUtility extends YugabyteUtilityProvider {}

    }

    /**
     * The Verification test classes are intended to only be run by the CI pipeline.
     */
    @TestPropertySource(properties = {"service.key=bulkoperations",
            "broadleaf.bulkoperations.database.provider=jpa"})
    public static class MariaVerificationIT extends MariaVerificationProvider {}
    @TestPropertySource(properties = {"service.key=bulkoperations",
            "broadleaf.bulkoperations.database.provider=jpa"})
    public static class PostgresVerificationIT extends PostgresVerificationProvider {}
    @TestPropertySource(properties = {"service.key=bulkoperations",
            "broadleaf.bulkoperations.database.provider=jpa"})
    public static class OracleVerificationIT extends OracleVerificationProvider {}
    @TestPropertySource(properties = {"service.key=bulkoperations",
            "broadleaf.bulkoperations.database.provider=jpa"})
    public static class MySqlVerificationIT extends MySqlVerificationProvider {}
    @TestPropertySource(properties = {"service.key=bulkoperations",
            "broadleaf.bulkoperations.database.provider=jpa"})
    public static class YugabyteVerificationIT extends YugabyteVerificationProvider {}
}
