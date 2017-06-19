package com.commercetools.sync.it.products;

import com.commercetools.sync.commons.utils.ClientConfigurationUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.queries.PagedQueryResult;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: re-factor with inventory/categories test sphere utils, when merged
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
     * Fetches resources of {@link T} using query provided by {@code query}.
     * Then each resource from result list is converted to {@link SphereRequest} using {@code ctpRequest}.
     * Then each request is executed by {@code client}.
     * Method blocks until above operations were done.
     *
     * <p>Example of use: you could provide a supplier which returns a query that will fetch all resources
     * of type {@link io.sphere.sdk.inventory.InventoryEntry}, and a function that returns delete command for given
     * inventory entry. It would result in deleting all inventory entries from the given CTP.
     *
     * @param client sphere client used to executing requests
     * @param query supplier of resources that need to be processed
     * @param ctpRequest function that takes resource and returns sphere request
     * @param <T> type of resource
     */
    public static <T> void fetchAndProcess(@Nonnull final SphereClient client,
                                           @Nonnull final Supplier<SphereRequest<PagedQueryResult<T>>> query,
                                           @Nonnull final Function<T, SphereRequest<T>> ctpRequest) {

        client.execute(query.get()).toCompletableFuture().join().getResults()
                .forEach(item -> client.execute(ctpRequest.apply(item)).toCompletableFuture().join());
    }
}
