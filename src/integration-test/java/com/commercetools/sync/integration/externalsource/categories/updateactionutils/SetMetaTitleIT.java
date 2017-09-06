package com.commercetools.sync.integration.externalsource.categories.updateactionutils;


import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class SetMetaTitleIT {
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);

        // Create a mock old category in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaTitle(
                LocalizedString.of(Locale.ENGLISH, "classic furniture meta info",
                    Locale.GERMAN, "Klassische Möbel Meta Info"))
            .build();

        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();
    }

    /**
     * Cleans up the target data that was built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }


    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different MetaTitle
        final CategoryDraft newCategory = CategoryDraftBuilder.of(
            LocalizedString.of(Locale.ENGLISH, "classic furniture"),
            LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
                                                              .metaTitle(LocalizedString
                                                                  .of(Locale.ENGLISH, "new metaTitle"))
                                                              .build();

        // Build set MetaTitle update action
        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(oldCategory, newCategory).orElse(null);


        assertThat(setMetaTitleUpdateAction).isNotNull();
        assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
        assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle())
            .isEqualTo(newCategory.getMetaTitle());
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a different order of locales of MetaTitle
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaTitle(
                LocalizedString.of(Locale.GERMAN, "Klassische Möbel Meta Info",
                    Locale.ENGLISH, "classic furniture meta info"))
            .build();

        // Build set MetaTitle update action
        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNull();
    }

    @Test
    public void buildSetMetaTitleUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a same order of locales of MetaTitle
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaTitle(
                LocalizedString.of(Locale.ENGLISH, "classic furniture meta info",
                    Locale.GERMAN, "Klassische Möbel Meta Info"))
            .build();

        // Build set MetaTitle update action
        final UpdateAction<Category> setMetaTitleUpdateAction =
            buildSetMetaTitleUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(setMetaTitleUpdateAction).isNull();
    }
}
