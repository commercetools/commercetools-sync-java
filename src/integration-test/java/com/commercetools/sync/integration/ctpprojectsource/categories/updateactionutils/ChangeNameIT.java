package com.commercetools.sync.integration.ctpprojectsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

class ChangeNameIT {
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @BeforeAll
    static void setup() {
        deleteAllCategories(CTP_SOURCE_CLIENT);
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypesFromTargetAndSource();

        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture", Locale.GERMAN, "klassische moebel"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .build();

        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();
    }

    /**
     * Cleans the source CTP project.
     */
    @BeforeEach
    void setupTest() {
        deleteAllCategories(CTP_SOURCE_CLIENT);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteAllCategories(CTP_SOURCE_CLIENT);
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypesFromTargetAndSource();
    }

    @Test
    void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "modern classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .build();

        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different name
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build change name update action
        final UpdateAction<Category> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldCategory, draftFromCategory).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName())
            .isEqualTo(newCategory.getName());
    }

    @Test
    void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .build();

        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different name
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build change name update action
        final UpdateAction<Category> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldCategory, draftFromCategory).orElse(null);

        assertThat(changeNameUpdateAction).isNull();
    }

    @Test
    void buildChangeNameUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        // Create a mock new category in the source project.
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.GERMAN, "klassische moebel",
                Locale.ENGLISH, "classic furniture"), oldCategory.getSlug())
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different order of locales of name
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build change name update action
        final UpdateAction<Category> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldCategory, draftFromCategory).orElse(null);

        assertThat(changeNameUpdateAction).isNull();
    }
}
