package com.commercetools.sync.commons.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.categories.queries.CategoryQueryModel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.queries.QuerySort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CtpQueryUtilsTest {
    @Captor
    private ArgumentCaptor<CategoryQuery> sphereRequestArgumentCaptor;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void queryAll_WithCategoryKeyQuery_ShouldFetchCorrectCategories() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final List<Category> categories = getMockCategoryPage();
        final PagedQueryResult mockQueryResults = getMockQueryResults(categories);
        when(sphereClient.execute(any())).thenReturn(completedFuture(mockQueryResults));

        final List<String> keysToQuery = IntStream
            .range(1, 510)
            .mapToObj(i -> "key" + i)
            .collect(Collectors.toList());

        final Function<CategoryQueryModel, QueryPredicate<Category>> keyPredicateFunction =
            categoryQueryModel -> categoryQueryModel.key().isIn(keysToQuery);

        // test
        queryAll(sphereClient, CategoryQuery.of().plusPredicates(keyPredicateFunction), identity())
            .toCompletableFuture()
            .join();

        // assertions
        verify(sphereClient, times(2)).execute(sphereRequestArgumentCaptor.capture());
        assertThat(sphereRequestArgumentCaptor.getAllValues())
            .containsExactly(
                getFirstPageQuery(keyPredicateFunction),
                getSecondPageQuery(keyPredicateFunction, categories));
    }

    @Nonnull
    private List<Category> getMockCategoryPage() {
        return IntStream
            .range(0, 500)
            .mapToObj(i -> {
                final Category category = mock(Category.class);
                when(category.getId()).thenReturn(UUID.randomUUID().toString());
                return category;
            })
            .collect(Collectors.toList());
    }

    @Nonnull
    private PagedQueryResult getMockQueryResults(@Nonnull final List<Category> mockPage) {
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults())
            .thenReturn(mockPage) // get full page on first call
            .thenReturn(mockPage.subList(0, 10)); // get only 10 categories on second call
        return pagedQueryResult;
    }

    @Nonnull
    private CategoryQuery getFirstPageQuery(
        @Nonnull final Function<CategoryQueryModel, QueryPredicate<Category>> keyPredicate) {

        return CategoryQuery
            .of()
            .plusPredicates(keyPredicate).withSort(QuerySort.of("id asc"))
            .withLimit(500);
    }

    @Nonnull
    private CategoryQuery getSecondPageQuery(
        @Nonnull final Function<CategoryQueryModel, QueryPredicate<Category>> keyPredicateFunction,
        @Nonnull final List<Category> categoryPage) {

        final String lastCategoryIdInPage = categoryPage
            .get(categoryPage.size() - 1)
            .getId();

        return CategoryQuery
            .of()
            .plusPredicates(keyPredicateFunction).withSort(QuerySort.of("id asc"))
            .plusPredicates(QueryPredicate.of(format("id > \"%s\"", lastCategoryIdInPage)))
            .withLimit(500);
    }
}