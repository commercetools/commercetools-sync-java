package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.helpers.CtpClient;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.queries.PagedQueryResult;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

public class SphereClientUtils {

    //TODO provide values from conf file
    private static final String CTP_SOURCE_PROJECT_KEY = "";
    private static final String CTP_SOURCE_CLIENT_ID = "";
    private static final String CTP_SOURCE_CLIENT_SECRET = "";

    //TODO provide values from conf file
    private static final String CTP_TARGET_PROJECT_KEY = "";
    private static final String CTP_TARGET_CLIENT_ID = "";
    private static final String CTP_TARGET_CLIENT_SECRET = "";

    private static final CtpClient CTP_SOURCE_CLIENT = new CtpClient(SphereClientConfig.of(
        CTP_SOURCE_PROJECT_KEY, CTP_SOURCE_CLIENT_ID, CTP_SOURCE_CLIENT_SECRET));
    private static final CtpClient CTP_TARGET_CLIENT = new CtpClient(SphereClientConfig.of(
        CTP_TARGET_PROJECT_KEY, CTP_TARGET_CLIENT_ID, CTP_TARGET_CLIENT_SECRET));

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
        @Nonnull final BlockingSphereClient client,
        @Nonnull final Supplier<SphereRequest<PagedQueryResult<T>>> querySupplier,
        @Nonnull final Function<T, SphereRequest<T>> deleteFunction) {

        client.executeBlocking(querySupplier.get())
            .getResults()
            .stream()
            .forEach(item -> client.executeBlocking(deleteFunction.apply(item)));
    }

    /**
     * Returns initialized {@link CtpClient} instance that represent client of test's SOURCE project (the one that
     * would be synced into other project).
     *
     * @return {@link CtpClient} of "source" project
     */
    public static CtpClient getCtpClientOfSourceProject() {
        return CTP_SOURCE_CLIENT;
    }

    /**
     * Returns initialized {@link CtpClient} instance that represent client of test's TARGET project (the one to
     * which other project would be synced into).
     *
     * @return {@link CtpClient} of "target" project
     */
    public static CtpClient getCtpClientOfTargetProject() {
        return CTP_TARGET_CLIENT;
    }
}
