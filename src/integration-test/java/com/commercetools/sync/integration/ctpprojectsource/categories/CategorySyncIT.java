package com.commercetools.sync.integration.ctpprojectsource.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.queries.CategoryQuery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCategoryDraftsWithPrefix;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CategorySyncIT {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(com.commercetools.sync.integration.externalsource.categories.CategorySyncIT.class);
    private CategorySync categorySync;
    private Category targetProjectRootCategory;
    private Category sourceProjectRootCategory;

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @Before
    public void setup() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();

        sourceProjectRootCategory = createRootCategory(CTP_SOURCE_CLIENT);
        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        createCategories(CTP_TARGET_CLIENT, getMockCategoryDrafts(targetProjectRootCategory, 2));
        createCategories(CTP_SOURCE_CLIENT, getMockCategoryDraftsWithPrefix(Locale.GERMAN, "new",
            sourceProjectRootCategory, 2));

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
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();
    }

    @Test
    @Ignore // TODO: Remove ignore annotation once JVM SDK GH issue #1444 is resolved.
    public void syncDrafts_FromAnotherCtpProject_ShouldUpdateCategories() {
        final List<Category> sourceCategories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of().withLimit(SphereClientUtils.QUERY_MAX_LIMIT))
            .toCompletableFuture().join().getResults();
        final List<CategoryDraft> sourceCategoryDrafts = sourceCategories
            .stream()
            .map(category -> CategoryDraftBuilder.of(category).build())
            .collect(Collectors.toList());

        categorySync.sync(sourceCategoryDrafts);

        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 1, 0, 1, 0));
    }


}
