package com.commercetools.sync.integration.externalsource.categories.updateactionutils;

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
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class SetDescriptionIT {
    private static Category targetProjectRootCategory;
    private static Category oldCategory;
    private List<String> callBackResponses = new ArrayList<>();
    private CategorySyncOptions categorySyncOptions;

    /**
     * Deletes Categories and Types from the target CTP projects, then it populates it with category test data.
     */
    @BeforeClass
    public static void setup() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .description(
                LocalizedString.of(Locale.ENGLISH, "amazing description", Locale.GERMAN, "Erstaunliche Beschreibung"))
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

    /**
     * Cleans the callback response collector.
     */
    @Before
    public void setupTest() {
        callBackResponses = new ArrayList<>();
        categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                        .setWarningCallBack(callBackResponses::add)
                                                        .build();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentValues_ShouldBuildUpdateAction() {
        // Prepare new category draft with a different description
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .description(LocalizedString.of(Locale.ENGLISH, "amazing description"))
            .build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, newCategory, categorySyncOptions).orElse(null);

        assertThat(setDescriptionUpdateAction).isNotNull();
        assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
        assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
            .isEqualTo(LocalizedString.of(Locale.ENGLISH, "amazing description"));
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithDifferentLocaleOrder_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a different order of locales of description
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .description(
                LocalizedString.of(Locale.GERMAN, "Erstaunliche Beschreibung", Locale.ENGLISH, "amazing description"))
            .build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, newCategory, categorySyncOptions).orElse(null);

        assertThat(setDescriptionUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithSameValues_ShouldNotBuildUpdateAction() {
        // Prepare new category draft with a same description as root category
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .description(oldCategory.getDescription())
            .build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, newCategory, categorySyncOptions).orElse(null);

        assertThat(setDescriptionUpdateAction).isNull();
        assertThat(callBackResponses).isEmpty();
    }

    @Test
    public void buildSetDescriptionUpdateAction_WithNullValue_ShouldTriggerCallback() {
        // Prepare new category draft with no description
        final CategoryDraft newCategory = CategoryDraftBuilder
            .of(oldCategory.getName(), oldCategory.getSlug())
            .build();

        // Build set description update action
        final UpdateAction<Category> setDescriptionUpdateAction =
            buildSetDescriptionUpdateAction(oldCategory, newCategory, categorySyncOptions).orElse(null);

        assertThat(setDescriptionUpdateAction).isNull();
        assertThat(callBackResponses).hasSize(1);
        assertThat(callBackResponses.get(0)).isEqualTo(
            format("Cannot unset 'description' field of category with id '%s'.", oldCategory.getId()));
    }

}
