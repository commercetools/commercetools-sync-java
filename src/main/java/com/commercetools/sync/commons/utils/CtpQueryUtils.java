package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyId;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.queries.QueryDsl;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.sphere.sdk.queries.QueryExecutionUtils.DEFAULT_PAGE_SIZE;

public final class CtpQueryUtils {

    /**
     * Queries all elements matching a query by using a limit based pagination with a combination of id sorting and a
     * page size 500. More on the algorithm can be found here: http://dev.commercetools.com/http-api.html#offset.
     *
     * <p>The method takes a callback {@link Function} that returns a result of type {@code <S>} that is returned on
     * every page of elements queried. Eventually, the method returns a {@link CompletionStage} that contains a list of
     * all the results of the callbacks returned from every page.
     *
     * <p>NOTE: This method fetches all paged results sequentially as opposed to fetching the pages in parallel.
     *
     * @param client     commercetools client
     * @param query      query containing predicates and expansion paths
     * @param pageMapper callback function that is called on every page queried
     * @param <T>        type of one query result element
     * @param <C>        type of the query
     * @param <S>        type of the returned result of the callback function on every page.
     * @return a completion stage containing a list of mapped pages as a result.
     */
    @Nonnull
    public static <T extends ResourceView, C extends QueryDsl<T, C>, S> CompletionStage<List<S>>
        queryAll(@Nonnull final SphereClient client, @Nonnull final QueryDsl<T, C> query,
                 @Nonnull final Function<List<T>, S> pageMapper) {
        return queryAll(client, query, pageMapper, DEFAULT_PAGE_SIZE);
    }

    /**
     * Queries all elements matching a query by using a limit based pagination with a combination of id sorting and a
     * page size 500. More on the algorithm can be found here: http://dev.commercetools.com/http-api.html#offset
     *
     * <p>The method takes a consumer {@link Consumer} that is applied on every page of elements queried.
     *
     * <p>NOTE: This method fetches all paged results sequentially as opposed to fetching the pages in parallel.
     *
     * @param client       commercetools client
     * @param query        query containing predicates and expansion paths
     * @param pageConsumer consumer applied on every page queried
     * @param <T>          type of one query result element
     * @param <C>          type of the query
     * @return a completion stage containing void as a result after the consumer was applied on all pages.
     */
    @Nonnull
    public static <T extends ResourceView, C extends QueryDsl<T, C>> CompletionStage<Void>
        queryAll(@Nonnull final SphereClient client, @Nonnull final QueryDsl<T, C> query,
                 @Nonnull final Consumer<List<T>> pageConsumer) {
        return queryAll(client, query, pageConsumer, DEFAULT_PAGE_SIZE);
    }

    /**
     * Queries all elements matching a query by using a limit based pagination with a combination of id sorting and the
     * supplied {@code pageSize}.
     * More on the algorithm can be found here: http://dev.commercetools.com/http-api.html#offset.
     *
     * <p>The method takes a callback {@link Function} that returns a result of type {@code <S>} that is returned on
     * every page of elements queried. Eventually, the method returns a {@link CompletionStage} that contains a list of
     * all the results of the callbacks returned from every page.
     *
     * <p>NOTE: This method fetches all paged results sequentially as opposed to fetching the pages in parallel.
     *
     * @param client     commercetools client
     * @param query      query containing predicates and expansion paths
     * @param pageMapper callback function that is called on every page queried
     * @param <T>        type of one query result element
     * @param <C>        type of the query
     * @param <S>        type of the returned result of the callback function on every page.
     * @param pageSize   the page size.
     * @return a completion stage containing a list of mapped pages as a result.
     */
    @Nonnull
    public static <T extends ResourceView, C extends QueryDsl<T, C>, S> CompletionStage<List<S>>
        queryAll(@Nonnull final SphereClient client, @Nonnull final QueryDsl<T, C> query,
                 @Nonnull final Function<List<T>, S> pageMapper, final int pageSize) {
        final QueryAll<T, C, S> queryAll = QueryAll.of(client, query, pageSize);
        return queryAll.run(pageMapper);
    }

    /**
     * Queries all elements matching a query by using a limit based pagination with a combination of id sorting and the
     * supplied {@code pageSize}.
     * More on the algorithm can be found here: http://dev.commercetools.com/http-api.html#offset
     *
     * <p>The method takes a {@link Consumer} that is applied on every page of the queried elements.
     *
     * <p>NOTE: This method fetches all paged results sequentially as opposed to fetching the pages in parallel.
     *
     * @param client       commercetools client
     * @param query        query containing predicates and expansion paths
     * @param pageConsumer consumer applied on every page queried
     * @param <T>          type of one query result element
     * @param <C>          type of the query
     * @param pageSize     the page size
     * @return a completion stage containing void as a result after the consumer was applied on all pages.
     */
    @Nonnull
    public static <T extends ResourceView, C extends QueryDsl<T, C>> CompletionStage<Void>
        queryAll(@Nonnull final SphereClient client, @Nonnull final QueryDsl<T, C> query,
                 @Nonnull final Consumer<List<T>> pageConsumer, final int pageSize) {
        final QueryAll<T, C, Void> queryAll = QueryAll.of(client, query, pageSize);
        return queryAll.run(pageConsumer);
    }

    /**
     * Creates a graphQL query to fetch all elements where keys matching given set of keys with pagination using a
     * combination of limit and id sorting.
     *
     * <p>The method takes a {@link Consumer} that is applied on every page of the queried elements.
     *
     * <p>NOTE: This method fetches all paged results sequentially as opposed to fetching the pages in parallel.
     *
     * @param client            commercetools client
     * @param resourceKeyIdGraphQlRequest    graphql query containing predicates and pagination limits
     * @param pageConsumer      consumer applied on every page queried
     * @return a completion stage containing void as a result after the consumer was applied on all pages.
     */
    @Nonnull
    public static CompletionStage<Void> queryAll(@Nonnull final SphereClient client,
                                                 @Nonnull final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest,
                                                 @Nonnull final Consumer<Set<ResourceKeyId>> pageConsumer) {
        GraphQlQueryAll graphQlQueryAll = GraphQlQueryAll.of(client, resourceKeyIdGraphQlRequest, DEFAULT_PAGE_SIZE);
        return graphQlQueryAll.run(pageConsumer);
    }

    private CtpQueryUtils() {
    }
}
