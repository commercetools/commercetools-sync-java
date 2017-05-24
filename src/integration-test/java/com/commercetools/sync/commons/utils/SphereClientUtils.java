package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.queries.PagedQueryResult;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

public class SphereClientUtils {

    public static final SphereClient CTP_SOURCE_CLIENT = ClientConfigurationUtils.createClient(
        SphereClientConfig.of(
            System.getenv("SOURCE_PROJECT_KEY"),
            System.getenv("SOURCE_CLIENT_ID"),
            System.getenv("SOURCE_CLIENT_SECRET")));
    public static final SphereClient CTP_TARGET_CLIENT = ClientConfigurationUtils.createClient(
        SphereClientConfig.of(
            System.getenv("TARGET_PROJECT_KEY"),
            System.getenv("TARGET_CLIENT_ID"),
            System.getenv("TARGET_CLIENT_SECRET")));

    /**
     * Max limit that can be applied to a query in CTP.
     */
    public static final Long QUERY_MAX_LIMIT = 500L;

    /**
     * Fetches resources of {@link T} using query provided by {@code querySupplier}.
     * Then each resource from result list is deleted using command provided by {@code deleteFunction}.
     * Method blocks until above operations were done.
     *
     * @param client sphere client used to executing requests
     * @param querySupplier supplier of resources that need to be cleaned up
     * @param deleteFunction function that takes resource and returns its delete command
     * @param <T> type of resource to cleanup
     */
    public static <T> void cleanupTable(
        @Nonnull final SphereClient client,
        @Nonnull final Supplier<SphereRequest<PagedQueryResult<T>>> querySupplier,
        @Nonnull final Function<T, SphereRequest<T>> deleteFunction) {

        client.execute(querySupplier.get())
            .toCompletableFuture()
            .join()
            .getResults()
            .stream()
            .forEach(item -> client.execute(deleteFunction.apply(item)).toCompletableFuture().join());
    }
}
