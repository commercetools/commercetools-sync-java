package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.types.commands.TypeDeleteCommand;
import io.sphere.sdk.types.queries.TypeQuery;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.queries.QueryExecutionUtils.queryAll;

public final class ITUtils {

    /**
     * Deletes all Types from CTP projects defined by the {@code sphereClient}
     *
     * @param ctpClient defines the CTP project to delete the Types from.
     */
    public static void deleteTypes(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, TypeQuery::of, TypeDeleteCommand::of);
    }

    /**
     * Deletes all Types from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteTypesFromTargetAndSource() {
        deleteTypes(CTP_TARGET_CLIENT);
        deleteTypes(CTP_SOURCE_CLIENT);
    }

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link BaseSyncStatistics}.
     * Note: The order of the fields in the built JSON String depends on the order of the instance variables in this
     * class.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON String.
     * @return a JSON String representation of the statistics object.
     */
    public static String getStatisticsAsJSONString(@Nonnull final BaseSyncStatistics statistics)
        throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(statistics);
    }

    /**
     * Applies the {@code resourceToRequestMapper} function on each page, fetched from the supplied
     * {@code queryRequestSupplier} on the supplied {@code ctpClient}, to map each resource to a {@link SphereRequest}
     * and then executes these requests in parallel within each page.
     *
     * @param ctpClient               defines the CTP project to apply the query on.
     * @param queryRequestSupplier    defines a supplier which, when executed, returns the query that should be made on
     *                                the CTP project.
     * @param resourceToRequestMapper defines a mapper function that should be applied on each resource, in the fetched
     *                                page from the query on the specified CTP project, to map it to a
     *                                {@link SphereRequest}.
     */
    public static <T extends Resource, C extends QueryDsl<T, C>> void queryAndExecute(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final Supplier<QueryDsl<T, C>> queryRequestSupplier,
        @Nonnull final Function<T, SphereRequest<T>> resourceToRequestMapper) {

        queryAndCompose(ctpClient, queryRequestSupplier,
            resource -> ctpClient.execute(resourceToRequestMapper.apply(resource)));
    }

    /**
     * Applies the {@code resourceToStageMapper} function on each page, fetched from the supplied
     * {@code queryRequestSupplier} on the supplied {@code ctpClient}, to map each resource to a {@link CompletionStage}
     * and then executes these stages in parallel within each page.
     *
     * @param ctpClient             defines the CTP project to apply the query on.
     * @param queryRequestSupplier  defines a supplier which, when executed, returns the query that should be made on
     *                              the CTP project.
     * @param resourceToStageMapper defines a mapper function that should be applied on each resource, in the fetched
     *                              page from the query on the specified CTP project, to map it to a
     *                              {@link CompletionStage}.
     */
    public static <T extends Resource, C extends QueryDsl<T, C>, S> void queryAndCompose(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final Supplier<QueryDsl<T, C>> queryRequestSupplier,
        @Nonnull final Function<T, CompletionStage<S>> resourceToStageMapper) {

        queryAll(ctpClient, queryRequestSupplier.get(), resourceToStageMapper)
            .thenApply(Collection::stream)
            .thenCompose(ITUtils::toAllOf)
            .toCompletableFuture().join();
    }

    private static <T> CompletionStage<Void> toAllOf(@Nonnull final Stream<? extends CompletionStage<T>> stageStream) {
        return CompletableFuture.allOf(stageStream.toArray(CompletableFuture[]::new));
    }
}
