package com.commercetools.sync.commons.helpers;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryDsl;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.queries.QuerySort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

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

    public void processCurrentPage() {
        if (pagedResult != null) {
            pagedResult.thenAccept(pagedQueryResult -> {
                final List<T> pageElements = pagedQueryResult.getResults();

                if (pageElements.size() > 0) {
                    final S mappedPage = pageMapper.apply(pageElements);
                    mappedResultsTillNow.add(mappedPage);
                }
            });
        }
    }

    public FetchAllHelper<T, S, C> copy(@Nullable final CompletionStage<PagedQueryResult<T>> pagedResult) {
        return new FetchAllHelper<>(this.client, this.pageMapper, this.mappedResultsTillNow, pagedResult, this.baseQuery, this.pageSize);
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
