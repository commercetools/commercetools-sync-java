package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.queries.PagedQueryResult;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.ClientConfigurationUtils.createClient;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;

public class SphereClientUtils {

    public static final String IT_PROPERTIES = "it.properties";

    public static final SphereClientConfig CTP_SOURCE_CLIENT_CONFIG = getCtpSourceClientConfig();
    public static final SphereClientConfig CTP_TARGET_CLIENT_CONFIG = getCtpTargetClientConfig();

    public static final SphereClient CTP_SOURCE_CLIENT = createClient(CTP_SOURCE_CLIENT_CONFIG);
    public static final SphereClient CTP_TARGET_CLIENT = createClient(CTP_TARGET_CLIENT_CONFIG);

    /**
     * Max limit that can be applied to a query in CTP.
     */
    public static final Long QUERY_MAX_LIMIT = 500L;

    /**
     * Fetches resources of {@code T} using a {@code query}. The {@code ctpRequest} is applied on each resultant
     * resource from fetching, to make a {@link SphereRequest}. Then each request is executed by {@code client}.
     * Method blocks until above operations were done.
     *
     * <p>Example of use: you could provide a query that will fetch all resources of type
     * {@link io.sphere.sdk.inventory.InventoryEntry}, and a function that returns delete command for given
     * inventory entry. It would result in deleting all inventory entries from the given CTP.
     *
     * @param client     sphere client used to executing requests
     * @param query      query of resources that need to be processed
     * @param ctpRequest function that takes resource and returns sphere request
     * @param <T>        type of resource
     */
    public static <T> void fetchAndProcess(@Nonnull final SphereClient client,
                                           @Nonnull final SphereRequest<PagedQueryResult<T>> query,
                                           @Nonnull final Function<T, SphereRequest<T>> ctpRequest) {
        client.execute(query)
              .thenCompose(pagedQueryResult -> allOf(
                  pagedQueryResult.getResults().stream()
                                  .map(item -> client.execute(ctpRequest.apply(item)))
                                  .map(CompletionStage::toCompletableFuture)
                                  .collect(Collectors.toList())
                                  .toArray(new CompletableFuture[pagedQueryResult.getResults().size()]))
              ).toCompletableFuture().join();
    }

    private static SphereClientConfig getCtpSourceClientConfig() {
        return getCtpClientConfig("source.", "SOURCE");
    }

    private static SphereClientConfig getCtpTargetClientConfig() {
        return getCtpClientConfig("target.", "TARGET");
    }

    private static SphereClientConfig getCtpClientConfig(@Nonnull final String propertiesPrefix,
                                                         @Nonnull final String envVarPrefix) {
        try {
            InputStream propStream = SphereClientUtils.class.getClassLoader().getResourceAsStream(IT_PROPERTIES);
            if (propStream != null) {
                Properties itProperties = new Properties();
                itProperties.load(propStream);
                return SphereClientConfig.ofProperties(itProperties, propertiesPrefix);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(format("IT properties file \"%s\" found, but CTP properties"
                    + " for prefix \"%s\" can't be read", IT_PROPERTIES, propertiesPrefix), exception);
        }

        return SphereClientConfig.ofEnvironmentVariables(envVarPrefix);
    }

}

