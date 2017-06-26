package com.commercetools.sync.integration.externalsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class SetMetaKeywordsIT {
    private static Category targetProjectRootCategory;
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        // Create a mock old category in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(
                LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .metaDescription(
                LocalizedString.of(Locale.ENGLISH, "classic furniture metaKeywords",
                    Locale.GERMAN, "Klassische Möbel metaKeywords"))
            .parent(targetProjectRootCategory)
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
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }


    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different MetaKeywords
        final CategoryDraft newCategory = CategoryDraftBuilder.of(
            LocalizedString.of(Locale.ENGLISH, "classic furniture"),
            LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
                                                              .metaDescription(LocalizedString
                                                                  .of(Locale.ENGLISH, "classic furniture metaKeywords"))
                                                              .build();

        // Build set MetaKeywords update action
        final UpdateAction<Category> setMetaDescriptionUpdateAction =
            buildSetMetaDescriptionUpdateAction(targetProjectRootCategory, newCategory).orElse(null);


        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(newCategory.getMetaDescription());
    }

    @Test
    public void buildSetMetaKeywordsUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
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
    public void buildSetMetaKeywordsUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
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
