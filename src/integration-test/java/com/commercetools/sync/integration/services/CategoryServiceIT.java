package com.commercetools.sync.integration.services;


import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
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

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class CategoryServiceIT {
    private CategoryService categoryService;
    private Category targetProjectRootCategory;
    private Category oldCategory;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @Before
    public void setup() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();

        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        // Create a mock new category in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("oldCategoryKey")
            .parent(targetProjectRootCategory)
            .custom(getMockCustomFieldsDraft())
            .build();
        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();

        categoryService = new CategoryServiceImpl(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
    }

    @Test
    public void fetchCachedCategoryId_WithNonExistingCategory_ShouldNotFetchACategory() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId("non-existing-category-key")
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithExistingCategory_ShouldFetchCategoryAndCache() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId(oldCategory.getKey())
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isNotEmpty();
    }


    @Test
    public void fetchCachedCategoryId_WithNonInvalidatedCache_ShouldFetchCategoryAndCache() {
        // Fetch any category to populate cache
        categoryService.fetchCachedCategoryId("anyKey").toCompletableFuture().join();

        // Create new category
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .parent(targetProjectRootCategory)
            .build();

        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        final Optional<String> newCategoryId =
            categoryService.fetchCachedCategoryId(newCategoryKey).toCompletableFuture().join();

        assertThat(newCategoryId).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithInvalidatedCache_ShouldFetchCategoryAndCache() {
        // Fetch any category to populate cache
        categoryService.fetchCachedCategoryId("anyKey").toCompletableFuture().join();

        // Create new category
        final String newCategoryKey = "newCategoryKey";

        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .parent(targetProjectRootCategory)
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        categoryService.invalidateCache();

        final Optional<String> newCategoryId =
            categoryService.fetchCachedCategoryId(newCategoryKey).toCompletableFuture().join();

        assertThat(newCategoryId).isNotEmpty();
    }

    @Test
    public void fetchCategoryByKey_WithExistingCategoryKey_ShouldFetchCategory() {
        final Optional<Category> categoryOptional = categoryService
            .fetchCategoryByKey(oldCategory.getKey()).toCompletableFuture().join();

        assertThat(categoryOptional).isNotEmpty();
        final Category category = categoryOptional.get();
        assertThat(category.getName()).isEqualTo(oldCategory.getName());
        assertThat(category.getSlug()).isEqualTo(oldCategory.getSlug());
        assertThat(category.getParent()).isEqualTo(oldCategory.getParent());
        assertThat(category.getCustom()).isEqualTo(oldCategory.getCustom());
        final LocalizedString localizedStringCustomField = category
            .getCustom().getFieldAsLocalizedString(LOCALISED_STRING_CUSTOM_FIELD_NAME);
        assertThat(localizedStringCustomField).isNotNull();
        assertThat(localizedStringCustomField.get(Locale.GERMAN)).isEqualTo("rot");
        final Boolean booleanCustomField = category.getCustom().getFieldAsBoolean(BOOLEAN_CUSTOM_FIELD_NAME);
        assertThat(booleanCustomField).isNotNull();
        assertThat(booleanCustomField.booleanValue()).isFalse();
    }

    @Test
    public void fetchCategoryByKey_WithNonExistingCategoryKey_ShouldNotFetchACategory() {
        final Optional<Category> categoryOptional = categoryService
            .fetchCategoryByKey("nonExistingKey").toCompletableFuture().join();

        assertThat(categoryOptional).isEmpty();
    }

    @Test
    public void createCategory_ShouldCreateCorrectCategory() {
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getMockCustomFieldsDraft())
            .parent(targetProjectRootCategory)
            .build();
        final Category category = categoryService.createCategory(categoryDraft).toCompletableFuture().join();

        //assert CTP state
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of()
                                  .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(newCategoryKey)))
            .toCompletableFuture().join().head();

        assertThat(categoryOptional).isNotEmpty();
        final Category fetchedCategory = categoryOptional.get();
        assertThat(fetchedCategory.getName()).isEqualTo(category.getName());
        assertThat(fetchedCategory.getSlug()).isEqualTo(category.getSlug());
        assertThat(fetchedCategory.getParent()).isEqualTo(Category.reference(targetProjectRootCategory.getId()));
        assertThat(fetchedCategory.getCustom()).isNotNull();
        assertThat(fetchedCategory.getKey()).isEqualTo(newCategoryKey);
    }

    @Test
    public void updateCategory_ShouldUpdateCategoryCorrectly() {
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(oldCategory.getKey())))
            .toCompletableFuture().join().head();

        final String newCategoryName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newCategoryName));

        final Category updatedCategory = categoryService
            .updateCategory(categoryOptional.get(), Collections.singletonList(changeNameUpdateAction))
            .toCompletableFuture().join();

        //assert CTP state
        final Optional<Category> fetchedCategoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(oldCategory.getKey())))
            .toCompletableFuture().join().head();

        assertThat(fetchedCategoryOptional).isNotEmpty();
        final Category fetchedCategory = fetchedCategoryOptional.get();
        assertThat(fetchedCategory.getName().get(Locale.GERMAN)).isEqualTo(newCategoryName);
        assertThat(updatedCategory.getSlug()).isEqualTo(oldCategory.getSlug());
        assertThat(fetchedCategory.getParent()).isEqualTo(Category.reference(targetProjectRootCategory.getId()));
        assertThat(fetchedCategory.getCustom()).isNotNull();
        assertThat(fetchedCategory.getKey()).isEqualTo(oldCategory.getKey());
    }
}
