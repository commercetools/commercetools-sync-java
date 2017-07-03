package com.commercetools.sync.integration.ctpprojectsource.categories.updateactionutils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetDescription;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class SetDescriptionIT {
    private Category sourceProjectRootCategory;
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

        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .description(LocalizedString.of(Locale.ENGLISH, "nice description", Locale.GERMAN, "nett Beschreibung"))
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
    public void buildChangeDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .description(LocalizedString.of(Locale.ENGLISH, "cool description"))
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategoryDraft).build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, draftFromCategory, categorySyncOptions)
                .orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
            .isEqualTo(newCategory.getDescription());
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildChangeDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .description(oldCategory.getDescription())
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a same description
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build set description update action for root categories with same description
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, draftFromCategory, categorySyncOptions)
                .orElse(null);

        assertThat(setDescriptionUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildChangeDescriptionUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .description(LocalizedString.of(Locale.GERMAN, "nett Beschreibung", Locale.ENGLISH, "nice description"))
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different order locales of description
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, draftFromCategory, categorySyncOptions).orElse(null);

        assertThat(setDescriptionUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildChangeDescriptionUpdateAction_WithNullValue_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, draftFromCategory, categorySyncOptions)
                .orElse(null);

        assertThat(setDescriptionUpdateAction).isNull();
        assertThat(callBackResponses).hasSize(1);
        assertThat(callBackResponses.get(0)).isEqualTo(format("Cannot unset 'description' field of category with id "
            + "'%s'.", oldCategory.getId()));
    }

}
