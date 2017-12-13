package com.commercetools.sync.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.queries.QuerySort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

class QueryAll<T extends Resource, S, C extends QueryDsl<T, C>> {
    private final SphereClient client;
    private Function<List<T>, S> pageMapper;
    private Consumer<List<T>> pageConsumer;

    private final CompletionStage<PagedQueryResult<T>> pagedResult;
    private final List<S> mappedResultsTillNow;

    private final QueryDsl<T, C> baseQuery;
    private final long pageSize;

    private QueryAll(@Nonnull final SphereClient client,
                     @Nonnull final List<S> mappedResultsTillNow,
                     @Nullable final CompletionStage<PagedQueryResult<T>> pagedResult,
                     @Nonnull final QueryDsl<T, C> baseQuery,
                     final long pageSize) {
        this.client = client;
        this.mappedResultsTillNow = mappedResultsTillNow;
        this.pagedResult = pagedResult;
        this.baseQuery = baseQuery;
        this.pageSize = pageSize;
    }

    private QueryAll(@Nonnull final SphereClient client,
                     @Nonnull final QueryDsl<T, C> baseQuery,
                     final long pageSize) {
        this.client = client;
        this.baseQuery = !baseQuery.sort().isEmpty() ? baseQuery : baseQuery.withSort(QuerySort.of("id asc"));
        this.pageSize = pageSize;

        this.mappedResultsTillNow = new ArrayList<>();
        this.pagedResult = queryPage(client);
    }

    @Nonnull
    static <T extends Resource, S, C extends QueryDsl<T, C>> QueryAll<T, S, C> of(
        @Nonnull final SphereClient client,
        @Nonnull final QueryDsl<T, C> baseQuery,
        final int pageSize) {
        return new QueryAll<>(client, baseQuery, pageSize);
    }

    @Nonnull
    CompletionStage<List<S>> run(final Function<List<T>, S> pageMapper) {
        this.pageMapper = pageMapper;
        return queryNextPages(this)
            .thenApply(nextPage -> nextPage.mappedResultsTillNow);
    }

    @Nonnull
    CompletionStage<Void> run(final Consumer<List<T>> pageConsumer) {
        this.pageConsumer = pageConsumer;
        return queryNextPages(this).thenAccept(result -> { });
    }

    private CompletionStage<QueryAll<T, S, C>> queryNextPages(final QueryAll<T, S, C> currentPageFetcher) {
        final CompletionStage<PagedQueryResult<T>> currentPageStage = currentPageFetcher.pagedResult;
        if (currentPageStage != null) {
            return currentPageStage.thenCompose(currentPage -> {
                final QueryAll<T, S, C> nextPageFetcher =
                    currentPageFetcher.processPageAndGetNextPageFetcher(currentPage);
                return queryNextPages(nextPageFetcher);
            });
        }
        return completedFuture(currentPageFetcher);
    }

    private QueryAll<T, S, C> processPageAndGetNextPageFetcher(@Nonnull final PagedQueryResult<T> page) {
        final List<T> currentPageElements = page.getResults();
        CompletionStage<PagedQueryResult<T>> nextPageStage = null;

        if (currentPageElements.size() == pageSize) {
            mapOrConsume(currentPageElements);
            nextPageStage = getNextPageStage(currentPageElements);
        } else {
            if (currentPageElements.size() > 0) {
                mapOrConsume(currentPageElements);
            }
        }
        return new QueryAll<>(client, mappedResultsTillNow, nextPageStage, baseQuery, pageSize);
    }

    private void mapOrConsume(@Nonnull final List<T> pageElements) {
        if (pageConsumer != null) {
            pageConsumer.accept(pageElements);
        } else {
            mappedResultsTillNow.add(pageMapper.apply(pageElements));
        }
    }

    @Nonnull
    private CompletionStage<PagedQueryResult<T>> getNextPageStage(@Nonnull final List<T> currentPageElements) {
        final String lastElementId = currentPageElements.get(currentPageElements.size() - 1).getId();
        final QueryPredicate<T> queryPredicate = QueryPredicate.of(format("id > \"%s\"", lastElementId));
        return queryPage(client, queryPredicate);
    }

    @Nonnull
    private CompletionStage<PagedQueryResult<T>> queryPage(@Nonnull final SphereClient client,
                                                           @Nullable final QueryPredicate<T> queryPredicate) {
        final QueryDsl<T, C> query = baseQuery
            .withLimit(this.pageSize)
            .withSort(QuerySort.of("id asc"));
        return client.execute(queryPredicate != null ? query.withPredicates(queryPredicate) : query);
    }

    @Nonnull
    private CompletionStage<PagedQueryResult<T>> queryPage(@Nonnull final SphereClient client) {
        return queryPage(client, null);
    }
}
