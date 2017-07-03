package com.commercetools.sync.integration.ctpprojectsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetExternalId;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;


public class SetExternalIdIT {
    private Category sourceProjectRootCategory;
    private static Category oldCategory;
    private List<String> callBackResponses = new ArrayList<>();

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
            .externalId("externalId1")
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
    public void buildSetExternalIdHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .externalId("externalId2")
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different external id
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategoryDraft).build();

        // Build set external id update action
        final UpdateAction<Category> setExternalId = buildSetExternalIdUpdateAction(oldCategory, draftFromCategory)
            .orElse(null);

        assertThat(setExternalId).isNotNull();
        assertThat(setExternalId.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalId).getExternalId())
            .isEqualTo(newCategory.getExternalId());
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildSetExternalIdUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .externalId(oldCategory.getExternalId())
            .parent(sourceProjectRootCategory)
            .build();
        final Category newCategory = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture()
                                                      .join();

        // Prepare new category draft with a different externalId
        final CategoryDraft draftFromCategory = CategoryDraftBuilder.of(newCategory).build();


        // Build set externalId update action for root categories with same externalId
        final UpdateAction<Category> setExternalId = buildSetExternalIdUpdateAction(oldCategory, draftFromCategory)
            .orElse(null);

        assertThat(setExternalId).isNull();
        assertThat(callBackResponses).isEmpty();
    }
}
