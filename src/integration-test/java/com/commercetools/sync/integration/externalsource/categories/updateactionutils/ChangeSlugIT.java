package com.commercetools.sync.integration.externalsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangeSlugIT {
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);

        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder.of(
            LocalizedString.of(Locale.ENGLISH, "classic furniture"),
            LocalizedString.of(Locale.GERMAN, "klassische-moebel", Locale.ENGLISH, "classic-furniture"))
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
    public void buildChangeSlugUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different slug
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .build();

        // Build change slug update action
        final UpdateAction<Category> changeSlugUpdateAction =
            buildChangeSlugUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(changeSlugUpdateAction).isNotNull();
        assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
        assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug())
            .isEqualTo(newCategory.getSlug());
    }

    @Test
    public void buildChangeSlugUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a different order of locales of slug
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .build();

        // Build change slug update action
        final UpdateAction<Category> changeSlugUpdateAction =
            buildChangeSlugUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(changeSlugUpdateAction).isNull();
    }

    @Test
    public void buildChangeSlugUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a same slug
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .build();

        // Build change slug update action
        final UpdateAction<Category> changeSlugUpdateAction =
            buildChangeSlugUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(changeSlugUpdateAction).isNull();
    }
}
