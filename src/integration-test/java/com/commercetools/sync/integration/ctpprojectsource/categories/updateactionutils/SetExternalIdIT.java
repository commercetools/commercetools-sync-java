package com.commercetools.sync.integration.ctpprojectsource.categories.updateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.SetExternalId;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;


class SetExternalIdIT {
    private static Category oldCategory;
    private List<String> callBackResponses = new ArrayList<>();

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
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture"))
            .externalId("externalId1")
            .build();

        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();
    }

    /**
     * Deletes all the categories in the source CTP project and the callback response collector.
     */
    @BeforeEach
    void setupTest() {
        deleteAllCategories(CTP_SOURCE_CLIENT);
        callBackResponses = new ArrayList<>();
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
    void buildSetExternalIdHintUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .externalId("externalId2")
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
    void buildSetExternalIdUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .externalId(oldCategory.getExternalId())
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
