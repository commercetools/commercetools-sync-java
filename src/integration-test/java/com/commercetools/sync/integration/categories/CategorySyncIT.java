package com.commercetools.sync.integration.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Locale;

import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.deleteCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.getMockCategoryDrafts;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.getMockCategoryDraftsWithNamePrefix;
import static com.commercetools.sync.integration.categories.utils.CategoryITUtils.getMockCustomFieldsJsons;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CategorySyncIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncIT.class);
    private CategorySync categorySync;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @Before
    public void setup() {
        deleteCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
        createCategories(CTP_TARGET_CLIENT, getMockCategoryDrafts());
        createCategories(CTP_SOURCE_CLIENT, getMockCategoryDraftsWithNamePrefix(Locale.ENGLISH, "new"));

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                  .setErrorCallBack(LOGGER::error)
                                                                                  .setErrorCallBack(LOGGER::warn)
                                                                                  .build();
        categorySync = new CategorySync(categorySyncOptions);
    }

    /**
     * Cleans up the target and source test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
    }

    @Test
    public void syncDrafts_WithANewCategory_ShouldCreateCategory() {
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "newCategory"),
                LocalizedString.of(Locale.ENGLISH, "new-category-slug"))
            .externalId("newCategoryExternalId")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();

        categorySync.sync(Collections.singletonList(categoryDraft));
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 1, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithChangedCategory_ShouldUpdateCategory() {
        final CategoryDraft updatedCategory =
            CategoryDraftBuilder
                .of(LocalizedString.of(Locale.ENGLISH, "newCategory"),
                    LocalizedString.of(Locale.ENGLISH, "new-category-slug"))
                .externalId("1")
                .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
                .build();

        categorySync.sync(Collections.singletonList(updatedCategory));
        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 1, 0, 1, 0));
    }
}
