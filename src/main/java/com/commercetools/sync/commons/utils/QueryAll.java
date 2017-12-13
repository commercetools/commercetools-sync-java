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

final class QueryAll<T extends Resource, S, C extends QueryDsl<T, C>> {
    private final SphereClient client;
    private Function<List<T>, S> pageMapper;
    private Consumer<List<T>> pageConsumer;
    private final CompletionStage<PagedQueryResult<T>> pagedResult;
    private final List<S> mappedResultsTillNow;
    private final QueryDsl<T, C> baseQuery;
    private final long pageSize;

    private QueryAll(@Nonnull final SphereClient client,
                     @Nonnull final QueryDsl<T, C> baseQuery,
                     final long pageSize) {

        this.client = client;
        this.baseQuery = !baseQuery.sort().isEmpty() ? baseQuery : baseQuery.withSort(QuerySort.of("id asc"));
        this.pageSize = pageSize;
        this.mappedResultsTillNow = new ArrayList<>();
        this.pagedResult = queryPage();
    }

    @Nonnull
    static <T extends Resource, S, C extends QueryDsl<T, C>> QueryAll<T, S, C> of(
        @Nonnull final SphereClient client,
        @Nonnull final QueryDsl<T, C> baseQuery,
        final int pageSize) {

        return new QueryAll<>(client, baseQuery, pageSize);
    }

    /**
     * Given a {@link Function} to a page of resources of type {@code T} that returns a mapped result of type {@code S},
     * this method sets this instance's {@code pageMapper} to the supplied value, then it makes requests to fetch the
     * entire result space of the resource {@code T} on CTP, while applying the function on each fetched page.
     *
     * @param pageMapper the function to apply on each fetched page of the result space.
     * @return a future containing a list of mapped results of type {@code S}, after the function applied all the pages.
     */
    @Nonnull
    CompletionStage<List<S>> run(@Nonnull final Function<List<T>, S> pageMapper) {
        this.pageMapper = pageMapper;
        return queryNextPages(pagedResult)
            .thenApply(nextPage -> nextPage.mappedResultsTillNow);
    }

    /**
     * Given a {@link Consumer} to a page of resources of type {@code T}, this method sets this instance's
     * {@code pageConsumer} to the supplied value, then it makes requests to fetch the entire result space of the
     * resource {@code T} on CTP, while accepting the consumer on each fetched page.
     *
     * @param pageConsumer the consumer to accept on each fetched page of the result space.
     * @return a future containing void after the consumer accepted all the pages.
     */
    @Nonnull
    CompletionStage<Void> run(@Nonnull final Consumer<List<T>> pageConsumer) {
        this.pageConsumer = pageConsumer;
        return queryNextPages(pagedResult).thenAccept(result -> { });
    }

    /**
     * Given a future containing a current page result {@link PagedQueryResult}, this method checks if the future is not
     * null. If it is not, then it processes the result and attempts to fetch a next page, then it recursivley calls
     * itself on the next page. If there is no next page, then the future would be null and this method would just
     * return a future containing this instance of {@link QueryAll} as a result.
     *
     * @param currentPageStage a future containing a result {@link PagedQueryResult}.
     * @return a future containing this instance of {@link QueryAll} as a result.
     */
    @Nonnull
    private CompletionStage<QueryAll<T, S, C>> queryNextPages(
        @Nullable final CompletionStage<PagedQueryResult<T>> currentPageStage) {
        if (currentPageStage != null) {
            return currentPageStage
                .thenCompose(currentPage -> queryNextPages(processPageAndGetNext(currentPage)));
        }
        return completedFuture(this);
    }

    /**
     * Given a result {@link PagedQueryResult}, this method checks if there are elements in the result (size > 0), then
     * it maps or consumes the resultant list using this instance's {@code pageMapper} or {code pageConsumer} whichever
     * is available. Then, it checks if this page is the last page or not (by checking if the result size is equal to
     * this instance's {@code pageSize}). If It is, then it means there might be still more results. However, if
     * it is less, then it means for sure there are no more results and this is the last page. If there is a next page,
     * then a new future of the next page is returned. If there are no more results, the method returns null.
     *
     * @param page the current page result.
     * @return If there is a next page, then a new future of the next page is returned. If there are no more results,
     *         the method returns null.
     */
    @Nullable
    private CompletionStage<PagedQueryResult<T>> processPageAndGetNext(@Nonnull final PagedQueryResult<T> page) {
        final List<T> currentPageElements = page.getResults();
        if (currentPageElements.size() == pageSize) {
            mapOrConsume(currentPageElements);
            return getNextPageStage(currentPageElements);
        } else {
            if (currentPageElements.size() > 0) {
                mapOrConsume(currentPageElements);
            }
        }
        return null;
    }

    /**
     * Given a list of page elements of resource {@code T}, this method checks if this instance's {@code pageConsumer}
     * or {@code pageMapper} is set (not null). The one which is set is then applied on the list of page elements.
     *
     *
     * @param pageElements list of page elements of resource {@code T}.
     */
    private void mapOrConsume(@Nonnull final List<T> pageElements) {
        if (pageConsumer != null) {
            pageConsumer.accept(pageElements);
        } else {
            mappedResultsTillNow.add(pageMapper.apply(pageElements));
        }
    }

    /**
     * Given a list of page elements of resource {@code T}, this method gets the id of the last element in the list
     * and creates a future containing the fetched results which have an id greater than the id of the last element
     * in the list.
     *
     * @param pageElements list of page elements of resource {@code T}.
     * @return a future containing the fetched results which have an id greater than the id of the last element
     *          in the list.
     */
    @Nonnull
    private CompletionStage<PagedQueryResult<T>> getNextPageStage(@Nonnull final List<T> pageElements) {
        final String lastElementId = pageElements.get(pageElements.size() - 1).getId();
        final QueryPredicate<T> queryPredicate = QueryPredicate.of(format("id > \"%s\"", lastElementId));
        return queryPage(queryPredicate);
    }

    /**
     * Gets the results of {@link this} instance's query with a limit of this instance's {@code pageSize} and optionally
     * appending the {@code queryPredicate} if it is not null.
     *
     * @param queryPredicate query predicate to append if not null.
     * @return a future containing the results of the requested page of applying the query with a limit of this
     *         instance's {@code pageSize} and optionally appending the {@code queryPredicate} if it is not null.
     */
    @Nonnull
    private CompletionStage<PagedQueryResult<T>> queryPage(@Nullable final QueryPredicate<T> queryPredicate) {
        final QueryDsl<T, C> query = baseQuery.withLimit(pageSize);
        return client.execute(queryPredicate != null ? query.withPredicates(queryPredicate) : query);
    }

    /**
     * Gets the results of {@link this} instance's query with a limit of this instance's {@code pageSize}.
     *
     * @return a future containing the results of the requested page of applying the query with {@code pageSize}.
     */
    @Nonnull
    private CompletionStage<PagedQueryResult<T>> queryPage() {
        return queryPage(null);
    }
}
