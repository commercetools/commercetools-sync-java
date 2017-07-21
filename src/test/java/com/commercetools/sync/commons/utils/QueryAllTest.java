package com.commercetools.sync.commons.utils;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryAllTest {
    private static final String CATEGORY_KEY = "catKey";
    private static final String CATEGORY_ID = UUID.randomUUID().toString();
    private static final SphereClient sphereClient = mock(SphereClient.class);

    @BeforeClass
    public static void setup() {
        final Category mockCategory = mock(Category.class);
        when(mockCategory.getKey()).thenReturn(CATEGORY_KEY);
        when(mockCategory.getId()).thenReturn(CATEGORY_ID);

        final PagedQueryResult<Category> pagedQueryResult =
            PagedQueryResult.of(Arrays.asList(mockCategory, mockCategory, mockCategory, mockCategory));
        when(sphereClient.execute(any())).thenReturn(CompletableFuture.completedFuture(pagedQueryResult));
    }

    @Test
    public void run_WithCallback_ShouldApplyCallback() {
        final QueryAll<Category, CategoryQuery> query = QueryAll.of(CategoryQuery.of(), 1);

        final Function<List<Category>, Optional<Category>> getFirstCategoryInPage =
            categoryPage -> categoryPage.isEmpty()? Optional.empty() : Optional.of(categoryPage.get(0));

        final List<Optional<Category>> firstCategories = query.run(sphereClient, getFirstCategoryInPage)
                                                   .toCompletableFuture()
                                                   .join();
        assertThat(firstCategories).hasSize(4);
        firstCategories.stream()
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .map(Category::getKey)
                       .forEach(key -> assertThat(key).isEqualTo(CATEGORY_KEY));
    }

    @Test
    public void run_WithConsumer_ShouldApplyConsumer() {
        final QueryAll<Category, CategoryQuery> query = QueryAll.of(CategoryQuery.of(), 1);
        final List<String> categoryIds = new ArrayList<>();

        final Consumer<List<Category>> categoryIdCollector = page ->
            page.forEach(category -> categoryIds.add(category.getId()));

        query.run(sphereClient, categoryIdCollector)
             .toCompletableFuture()
             .join();

        assertThat(categoryIds).hasSize(16);
        categoryIds.forEach(id -> assertThat(id).isEqualTo(CATEGORY_ID));
    }

    @Test
    public void getTotalNumberOfPages_WithUniformSplitting_ShouldReturnCorrectTotal() {
        final QueryAll<Category, CategoryQuery> query = QueryAll.of(CategoryQuery.of(), 2);
        final long totalNumberOfPages = query.getTotalNumberOfPages(10);
        assertThat(totalNumberOfPages).isEqualTo(5);
    }

    @Test
    public void getTotalNumberOfPages_WithNonUniformSplitting_ShouldReturnCorrectTotal() {
        final QueryAll<Category, CategoryQuery> query = QueryAll.of(CategoryQuery.of(), 2);
        final long totalNumberOfPages = query.getTotalNumberOfPages(7);
        assertThat(totalNumberOfPages).isEqualTo(4);
    }
}
