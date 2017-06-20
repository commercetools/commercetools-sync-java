package com.commercetools.sync.services;


import com.commercetools.sync.integration.categories.utils.CategoryITUtils;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.getMockCategoryDrafts;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.deleteCategoriesFromTargetAndSource;
import static org.assertj.core.api.Assertions.assertThat;

public class CategoryServiceIT {
    private CategoryService categoryService;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @Before
    public void setup() {
        deleteCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        createCategories(CTP_TARGET_CLIENT, getMockCategoryDrafts());
        categoryService = new CategoryServiceImpl(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
    }

    @Test
    public void fetchCachedCategoryId_WithNonExistingCategory_ShouldNotFetchACategory() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId("non-existing-category-external-id")
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithExistingCategory_ShouldFetchCategoryAndCache() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId("1")
                                                     .toCompletableFuture()
                                                     .join();
        assertThat(categoryId).isNotEmpty();
    }


    @Test
    public void fetchCachedCategoryId_WithNonInvalidatedCache_ShouldFetchCategoryAndCache() {
        // Fetch any category to populate cache
        categoryService.fetchCachedCategoryId("anyExternalId").toCompletableFuture().join();

        // Create new category
        final String newCategoryExternalId = "newCategoryExternalId";
        final CategoryDraft categoryDraft = CategoryITUtils
            .getMockCategoryDraft(Locale.GERMAN, "newCategoryName", "newCategorySlug", newCategoryExternalId);
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        final Optional<String> newCategoryId =
            categoryService.fetchCachedCategoryId(newCategoryExternalId).toCompletableFuture().join();

        assertThat(newCategoryId).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithInvalidatedCache_ShouldFetchCategoryAndCache() {
        // Fetch any category to populate cache
        categoryService.fetchCachedCategoryId("anyExternalId").toCompletableFuture().join();

        // Create new category
        final String newCategoryExternalId = "newCategoryExternalId";
        final CategoryDraft categoryDraft = CategoryITUtils
            .getMockCategoryDraft(Locale.GERMAN, "newCategoryName", "newCategorySlug", newCategoryExternalId);
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        categoryService.invalidateCache();

        final Optional<String> newCategoryId =
            categoryService.fetchCachedCategoryId(newCategoryExternalId).toCompletableFuture().join();

        assertThat(newCategoryId).isNotEmpty();
    }

    @Test
    public void fetchCategoryByExternalId_WithExistingCategoryExternalId_ShouldFetchCategory() {
        final Optional<Category> categoryOptional = categoryService
            .fetchCategoryByExternalId("1").toCompletableFuture().join();

        assertThat(categoryOptional).isNotEmpty();
        final Category category = categoryOptional.get();
        assertThat(category.getName().get(Locale.GERMAN)).isEqualTo("draft");
        assertThat(category.getSlug()).isNotNull();
        assertThat(category.getSlug().get(Locale.GERMAN)).isEqualTo("slug");
        assertThat(category.getParent()).isNull();
        assertThat(category.getCustom()).isNotNull();
        final LocalizedString localizedStringCustomField = category
            .getCustom().getFieldAsLocalizedString(LOCALISED_STRING_CUSTOM_FIELD_NAME);
        assertThat(localizedStringCustomField).isNotNull();
        assertThat(localizedStringCustomField.get(Locale.GERMAN)).isEqualTo("rot");
        final Boolean booleanCustomField = category.getCustom().getFieldAsBoolean(BOOLEAN_CUSTOM_FIELD_NAME);
        assertThat(booleanCustomField).isNotNull();
        assertThat(booleanCustomField.booleanValue()).isFalse();
    }

    @Test
    public void fetchCategoryByExternalId_WithNonExistingCategoryExternalId_ShouldNotFetchACategory() {
        final Optional<Category> categoryOptional = categoryService
            .fetchCategoryByExternalId("nonExistingExternalId").toCompletableFuture().join();

        assertThat(categoryOptional).isEmpty();
    }

    @Test
    public void createCategory_ShouldCreateCorrectCategory() {
        final String newCategoryExternalId = "newCategoryExternalId";
        final CategoryDraft categoryDraft = CategoryITUtils
            .getMockCategoryDraft(Locale.GERMAN, "newCategoryName", "newCategorySlug", newCategoryExternalId);
        final Category category = categoryService.createCategory(categoryDraft).toCompletableFuture().join();

        assertThat(category).isNotNull();
        assertThat(category.getName().get(Locale.GERMAN)).isEqualTo("newCategoryName");
        assertThat(category.getSlug()).isNotNull();
        assertThat(category.getSlug().get(Locale.GERMAN)).isEqualTo("newCategorySlug");
        assertThat(category.getParent()).isNull();
        assertThat(category.getCustom()).isNotNull();
        assertThat(category.getExternalId()).isEqualTo(newCategoryExternalId);

        //assert CTP state
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of().byExternalId(newCategoryExternalId))
            .toCompletableFuture().join().head();

        assertThat(categoryOptional).isNotEmpty();
        final Category fetchedCategory = categoryOptional.get();
        assertThat(fetchedCategory.getName().get(Locale.GERMAN)).isEqualTo("newCategoryName");
        assertThat(fetchedCategory.getSlug()).isNotNull();
        assertThat(fetchedCategory.getSlug().get(Locale.GERMAN)).isEqualTo("newCategorySlug");
        assertThat(fetchedCategory.getParent()).isNull();
        assertThat(fetchedCategory.getCustom()).isNotNull();
        assertThat(fetchedCategory.getExternalId()).isEqualTo(newCategoryExternalId);
    }

    @Test
    public void updateCategory_ShouldUpdateCategoryCorrectly() {
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of().byExternalId("1"))
            .toCompletableFuture().join().head();

        final String newCategoryName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newCategoryName));

        final Category updatedCategory = categoryService
            .updateCategory(categoryOptional.get(), Collections.singletonList(changeNameUpdateAction))
            .toCompletableFuture().join();

        assertThat(updatedCategory).isNotNull();
        assertThat(updatedCategory.getName().get(Locale.GERMAN)).isEqualTo(newCategoryName);
        assertThat(updatedCategory.getSlug()).isNotNull();
        assertThat(updatedCategory.getSlug().get(Locale.GERMAN)).isEqualTo("slug");
        assertThat(updatedCategory.getParent()).isNull();
        assertThat(updatedCategory.getCustom()).isNotNull();
        assertThat(updatedCategory.getExternalId()).isEqualTo("1");

        //assert CTP state
        final Optional<Category> fetchedCategoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of().byExternalId("1"))
            .toCompletableFuture().join().head();

        assertThat(fetchedCategoryOptional).isNotEmpty();
        final Category fetchedCategory = fetchedCategoryOptional.get();
        assertThat(fetchedCategory.getName().get(Locale.GERMAN)).isEqualTo(newCategoryName);
        assertThat(fetchedCategory.getSlug()).isNotNull();
        assertThat(fetchedCategory.getSlug().get(Locale.GERMAN)).isEqualTo("slug");
        assertThat(fetchedCategory.getParent()).isNull();
        assertThat(fetchedCategory.getCustom()).isNotNull();
        assertThat(fetchedCategory.getExternalId()).isEqualTo("1");
    }
}
