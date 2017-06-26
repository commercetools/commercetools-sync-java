package com.commercetools.sync.integration.externalsource.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Locale;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCustomFieldsJsons;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CategorySyncIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncIT.class);
    private CategorySync categorySync;
    private Category targetProjectRootCategory;
    private final String oldCategoryExternalId = "oldCategoryExternalId";

    /**
     * Deletes Categories and Types from target CTP project, then it populates it with category test data.
     */
    @Before
    public void setupTest() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                  .setErrorCallBack(LOGGER::error)
                                                                                  .setErrorCallBack(LOGGER::warn)
                                                                                  .build();
        categorySync = new CategorySync(categorySyncOptions);

        // Create a mock in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .externalId(oldCategoryExternalId)
            .parent(targetProjectRootCategory)
            .custom(getMockCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                         .toCompletableFuture()
                         .join();
    }

    /**
     * Cleans up the target test data that were built in each test.
     */
    @After
    public void tearDownTest() {
        deleteRootCategory(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the entire target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteRootCategory(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void syncDrafts_WithANewCategoryWithNewSlug_ShouldCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .externalId("newCategoryExternalId")
            .parent(targetProjectRootCategory)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();

        categorySync.sync(Collections.singletonList(categoryDraft));
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 1, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithANewCategoryWithDuplicateSlug_ShouldNotCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .externalId("newCategoryExternalId")
            .parent(targetProjectRootCategory)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();

        categorySync.sync(Collections.singletonList(categoryDraft));
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 1, 0, 0, 1));
    }

    @Test
    public void syncDrafts_WithChangedCategory_ShouldUpdateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .externalId(oldCategoryExternalId)
            .parent(targetProjectRootCategory)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();

        categorySync.sync(Collections.singletonList(categoryDraft));
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 1, 0, 1, 0));
    }

}
