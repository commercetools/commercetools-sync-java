package com.commercetools.sync.commons.helpers;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QuerySort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

final class QueryAll<T, C extends QueryDsl<T, C>> {
    private final QueryDsl<T, C> baseQuery;
    private final long pageSize;

    private QueryAll(final QueryDsl<T, C> baseQuery, final long pageSize) {
        this.baseQuery = !baseQuery.sort().isEmpty() ? baseQuery : baseQuery.withSort(QuerySort.of("id asc"));
        this.pageSize = pageSize;
    }

    <S> CompletionStage<List<S>> run(final SphereClient client, final Function<List<T>, S> callback) {
        return queryPage(client, 0).thenCompose(result -> {
            final List<CompletableFuture<S>> futureResults = new ArrayList<>();

            final S callbackResult = callback.apply(result.getResults());
            final List<CompletableFuture<S>> nextPagesCallbackResults =
                queryNextPages(client, result.getTotal(), callback);

            futureResults.add(completedFuture(callbackResult));
            futureResults.addAll(nextPagesCallbackResults);
            return transformListOfFuturesToFutureOfLists(futureResults);
        });
    }

    CompletionStage<Void> run(final SphereClient client, final Consumer<List<T>> consumer) {
        return queryPage(client, 0).thenCompose(result -> {
            final List<CompletableFuture<Void>> futureResults = new ArrayList<>();
            consumer.accept(result.getResults());
            futureResults.addAll(queryNextPages(client, result.getTotal(), consumer));
            return CompletableFuture.allOf(futureResults.toArray(new CompletableFuture[futureResults.size()]));
        });
    }

    private List<CompletableFuture<Void>> queryNextPages(final SphereClient client, final long totalElements,
                                                         final Consumer<List<T>> consumer) {
        final long totalPages = totalElements / pageSize;
        return LongStream.rangeClosed(1, totalPages)
                         .mapToObj(page -> queryPage(client, page)
                             .thenApply(PagedQueryResult::getResults)
                             .thenAccept(consumer)
                             .toCompletableFuture()).collect(toList());
    }

    private <S> List<CompletableFuture<S>> queryNextPages(final SphereClient client, final long totalElements,
                                                          final Function<List<T>, S> callback) {
        final long totalPages = totalElements / pageSize;
        return LongStream.rangeClosed(1, totalPages)
                         .mapToObj(page -> queryPage(client, page)
                             .thenApply(PagedQueryResult::getResults)
                             .thenApply(callback)
                             .toCompletableFuture())
                         .collect(toList());
    }

    private CompletionStage<PagedQueryResult<T>> queryPage(final SphereClient client, final long pageNumber) {
        final QueryDsl<T, C> query = baseQuery
            .withOffset(pageNumber * pageSize)
            .withLimit(pageSize);
        return client.execute(query);
    }

    private <S> CompletableFuture<List<S>> transformListOfFuturesToFutureOfLists(
        final List<CompletableFuture<S>> futures) {
        final CompletableFuture[] futuresAsArray = futures.toArray(new CompletableFuture[futures.size()]);
        return CompletableFuture.allOf(futuresAsArray)
                                .thenApply(x -> futures.stream()
                                                       .map(CompletableFuture::join)
                                                       .collect(Collectors.toList()));
    }

    static <T, C extends QueryDsl<T, C>> QueryAll<T, C> of(final QueryDsl<T, C> baseQuery, final int pageSize) {
        return new QueryAll<>(baseQuery, pageSize);
    }
}