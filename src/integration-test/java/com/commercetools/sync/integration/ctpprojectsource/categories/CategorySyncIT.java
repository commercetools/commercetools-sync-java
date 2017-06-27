package com.commercetools.sync.integration.ctpprojectsource.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.expansion.CategoryExpansionModel;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCategoryDraftsWithPrefix;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.replaceReferenceIdsWithKeys;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CategorySyncIT {
    private CategorySync categorySync;
    private Category targetProjectRootCategory;
    private Category sourceProjectRootCategory;

    private List<String> callBackErrorResponses = new ArrayList<>();
    private List<Throwable> callBackExceptions = new ArrayList<>();
    private List<String> callBackWarningResponses = new ArrayList<>();

    /**
     * Deletes Categories and Types from source and target CTP projects, then it populates target CTP project with
     * category test data.
     */
    @Before
    public void setup() {
        deleteRootCategoriesFromTargetAndSource();
        deleteTypesFromTargetAndSource();

        targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        createCategories(CTP_TARGET_CLIENT, getMockCategoryDrafts(targetProjectRootCategory, 2));

        sourceProjectRootCategory = createRootCategory(CTP_SOURCE_CLIENT);
        callBackErrorResponses = new ArrayList<>();
        callBackExceptions = new ArrayList<>();
        callBackWarningResponses = new ArrayList<>();

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .setErrorCallBack((errorMessage, exception) -> {
                callBackErrorResponses.add(errorMessage);
                callBackExceptions.add(exception);
            })
            .setWarningCallBack(callBackWarningResponses::add)
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
    public void syncDrafts_withChangesOnly_ShouldUpdateCategories() {
        createCategories(CTP_SOURCE_CLIENT, getMockCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            sourceProjectRootCategory, 2));

        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        // Put the keys in the reference ids to prepare for reference resolution
        final List<CategoryDraft> categoryDrafts = replaceReferenceIdsWithKeys(categories);

        categorySync.sync(categoryDrafts);

        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 3, 0, 2, 0));

        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).hasSize(1);
        assertThat(callBackWarningResponses.get(0)).isEqualTo(format("Cannot unset 'parent' field of category with id"
            + " '%s'.", targetProjectRootCategory.getId()));
    }

    @Test
    public void syncDrafts_withNewCategories_ShouldCreateCategories() {
        createCategories(CTP_SOURCE_CLIENT, getMockCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            sourceProjectRootCategory, 3));

        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        // Put the keys in the reference ids to prepare for reference resolution
        final List<CategoryDraft> categoryDrafts = replaceReferenceIdsWithKeys(categories);

        categorySync.sync(categoryDrafts);

        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 4, 1, 2, 0));
        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).hasSize(1);
        assertThat(callBackWarningResponses.get(0)).isEqualTo(format("Cannot unset 'parent' field of category with id"
            + " '%s'.", targetProjectRootCategory.getId()));
    }

    @Test
    public void syncDrafts_WithUpdatedCategoriesWithoutReferenceKeys_ShouldNotSyncCategories() {
        createCategories(CTP_SOURCE_CLIENT, getMockCategoryDraftsWithPrefix(Locale.ENGLISH, "new",
            sourceProjectRootCategory, 2));

        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        final List<CategoryDraft> categoryDrafts = categories.stream()
                                                             .map(category -> CategoryDraftBuilder.of(category).build())
                                                             .collect(Collectors.toList());

        categorySync.sync(categoryDrafts);

        assertThat(categorySync.getStatistics().getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 3, 0, 0, 2));
        assertThat(callBackErrorResponses).hasSize(2);
        assertThat(callBackErrorResponses.get(0)).isEqualTo(format("Failed to resolve custom type reference on "
                + "CategoryDraft with externalId:'%s'. "
                + "Reason: com.commercetools.sync.commons.exceptions.ReferenceResolutionException: "
                + "Found a UUID in the id field. Expecting a key without a UUID value. If you want to allow UUID values"
                + " for reference keys, please use the setAllowUuidKeys(true) option in the sync options.",
            categoryDrafts.get(1).getExternalId()));
        assertThat(callBackErrorResponses.get(1)).isEqualTo(format("Failed to resolve custom type reference on "
                + "CategoryDraft with externalId:'%s'. "
                + "Reason: com.commercetools.sync.commons.exceptions.ReferenceResolutionException: "
                + "Found a UUID in the id field. Expecting a key without a UUID value. If you want to allow UUID values"
                + " for reference keys, please use the setAllowUuidKeys(true) option in the sync options.",
            categoryDrafts.get(2).getExternalId()));

        assertThat(callBackExceptions).hasSize(2);
        assertThat(callBackExceptions.get(0)).isInstanceOf(CompletionException.class);
        assertThat(callBackExceptions.get(0).getCause()).isInstanceOf(ReferenceResolutionException.class);


        assertThat(callBackExceptions.get(1)).isInstanceOf(CompletionException.class);
        assertThat(callBackExceptions.get(1).getCause()).isInstanceOf(ReferenceResolutionException.class);

        assertThat(callBackWarningResponses).hasSize(1);
        assertThat(callBackWarningResponses.get(0)).isEqualTo(format("Cannot unset 'parent' field of category with id"
            + " '%s'.", targetProjectRootCategory.getId()));
    }


}
