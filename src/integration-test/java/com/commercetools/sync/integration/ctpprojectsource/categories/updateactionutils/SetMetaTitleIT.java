package com.commercetools.sync.integration.ctpprojectsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class SetMetaTitleIT {
    private Category sourceProjectRootCategory;
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();

        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture", Locale.GERMAN, "klassische moebel"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .metaTitle(
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .parent(targetProjectRootCategory)
            .build();

        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();
    }

    /**
     * Cleans the source CTP project and creates a new root category.
     */
    @Before
    public void setupTest() {
        deleteRootCategory(CTP_SOURCE_CLIENT);
        sourceProjectRootCategory = createRootCategory(CTP_SOURCE_CLIENT);
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
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "modern classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .metaTitle(
                LocalizedString.of(Locale.ENGLISH, "new-furniture", Locale.GERMAN, "neue-moebel"))
            .parent(sourceProjectRootCategory)
            .build();

        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build set metatitle data update action
        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(oldCategory, draftFromCategory).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
            .isEqualTo(draftFromCategory.getMetaTitle());
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "modern classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .metaTitle(
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .parent(sourceProjectRootCategory)
            .build();

        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with the same metaTitle
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build set metatitle update action
        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(oldCategory, draftFromCategory).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNull();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "modern classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .metaTitle(
                LocalizedString.of(Locale.GERMAN, "klassische-moebel", Locale.ENGLISH, "classic-furniture"))
            .parent(sourceProjectRootCategory)
            .build();

        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different order of locales of name
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build set metatitle update action
        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(oldCategory, draftFromCategory).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNull();
    }
}
