package com.commercetools.sync.commons.utils;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.models.ResourceKeyId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

final class GraphQlQueryAll {
    private final SphereClient client;
    private final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest;
    private final long pageSize;

    private Consumer<Set<ResourceKeyId>> pageConsumer;

    private GraphQlQueryAll(@Nonnull final SphereClient client,
                            @Nonnull final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest,
                            final long pageSize) {

        this.client = client;
        this.resourceKeyIdGraphQlRequest = withDefaults(resourceKeyIdGraphQlRequest, pageSize);
        this.pageSize = pageSize;
    }

    @Nonnull
    private static ResourceKeyIdGraphQlRequest withDefaults(
        @Nonnull final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest,
        final long pageSize) {

        return resourceKeyIdGraphQlRequest.withLimit(pageSize);
    }

    @Nonnull
    static GraphQlQueryAll of(@Nonnull final SphereClient client,
                              @Nonnull final ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest,
                              final int pageSize) {

        return new GraphQlQueryAll(client, resourceKeyIdGraphQlRequest, pageSize);
    }

    /**
     * Given a {@link Consumer} to a page of results of type {@code BaseGraphQlResult}, this method sets this instance's
     * {@code pageConsumer} to the supplied value, then it makes requests to fetch the entire result space of a
     * graphql query request {@code BaseGraphQlRequest} to CTP, while accepting the consumer on each fetched page.
     *
     * @param pageConsumer the consumer to accept on each fetched page of the result space.
     * @return a future containing void after the consumer accepted all the pages.
     */
    @Nonnull
    CompletionStage<Void> run(@Nonnull final Consumer<Set<ResourceKeyId>> pageConsumer) {

        this.pageConsumer = pageConsumer;
        final CompletionStage<ResourceKeyIdGraphQlResult> firstPage = client.execute(resourceKeyIdGraphQlRequest);
        return queryNextPages(firstPage);
    }

    /**
     * Given a completion stage {@code currentPageStage} containing a current graphql result
     * {@link ResourceKeyIdGraphQlResult}, this method composes the completion stage by first checking if the result
     * is null or not. If it is not, then it recursively (by calling itself with the next page's completion stage
     * result) composes to the supplied stage, stages of the all next pages' processing. If there is no next page,
     * then the result of the {@code currentPageStage} would be null and this method would just return a completed
     * future containing null result, which in turn signals the last page of processing.
     *
     * @param currentPageStage a future containing a graphql result {@link ResourceKeyIdGraphQlResult}.
     */
    @Nonnull
    private CompletionStage<Void> queryNextPages(
        @Nonnull final CompletionStage<ResourceKeyIdGraphQlResult> currentPageStage) {
        return currentPageStage.thenCompose(currentPage ->
            currentPage != null ? queryNextPages(processPageAndGetNext(currentPage)) : completedFuture(null));
    }

    /**
     * Given a graphql query result representing a page {@link ResourceKeyIdGraphQlResult}, this method checks if there
     * are elements in the result (size > 0), then it consumes the resultant list using this instance's {@code
     * pageConsumer}. Then it attempts to fetch the next page if it exists and returns a completion stage
     * containing the result of the next page. If there is a next page, then a new future of the next page is returned.
     * If there are no more results, the method returns a completed future containing null.
     *
     * @param page the current page result.
     * @return If there is a next page, then a new future of the next page is returned. If there are no more results,
     *         the method returns a completed future containing null.
     */
    @Nonnull
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // `https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<ResourceKeyIdGraphQlResult> processPageAndGetNext(
        @Nonnull final ResourceKeyIdGraphQlResult page) {
        final Set<ResourceKeyId> currentPageElements = page.getResults();
        if (!currentPageElements.isEmpty()) {
            consumePageElements(currentPageElements);
            return getNextPageStage(currentPageElements);
        }
        return completedFuture(null);
    }

    private void consumePageElements(@Nonnull final Set<ResourceKeyId> pageElements) {
        pageConsumer.accept(pageElements);
    }

    /**
     * Given a list of page elements of type {@code ResourceKeyId}, this method checks if this page is the last page or
     * not by checking if the result size is equal to this instance's {@code pageSize}). If it is, then it means
     * there might be still more results. However, if not, then it means for sure there are no more results and this
     * is the last page. If there is a next page, the id of the last element in the list is used and a future is
     * created containing the fetched results which have an id greater than the id of the last element in the list
     * and this future is returned. If there are no more results, the method returns a completed future containing null.
     *
     * @param pageElements set of page elements of type {@code ResourceKeyId}.
     * @return a future containing the fetched results which have an id greater than the id of the last element
     *          in the set.
     */
    @Nonnull
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // `https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<ResourceKeyIdGraphQlResult> getNextPageStage(
        @Nonnull final Set<ResourceKeyId> pageElements) {
        if (pageElements.size() == pageSize) {
            String lastElementId = EMPTY;
            Iterator<ResourceKeyId> iterator = pageElements.iterator();
            while (iterator.hasNext()) {
                lastElementId = iterator.next().getId();
            }
            final String queryPredicate = isBlank(lastElementId) ? null : format("id > \\\\\\\"%s\\\\\\\"",
                lastElementId);

            return client.execute(resourceKeyIdGraphQlRequest.withPredicate(queryPredicate));
        }
        return completedFuture(null);
    }
}
