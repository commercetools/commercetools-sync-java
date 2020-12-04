package com.commercetools.sync.integration.commons;


import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.models.ResourceKeyId;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.queries.CategoryQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.queryAll;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDraftsWithPrefix;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

class CtpQueryUtilsIT {

    /**
     * Delete all categories and types from target project. Then create custom types for target CTP project categories.
     */
    @BeforeAll
    static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Categories and Types from target CTP projects, then it populates target CTP project with category test
     * data.
     */
    @BeforeEach
    void setupTest() {
        deleteAllCategories(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void queryAll_WithKeyCollectorConsumerOn100CategoriesWithCustomPageSize_ShouldCollectKeys() {
        final int numberOfCategories = 100;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));
        final List<String> categoryKeys = new ArrayList<>();

        final Consumer<List<Category>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryKeys.add(category.getKey()));

        queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), categoryPageConsumer, 10).toCompletableFuture().join();
        assertThat(categoryKeys).hasSize(numberOfCategories);
    }

    @Test
    void queryAll_WithKeyCollectorConsumerOn600Categories_ShouldCollectKeys() {
        final int numberOfCategories = 600;
        final List<CategoryDraft> categoryDrafts = getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories);
        createCategories(CTP_TARGET_CLIENT, categoryDrafts);

        final List<String> categoryKeys = new ArrayList<>();

        final Consumer<List<Category>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryKeys.add(category.getKey()));

        queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), categoryPageConsumer).toCompletableFuture().join();
        assertThat(categoryKeys).hasSize(numberOfCategories);
    }

    @Test
    void queryAll_WithCategoryCollectorCallbackWithUniformPageSplitting_ShouldFetchAllCategories() {
        final int numberOfCategories = 10;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));

        final List<List<Category>> categoryPages =
            queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), (categories -> categories), 2)
                .toCompletableFuture().join();

        assertThat(categoryPages).hasSize(5);

        categoryPages.forEach(page -> assertThat(page.size()).isEqualTo(2));

        // Assert that all categories created are fetched.
        final Set<String> pageCategoryKeys = new HashSet<>();
        categoryPages.forEach(page -> {
            assertThat(page.size()).isEqualTo(2);
            page.forEach(category -> pageCategoryKeys.add(category.getKey()));
        });
        assertThat(pageCategoryKeys).hasSize(numberOfCategories);
    }

    @Test
    void queryAll_WithCategoryCollectorCallbackWithNonUniformPageSplitting_ShouldFetchAllCategories() {
        final int numberOfCategories = 7;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));

        final List<List<Category>> categoryPages =
            queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), (categories -> categories), 2)
                .toCompletableFuture().join();

        assertThat(categoryPages).hasSize(4);

        // Assert that all categories created are fetched.
        final Set<String> pageCategoryKeys = new HashSet<>();
        categoryPages.forEach(page -> page.forEach(category -> pageCategoryKeys.add(category.getKey())));
        assertThat(pageCategoryKeys).hasSize(numberOfCategories);
    }

    @Test
    void queryAll_WithCategoryCollectorCallbackOn600Categories_ShouldFetchAllCategories() {
        final int numberOfCategories = 600;
        createCategories(CTP_TARGET_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories));

        final List<List<Category>> categoryPages =
            queryAll(CTP_TARGET_CLIENT, CategoryQuery.of(), (categories -> categories)).toCompletableFuture().join();
        assertThat(categoryPages).hasSize(2);
        assertThat(categoryPages.get(0)).hasSize(500);
        assertThat(categoryPages.get(1)).hasSize(100);
    }


    @Test
    void queryAll_WithIdCollectorConsumerWithSinglePage_ShouldCollectIds() {
        final int numberOfCategories = 100;
        List<CategoryDraft> categoryDrafts =
            getCategoryDraftsWithPrefix(Locale.ENGLISH, "new", null, numberOfCategories);
        List<Category> categories = createCategories(CTP_TARGET_CLIENT, categoryDrafts);

        Set<String> allCategoryKeys = categories.stream()
                                                .map(category -> category.getKey())
                                                .collect(Collectors.toSet());
        final List<String> categoryIds = new ArrayList<>();

        final Consumer<Set<ResourceKeyId>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryIds.add(category.getId()));

        ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest = new ResourceKeyIdGraphQlRequest(allCategoryKeys,
            GraphQlQueryResources.CATEGORIES);

        queryAll(CTP_TARGET_CLIENT, resourceKeyIdGraphQlRequest, categoryPageConsumer).toCompletableFuture().join();
        assertThat(categoryIds).hasSize(numberOfCategories);
    }

    @Test
    void queryAll_WithIdCollectorConsumerWithMultiplePages_ShouldCollectIds() {
        final int numberOfCategories = 600;
        final List<CategoryDraft> categoryDrafts = getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, numberOfCategories);
        List<Category> categories = createCategories(CTP_TARGET_CLIENT, categoryDrafts);
        Set<String> allCategoryKeys = categories.stream()
                                                .map(category -> category.getKey())
                                                .collect(Collectors.toSet());

        final List<String> categoryIds = new ArrayList<>();

        final Consumer<Set<ResourceKeyId>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryIds.add(category.getId()));

        ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
            new ResourceKeyIdGraphQlRequest(allCategoryKeys, GraphQlQueryResources.CATEGORIES);

        queryAll(CTP_TARGET_CLIENT, resourceKeyIdGraphQlRequest, categoryPageConsumer).toCompletableFuture().join();
        assertThat(categoryIds).hasSize(numberOfCategories);
    }

    @Test
    void queryAll_WithIdCollectorConsumerWithRequestedKeySubset_ShouldCollectIds() {
        final List<CategoryDraft> categoryDrafts = getCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            null, 5);
        List<Category> categories = createCategories(CTP_TARGET_CLIENT, categoryDrafts);

        final List<String> categoryIds = new ArrayList<>();

        final Consumer<Set<ResourceKeyId>> categoryPageConsumer = categoryPageResults ->
            categoryPageResults.forEach(category -> categoryIds.add(category.getId()));

        ResourceKeyIdGraphQlRequest resourceKeyIdGraphQlRequest =
            new ResourceKeyIdGraphQlRequest(singleton(categories.get(0).getKey()),
                GraphQlQueryResources.CATEGORIES);

        queryAll(CTP_TARGET_CLIENT, resourceKeyIdGraphQlRequest, categoryPageConsumer).toCompletableFuture().join();
        assertThat(categoryIds).hasSize(1);
        assertThat(categoryIds).containsExactly(categories.get(0).getId());
    }

}
