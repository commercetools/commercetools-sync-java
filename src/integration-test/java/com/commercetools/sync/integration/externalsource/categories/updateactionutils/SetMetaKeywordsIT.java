package com.commercetools.sync.integration.externalsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

class SetMetaKeywordsIT {
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeAll
    static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        // Create a mock old category in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaDescription(
                LocalizedString.of(Locale.ENGLISH, "classic furniture metaKeywords",
                    Locale.GERMAN, "Klassische Möbel metaKeywords"))
            .build();

        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();
    }

    /**
     * Cleans up the target data that was built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }


    @Test
    void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different MetaKeywords
        final CategoryDraft newCategory = CategoryDraftBuilder.of(
            LocalizedString.of(Locale.ENGLISH, "classic furniture"),
            LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
                                                              .metaDescription(LocalizedString
                                                                  .of(Locale.ENGLISH, "classic furniture metaKeywords"))
                                                              .build();

        // Build set MetaKeywords update action
        final UpdateAction<Category> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(oldCategory, newCategory).orElse(null);


        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(newCategory.getMetaDescription());
    }

    @Test
    void buildSetMetaKeywordsUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a different order of locales of MetaKeywords
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaDescription(
                LocalizedString.of(Locale.GERMAN, "Klassische Möbel metaKeywords",
                    Locale.ENGLISH, "classic furniture metaKeywords"))
            .build();

        // Build set MetaKeywords update action
        final UpdateAction<Category> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNull();
    }

    @Test
    void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a same order of locales of MetaKeywords
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaDescription(
                LocalizedString.of(Locale.ENGLISH, "classic furniture metaKeywords",
                    Locale.GERMAN, "Klassische Möbel metaKeywords"))
            .build();

        // Build set MetaKeywords update action
        final UpdateAction<Category> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNull();
    }
}
