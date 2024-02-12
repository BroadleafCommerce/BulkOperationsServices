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
package com.broadleafcommerce.bulkoperations.autoconfigure;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default property overrides for Bulk Operations Service
 */
public class BulkOperationsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String YAML_CLASSPATH_LOCATION = "bulkoperations-defaults.yml";

    private YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        try {
            loader.load("Broadleaf Bulk Operations Service -- Defaults",
                    new ClassPathResource(YAML_CLASSPATH_LOCATION))
                    .forEach(ps -> environment.getPropertySources().addLast(ps));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load the Bulk Operations service property defaults",
                    e);
        }

        if ("none".equals(environment.getProperty("broadleaf.database.provider"))) {
            String exclusionProperties =
                    StringUtils.join(List.of(DataSourceAutoConfiguration.class.getName()), ",");
            String exclusions = environment.getProperty("spring.autoconfigure.exclude");
            if (StringUtils.isNotEmpty(exclusions)) {
                exclusions += "," + exclusionProperties;
            } else {
                exclusions = exclusionProperties;
            }
            Set<String> removeDups = Arrays.stream(exclusions.split(",")).map(String::trim)
                    .collect(Collectors.toSet());
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("disable-jpa",
                            Collections.singletonMap("spring.autoconfigure.exclude",
                                    StringUtils.join(removeDups, ","))));
        }
    }

}
