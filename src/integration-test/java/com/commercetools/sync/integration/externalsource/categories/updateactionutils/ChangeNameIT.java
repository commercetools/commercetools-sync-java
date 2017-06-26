package com.commercetools.sync.integration.externalsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangeNameIT {
    private static Category oldCategory;

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);

        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.GERMAN, "Möbel", Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
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
    public void buildChangeNameUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different name
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .build();

        // Build change name update action
        final UpdateAction<Category> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldCategory, newCategory).orElse(null);


        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName())
            .isEqualTo(newCategory.getName());
    }

    @Test
    public void buildChangeNameUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a different order of locales of name
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture", Locale.GERMAN, "Möbel"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .build();

        // Build change name update action
        final UpdateAction<Category> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(changeNameUpdateAction).isNull();
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with same name as root
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .build();

        // Build change name update action
        final UpdateAction<Category> changeNameUpdateAction =
            buildChangeNameUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(changeNameUpdateAction).isNull();
    }
}
