package com.commercetools.sync.integration.externalsource.categories.updateactionutils;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeParentUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

class ChangeParentIT {
    private static Category oldCategory;
    private List<String> callBackResponses = new ArrayList<>();
    private CategorySyncOptions categorySyncOptions;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @BeforeAll
    static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);

        final CategoryDraft oldCategoryDraftParent = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("oldCategoryParent")
            .build();
        Category oldCategoryParent = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraftParent))
                                                      .toCompletableFuture()
                                                      .join();

        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .key("oldCategory")
            .parent(oldCategoryParent.toResourceIdentifier())
            .build();
        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();
    }

    /**
     * Cleans the callback response collector.
     */
    @BeforeEach
    void setupTest() {
        callBackResponses = new ArrayList<>();
        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                        .warningCallback(callBackResponses::add)
                                                        .build();
    }

    /**
     * Cleans up the target test data that was built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Create a mock category in the target project under this parent.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .parent(oldCategory.toResourceIdentifier())
            .build();

        // Build change parent update action between parent and new category.
        // The new category has parentCategory as its parent, while parentCategory has the targetProjectRootCategory
        // category as its parent.
        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(oldCategory, oldCategoryDraft, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) changeParentUpdateAction).getParent().getId())
            .isEqualTo(oldCategory.getId());
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with the same parent: targetProjectRootCategory
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .parent(ResourceIdentifier.ofId(oldCategory.getParent().getId()))
            .build();

        // Build change parent update action
        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(oldCategory, newCategory, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    void buildChangeParentUpdateAction_WithNullValue_ShouldTriggerCallback() {
        // Prepare new category draft with no parent
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture")).build();

        // Build change parent update action
        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(oldCategory, newCategory, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNull();
        assertThat(callBackResponses).hasSize(1);
        assertThat(callBackResponses.get(0)).isEqualTo(format("Cannot unset 'parent' field of category with id '%s'.",
            oldCategory.getId()));
    }

}
