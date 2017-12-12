package com.commercetools.sync.commons.helpers;

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
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;

public class FetchAllHelper<T extends Resource, S, C extends QueryDsl<T, C>> {
    private final SphereClient client;
    private final Function<List<T>, S> pageMapper;
    private final CompletionStage<PagedQueryResult<T>> pagedResult;
    private final List<S> mappedResultsTillNow;

    private final QueryDsl<T, C> baseQuery;
    private final long pageSize;

    public SphereClient getClient() {
        return client;
    }

    public Function<List<T>, S> getPageMapper() {
        return pageMapper;
    }

    public List<S> getMappedResultsTillNow() {
        return mappedResultsTillNow;
    }

    public CompletionStage<PagedQueryResult<T>> getPagedResult() {
        return pagedResult;
    }

    public FetchAllHelper(@Nonnull final SphereClient client,
                          @Nonnull final Function<List<T>, S> pageMapper,
                          @Nonnull final List<S> mappedResultsTillNow,
                          @Nullable final CompletionStage<PagedQueryResult<T>> pagedResult,
                          @Nonnull final QueryDsl<T, C> baseQuery,
                          final long pageSize) {
        this.client = client;
        this.pageMapper = pageMapper;
        this.mappedResultsTillNow = mappedResultsTillNow;
        this.pagedResult = pagedResult;
        this.baseQuery = baseQuery;
        this.pageSize = pageSize;
    }

    public FetchAllHelper(@Nonnull final SphereClient client,
                          @Nonnull final Function<List<T>, S> pageMapper,
                          @Nonnull final QueryDsl<T, C> baseQuery,
                          final long pageSize) {
        this.client = client;
        this.pageMapper = pageMapper;
        this.baseQuery = baseQuery;
        this.pageSize = pageSize;

        this.mappedResultsTillNow = new ArrayList<>();
        this.pagedResult = queryPage(client);
    }

    public CompletionStage<List<S>> fetchAll() {
        return fetchNextPages(this).thenApply(FetchAllHelper::getMappedResultsTillNow);
    }

    private CompletionStage<FetchAllHelper<T, S, C>> fetchNextPages(final FetchAllHelper<T, S, C> currentPageFetcher) {
        final CompletionStage<PagedQueryResult<T>> currentPageStage = currentPageFetcher.getPagedResult();
        if (currentPageStage != null) {
            return currentPageStage.thenCompose(currentPage -> {
                final FetchAllHelper<T, S, C> nextPageFetcher =
                    currentPageFetcher.processPageAndGetNextPageFetcher(currentPage);
                return fetchNextPages(nextPageFetcher);
            });
        }
        return completedFuture(currentPageFetcher);
    }

    private FetchAllHelper<T, S, C> processPageAndGetNextPageFetcher(@Nonnull final PagedQueryResult<T> page) {
        final List<T> currentPageElements = page.getResults();
        CompletionStage<PagedQueryResult<T>> nextPageStage = null;

        if (currentPageElements.size() > 0) {
            applyPageMapperAndAppendResults(page);
            nextPageStage = getNextPageStage(currentPageElements);
        }
        return new FetchAllHelper<>(client, pageMapper, mappedResultsTillNow, nextPageStage, baseQuery, pageSize);
    }

    private void applyPageMapperAndAppendResults(@Nonnull final PagedQueryResult<T> page) {
        final List<T> pageElements = page.getResults();
        if (pageElements.size() > 0) {
            final S mappedPage = pageMapper.apply(pageElements);
            mappedResultsTillNow.add(mappedPage);
        }
    }

    @Nonnull
    private CompletionStage<PagedQueryResult<T>> getNextPageStage(@Nonnull final List<T> currentPageElements) {
        final String lastElementId = currentPageElements.get(currentPageElements.size() - 1).getId();
        final QueryPredicate<T> queryPredicate = QueryPredicate.of(format("id > \"%s\"", lastElementId));
        return queryPage(client, queryPredicate);
    }

    @Nonnull
    CompletionStage<PagedQueryResult<T>> queryPage(@Nonnull final SphereClient client,
                                                   @Nullable final QueryPredicate<T> queryPredicate) {
        final QueryDsl<T, C> query = baseQuery
            .withLimit(this.pageSize)
            .withSort(QuerySort.of("id asc"));
        return client.execute(queryPredicate != null ? query.withPredicates(queryPredicate) : query);
    }

    @Nonnull
    CompletionStage<PagedQueryResult<T>> queryPage(@Nonnull final SphereClient client) {
        return queryPage(client, null);
    }

}
