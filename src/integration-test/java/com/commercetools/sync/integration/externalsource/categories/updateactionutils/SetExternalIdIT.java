package com.commercetools.sync.integration.externalsource.categories.updateactionutils;


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
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

public class SetExternalIdIT {
    private static Category oldCategory;
    private List<String> callBackResponses = new ArrayList<>();

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .externalId("oldExternalId")
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

    /**
     * Cleans the callback response collector.
     */
    @Before
    public void setupTest() {
        callBackResponses = new ArrayList<>();
    }

    @Test
    public void buildSetExternalIdUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different externalId
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .externalId("newExternalId")
            .build();

        // Build set externalId update action
        final UpdateAction<Category> setExternalIdUpdateAction =
            buildSetExternalIdUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(setExternalIdUpdateAction).isNotNull();
        assertThat(setExternalIdUpdateAction.getAction()).isEqualTo("setExternalId");
        assertThat(((SetExternalId) setExternalIdUpdateAction).getExternalId())
            .isEqualTo(newCategory.getExternalId());
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildSetExternalIdUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a same externalId as root category
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .externalId(oldCategory.getExternalId())
            .build();

        // Build set externalId update action
        final UpdateAction<Category> setExternalIdUpdateAction =
            buildSetExternalIdUpdateAction(oldCategory, newCategory).orElse(null);

        assertThat(setExternalIdUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }


}
