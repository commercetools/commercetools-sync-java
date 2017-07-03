package com.commercetools.sync.integration.ctpprojectsource.categories.updateactionutils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeParent;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeParentUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangeParentIT {
    private Category sourceProjectRootCategory;
    private static Category targetProjectRootCategory;
    private static Category oldCategory;
    private List<String> callBackResponses = new ArrayList<>();
    private CategorySyncOptions categorySyncOptions;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();

        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .parent(targetProjectRootCategory)
            .build();

        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();
    }

    /**
     * Cleans the source CTP project, callback response collector and creates a new root category in the source.
     */
    @Before
    public void setupTest() {
        deleteRootCategory(CTP_SOURCE_CLIENT);
        sourceProjectRootCategory = createRootCategory(CTP_SOURCE_CLIENT);
        callBackResponses = new ArrayList<>();
        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                        .setWarningCallBack(callBackResponses::add)
                                                        .build();
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
    public void buildChangeParentUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different parent. The parent is substituted with the target project's
        // parnet id, because the utils assume reference resolution has already been done at this point.
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory)
                                                                    .build();

        // Build change parent update action
        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(oldCategory, draftFromCategory, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNotNull();
        assertThat(changeParentUpdateAction.getAction()).isEqualTo("changeParent");
        assertThat(((ChangeParent) changeParentUpdateAction).getParent().getId())
            .isEqualTo(newCategory.getParent().getId());
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildChangeParentUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a same parent. The parent is substituted with the target project's
        // parnet id, because the utils assume reference resolution has already been done at this point.
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory)
                                                                    .parent(targetProjectRootCategory)
                                                                    .build();

        // Build change parent update action
        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(oldCategory, draftFromCategory, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildChangeParentUpdateAction_WithNullValue_ShouldTriggerCallback() {
        // Prepare new category draft from source project's root category which has no parent.
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(sourceProjectRootCategory)
                                                                    .build();

        // Build change parent update action
        final UpdateAction<Category> changeParentUpdateAction =
            buildChangeParentUpdateAction(oldCategory, draftFromCategory, categorySyncOptions).orElse(null);

        assertThat(changeParentUpdateAction).isNull();
        assertThat(callBackResponses).hasSize(1);
        assertThat(callBackResponses.get(0)).isEqualTo(format("Cannot unset 'parent' field of category with id '%s'.",
            oldCategory.getId()));
    }
}
