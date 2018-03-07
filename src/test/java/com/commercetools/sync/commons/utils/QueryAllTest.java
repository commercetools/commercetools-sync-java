package com.commercetools.sync.commons.utils;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.sphere.sdk.queries.QueryExecutionUtils.DEFAULT_PAGE_SIZE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryAllTest {
    private static final String CATEGORY_KEY = "catKey";
    private static final String CATEGORY_ID = UUID.randomUUID().toString();
    private static final SphereClient sphereClient = mock(SphereClient.class);

    /**
     * Prepares mock data for unit testing the CTP QueryAll utilities; specifically stubs a {@link SphereClient}
     * to always return a {@link PagedQueryResult} containing 4 identical mock categories on every call of
     * {@link SphereClient#execute(SphereRequest)}.
     */
    @BeforeClass
    public static void setup() {
        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn(CATEGORY_KEY);
        when(mockCategory.getId()).thenReturn(CATEGORY_ID);

        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(asList(mockCategory, mockCategory, mockCategory, mockCategory));

        when(sphereClient.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedQueryResult));
    }

    @Test
    public void run_WithCallback_ShouldApplyCallback() {
        final QueryAll<Category, CategoryQuery, Optional<Category>> query =
            QueryAll.of(sphereClient, CategoryQuery.of(), DEFAULT_PAGE_SIZE);

        final Function<List<Category>, Optional<Category>> getFirstCategoryInPage =
            categoryPage -> categoryPage.isEmpty() ? Optional.empty() : Optional.of(categoryPage.get(0));

        final List<Optional<Category>> firstCategories = query.run(getFirstCategoryInPage)
                                                              .toCompletableFuture()
                                                              .join();

        assertThat(firstCategories).hasSize(1);
        assertThat(firstCategories.get(0)).isPresent();
        assertThat(firstCategories.get(0).get().getKey()).isEqualTo(CATEGORY_KEY);
    }

    @Test
    public void run_WithConsumer_ShouldApplyConsumer() {
        final QueryAll<Category, CategoryQuery, Void> query =
            QueryAll.of(sphereClient, CategoryQuery.of(), DEFAULT_PAGE_SIZE);
        final List<String> categoryIds = new ArrayList<>();

        final Consumer<List<Category>> categoryIdCollector = page ->
            page.forEach(category -> categoryIds.add(category.getId()));

        query.run(categoryIdCollector).toCompletableFuture().join();

        assertThat(categoryIds).hasSize(4);
        categoryIds.forEach(id -> assertThat(id).isEqualTo(CATEGORY_ID));
    }
}
