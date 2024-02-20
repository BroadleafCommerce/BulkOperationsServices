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
package com.broadleafcommerce.bulkoperations.service.autoconfigure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.broadleafcommerce.bulk.v2.messaging.BulkOpsInitializeItemsRequestProducer;
import com.broadleafcommerce.bulk.v2.messaging.sandbox.CreateSandboxRequestProducer;
import com.broadleafcommerce.bulkoperations.domain.CatalogItem;
import com.broadleafcommerce.bulkoperations.oauth2.client.endpoint.OAuth2ClientCredentialsAccessTokenResponseClient;
import com.broadleafcommerce.bulkoperations.oauth2.client.web.SynchronizedDelegatingOAuth2AuthorizedClientManager;
import com.broadleafcommerce.bulkoperations.service.BulkOperationsService;
import com.broadleafcommerce.bulkoperations.service.DefaultBulkOperationsService;
import com.broadleafcommerce.bulkoperations.service.environment.BulkOperationsProviderProperties;
import com.broadleafcommerce.bulkoperations.service.environment.RouteConstants;
import com.broadleafcommerce.bulkoperations.service.handler.BulkOperationHandler;
import com.broadleafcommerce.bulkoperations.service.handler.CatalogBulkOperationHandler;
import com.broadleafcommerce.bulkoperations.service.provider.CatalogProvider;
import com.broadleafcommerce.bulkoperations.service.provider.SearchProvider;
import com.broadleafcommerce.bulkoperations.service.provider.external.ExternalCatalogProperties;
import com.broadleafcommerce.bulkoperations.service.provider.external.ExternalCatalogProvider;
import com.broadleafcommerce.bulkoperations.service.provider.external.ExternalSearchProperties;
import com.broadleafcommerce.bulkoperations.service.provider.external.ExternalSearchProvider;
import com.broadleafcommerce.bulkoperations.service.provider.utils.ProviderUtils;
import com.broadleafcommerce.common.extension.TypeFactory;
import com.broadleafcommerce.common.extension.data.DataRouteSupporting;
import com.broadleafcommerce.common.extension.data.PackageDataRouteSupplier;
import com.broadleafcommerce.common.messaging.notification.DetachedDurableMessageSender;
import com.broadleafcommerce.data.tracking.core.context.ContextInfoCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.function.Supplier;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({SSLVerificationProperties.class, ExternalSearchProperties.class,
        ExternalCatalogProperties.class})
public class BulkOperationsServiceAutoConfiguration {

    /**
     * We'll leave this with a general name as it can (and should) be reused in a flex package.
     *
     * @param clientRegistrations
     * @param sslVerificationProperties
     * @return
     */
    @Bean(name = "oAuth2FilterFunctionSupplier")
    @ConditionalOnMissingBean(name = "oAuth2FilterFunctionSupplier")
    public Supplier<ServletOAuth2AuthorizedClientExchangeFilterFunction> bulkOperationsOauth2FilterFunctionSupplier(
            ClientRegistrationRepository clientRegistrations,
            @Qualifier("bulkOperationsClientHttpConnector") Optional<ClientHttpConnector> clientHttpConnector) {
        final SynchronizedDelegatingOAuth2AuthorizedClientManager manager =
                new SynchronizedDelegatingOAuth2AuthorizedClientManager(clientRegistrations);
        manager.setAuthorizedClientProvider(
                getClientCredentialsAuthorizedClientProvider(clientHttpConnector));
        return () -> new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
    }

    // TODO: possibly move this to a broadleaf oauth2 client dependency
    // https://github.com/BroadleafCommerce/MicroPM/issues/1924
    private static OAuth2AuthorizedClientProvider getClientCredentialsAuthorizedClientProvider(
            Optional<ClientHttpConnector> clientHttpConnector) {
        return OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials(builder -> {
            WebClient.Builder webClientBuilder = WebClient.builder();
            clientHttpConnector.ifPresent(webClientBuilder::clientConnector);

            builder.accessTokenResponseClient(
                    new OAuth2ClientCredentialsAccessTokenResponseClient(
                            webClientBuilder.build()));
        }).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "bulkOperationsWebClient")
    public WebClient bulkOperationsWebClient(
            @Qualifier("oAuth2FilterFunctionSupplier") Supplier<ServletOAuth2AuthorizedClientExchangeFilterFunction> oauth2FilterSupplier,
            ObjectMapper objectMapper,
            @Qualifier("bulkOperationsClientHttpConnector") Optional<ClientHttpConnector> clientHttpConnector) {
        // Add our own object mapper
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();

        WebClient.Builder webClientBuilder = WebClient.builder();
        clientHttpConnector.ifPresent(webClientBuilder::clientConnector);

        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        return webClientBuilder
                .uriBuilderFactory(uriBuilderFactory)
                .exchangeStrategies(strategies)
                .apply(oauth2FilterSupplier.get().oauth2Configuration())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "bulkOperationsClientHttpConnector")
    public ClientHttpConnector bulkOperationsClientHttpConnector(
            SSLVerificationProperties sslVerificationProperties) throws SSLException {
        // TODO: possibly move this to a broadleaf oauth2 client dependency
        // https://github.com/BroadleafCommerce/MicroPM/issues/1924
        // TODO: disable ssl verification for WebClient that uses this, remove this once
        // https://github.com/BroadleafCommerce/MicroPM/issues/1323 is completed
        if (sslVerificationProperties.isDisabled()) {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            HttpClient httpClient = HttpClient
                    .create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            return new ReactorClientHttpConnector(httpClient);
        }

        return null; // WebClient builder will initialize the default ClientHttpConnector
    }


    /**
     * Defines a {@link DataRouteSupporting} for Bulk Ops. By default, this is detached from any
     * persistence and is used for supporting {@link ContextInfoCustomizer} in a flexpackage
     * configuration to ensure it is only invoked for this specific service.
     */
    @Bean(name = "bulkOperationsSource")
    @ConditionalOnMissingBean(name = "bulkOperationsSource")
    public DataRouteSupporting bulkOperationsSource() {
        return new OrchestrationDataRouteSupporting(RouteConstants.Persistence.BULK_OPS_ROUTE_KEY);
    }

    @Bean(name = "bulkOperationsRouteSupplier")
    @ConditionalOnMissingBean(name = "bulkOperationsRouteSupplier")
    public PackageDataRouteSupplier<DataRouteSupporting> bulkOperationsRouteSupplier(
            @Nullable @Qualifier("bulkOperationsSource") DataRouteSupporting route) {
        return () -> new PackageDataRouteSupplier.PackageMapping<>(
                RouteConstants.Persistence.BULK_OPS_ROUTE_PACKAGE,
                route);
    }

    @RequiredArgsConstructor
    private static class OrchestrationDataRouteSupporting implements DataRouteSupporting {

        private final String routeKey;

        @Override
        public String getLookupKey() {
            return routeKey;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    @Bean
    @ConditionalOnMissingBean
    BulkOperationHandler catalogBulkOperationHandler(
            CatalogProvider<? extends CatalogItem> catalogProvider,
            DetachedDurableMessageSender sender,
            BulkOperationsProviderProperties providerProperties,
            CreateSandboxRequestProducer createSandboxRequestProducer,
            BulkOpsInitializeItemsRequestProducer bulkOpsInitializeItemsRequestProducer,
            MessageSource messageSource,
            TypeFactory typeFactory) {
        return new CatalogBulkOperationHandler(catalogProvider,
                sender,
                providerProperties,
                createSandboxRequestProducer,
                bulkOpsInitializeItemsRequestProducer,
                messageSource,
                typeFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "bulkOpsCatalogProvider")
    CatalogProvider<? extends CatalogItem> bulkOpsCatalogProvider(
            @Qualifier("bulkOperationsWebClient") WebClient bulkOpsWebClient,
            TypeFactory typeFactory,
            ProviderUtils providerUtils,
            ExternalCatalogProperties properties) {
        return new ExternalCatalogProvider<>(bulkOpsWebClient,
                typeFactory,
                providerUtils,
                properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "bulkOpsSearchProvider")
    SearchProvider<? extends CatalogItem> bulkOpsSearchProvider(
            @Qualifier("bulkOperationsWebClient") WebClient bulkOpsWebClient,
            TypeFactory typeFactory,
            ProviderUtils providerUtils,
            ExternalSearchProperties properties) {
        return new ExternalSearchProvider<>(bulkOpsWebClient,
                typeFactory,
                providerUtils,
                properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ProviderUtils bulkOpsProviderUtils(ObjectMapper objectMapper) {
        return new ProviderUtils(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public BulkOperationsService bulkOperationsService() {
        return new DefaultBulkOperationsService();
    }
}
