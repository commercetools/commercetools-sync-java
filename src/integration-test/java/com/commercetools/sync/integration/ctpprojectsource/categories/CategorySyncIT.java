package com.commercetools.sync.integration.ctpprojectsource.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.expansion.CategoryExpansionModel;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.batchCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createChildren;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategoriesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteRootCategory;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCategoryDraftsWithPrefix;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getMockCustomFieldsJsons;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.replaceReferenceIdsWithKeys;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.syncBatches;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.ITUtils.getStatisticsAsJSONString;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CategorySyncIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySyncIT.class);
    private CategorySync categorySync;
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

        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);
        createCategories(CTP_TARGET_CLIENT, getMockCategoryDrafts(targetProjectRootCategory, 2));
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_TARGET_CLIENT);

        sourceProjectRootCategory = createRootCategory(CTP_SOURCE_CLIENT);
        callBackErrorResponses = new ArrayList<>();
        callBackExceptions = new ArrayList<>();
        callBackWarningResponses = new ArrayList<>();

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .setErrorCallBack((errorMessage, exception) -> {
                callBackErrorResponses.add(errorMessage);
                callBackExceptions.add(exception);
                LOGGER.error(errorMessage, exception);
            })
            .setWarningCallBack((warningMessage) -> {
                callBackWarningResponses.add(warningMessage);
                LOGGER.warn(warningMessage);
            })
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

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 3, 0, 2, 0));

        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).isEmpty();
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

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 4, 1, 2, 0));
        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).isEmpty();
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

        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 3, 0, 0, 2));
        assertThat(callBackErrorResponses).hasSize(2);
        final String key1 = categoryDrafts.get(1).getKey();
        assertThat(callBackErrorResponses.get(0)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
                + " key:'%s'. Reason: %s: Failed to resolve custom type reference on "
                + "CategoryDraft with key:'%s'. "
                + "Reason: Found a UUID in the id field. Expecting a key without a UUID value. If you want to allow"
                + " UUID values for reference keys, please use the setAllowUuidKeys(true) option in the sync options.",
            key1, ReferenceResolutionException.class.getCanonicalName(), key1));
        final String key2 = categoryDrafts.get(2).getKey();
        assertThat(callBackErrorResponses.get(1)).isEqualTo(format("Failed to resolve references on CategoryDraft with"
                + " key:'%s'. Reason: %s: Failed to resolve custom type reference on "
                + "CategoryDraft with key:'%s'. Reason: "
                + "Found a UUID in the id field. Expecting a key without a UUID value. If you want to allow UUID values"
                + " for reference keys, please use the setAllowUuidKeys(true) option in the sync options.",
            key2, ReferenceResolutionException.class.getCanonicalName(), key2));

        assertThat(callBackExceptions).hasSize(2);
        assertThat(callBackExceptions.get(0)).isInstanceOf(CompletionException.class);
        assertThat(callBackExceptions.get(0).getCause()).isInstanceOf(ReferenceResolutionException.class);


        assertThat(callBackExceptions.get(1)).isInstanceOf(CompletionException.class);
        assertThat(callBackExceptions.get(1).getCause()).isInstanceOf(ReferenceResolutionException.class);

        assertThat(callBackWarningResponses).isEmpty();
    }


    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    public void syncDrafts_withNewShuffledBatchOfCategories_ShouldCreateCategories() {
        //-----------------Test Setup------------------------------------
        // Delete all categories in target project
        deleteRootCategory(CTP_TARGET_CLIENT);

        // Create a total of 131 categories in the source project
        final List<Category> subFamily =
            createChildren(5, sourceProjectRootCategory,
                sourceProjectRootCategory.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);

        for (final Category child : subFamily) {
            final List<Category> subsubFamily =
                createChildren(5, child, child.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
            for (final Category subChild : subsubFamily) {
                createChildren(4, subChild, subChild.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
            }
        }
        //---------------------------------------------------------------

        // Fetch categories from source project
        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        // Put the keys in the reference ids to prepare for reference resolution
        final List<CategoryDraft> categoryDrafts = replaceReferenceIdsWithKeys(categories);

        // Make sure there is no hierarchical order
        Collections.shuffle(categoryDrafts);

        // Simulate batches of categories where not all parent references are supplied at once.
        final List<List<CategoryDraft>> batches = batchCategories(categoryDrafts, 13);

        final long startTime = System.currentTimeMillis();
        LOGGER.info("Starting to sync categories:");
        final CategorySyncStatistics syncStatistics = syncBatches(categorySync, batches,
            CompletableFuture.completedFuture(null)).toCompletableFuture().join();
        LOGGER.info(syncStatistics.getReportMessage());
        try {
            LOGGER.info(getStatisticsAsJSONString(syncStatistics));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to build JSON String of summary.", exception);
        }
        final long syncTimeTaken = System.currentTimeMillis() - startTime;
        LOGGER.info("Syncing categories took: " + syncTimeTaken + "ms");

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 131, 131, 0, 0));
        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    public void syncDrafts_withExistingShuffledCategoriesWithChangingCategoryHeirarchachy_ShouldUpdateCategories() {
        //-----------------Test Setup------------------------------------
        // Delete all categories in target project
        deleteRootCategory(CTP_TARGET_CLIENT);
        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        // Create a total of 131 categories in the target project
        final List<Category> subFamily =
            createChildren(5, targetProjectRootCategory,
                targetProjectRootCategory.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);

        for (final Category child : subFamily) {
            final List<Category> subsubFamily =
                createChildren(5, child, child.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);
            for (final Category subChild : subsubFamily) {
                createChildren(4, subChild, subChild.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);
            }
        }
        //---------------------------------------------------------------

        // Create a total of 131 categories in the source project
        final List<Category> sourceSubFamily =
            createChildren(5, sourceProjectRootCategory,
                sourceProjectRootCategory.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);

        for (final Category child : sourceSubFamily) {
            final List<Category> subsubFamily =
                createChildren(5, sourceProjectRootCategory,
                    child.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
            for (final Category subChild : subsubFamily) {
                createChildren(4, sourceProjectRootCategory,
                    subChild.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
            }
        }
        //---------------------------------------------------------------

        // Fetch categories from source project
        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        // Put the keys in the reference ids to prepare for reference resolution
        final List<CategoryDraft> categoryDrafts = replaceReferenceIdsWithKeys(categories);
        Collections.shuffle(categoryDrafts);

        final List<List<CategoryDraft>> batches = batchCategories(categoryDrafts, 13);

        final long startTime = System.currentTimeMillis();
        LOGGER.info("Starting to sync categories:");
        final CategorySyncStatistics syncStatistics = syncBatches(categorySync, batches,
            CompletableFuture.completedFuture(null)).toCompletableFuture().join();
        LOGGER.info(syncStatistics.getReportMessage());
        try {
            LOGGER.info(getStatisticsAsJSONString(syncStatistics));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to build JSON String of summary.", exception);
        }
        final long syncTimeTaken = System.currentTimeMillis() - startTime;
        LOGGER.info("Syncing categories took: " + syncTimeTaken + "ms");

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 131, 0, 130, 0));
        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    public void syncDrafts_withExistingCategoriesThatChangeParents_ShouldUpdateCategories() {
        //-----------------Test Setup------------------------------------
        // Delete all categories in target project
        deleteRootCategory(CTP_TARGET_CLIENT);
        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        // Create a total of 2 categories in the target project
        final List<Category> subFamily =
            createChildren(1, targetProjectRootCategory,
                targetProjectRootCategory.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);

        for (final Category child : subFamily) {
            createChildren(1, child, child.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);
        }
        //---------------------------------------------------------------

        // Create a total of 2 categories in the source project
        final List<Category> sourceSubFamily =
            createChildren(1, sourceProjectRootCategory,
                sourceProjectRootCategory.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);

        for (final Category child : sourceSubFamily) {
            createChildren(1, sourceProjectRootCategory,
                child.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
        }
        //---------------------------------------------------------------

        // Fetch categories from source project
        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        // Put the keys in the reference ids to prepare for reference resolution
        final List<CategoryDraft> categoryDrafts = replaceReferenceIdsWithKeys(categories);
        Collections.shuffle(categoryDrafts);

        final List<List<CategoryDraft>> batches = batchCategories(categoryDrafts, 1);

        final long startTime = System.currentTimeMillis();
        LOGGER.info("Starting to sync categories:");
        final CategorySyncStatistics syncStatistics = syncBatches(categorySync, batches,
            CompletableFuture.completedFuture(null)).toCompletableFuture().join();
        LOGGER.info(syncStatistics.getReportMessage());
        try {
            LOGGER.info(getStatisticsAsJSONString(syncStatistics));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to build JSON String of summary.", exception);
        }
        final long syncTimeTaken = System.currentTimeMillis() - startTime;
        LOGGER.info("Syncing categories took: " + syncTimeTaken + "ms");

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 3, 0, 2, 0));
        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    public void syncDrafts_withANonExistingNewParent_ShouldUpdateCategories() {
        //-----------------Test Setup------------------------------------
        // Delete all categories in target project
        deleteRootCategory(CTP_TARGET_CLIENT);
        final Category targetProjectRootCategory = createRootCategory(CTP_TARGET_CLIENT);

        // Create a total of 2 categories in the target project.
        final CategoryDraft parentDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "parent"),
                LocalizedString.of(Locale.ENGLISH, "parent"))
            .key("parent")
            .parent(targetProjectRootCategory)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();
        final Category parentCreated = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(parentDraft))
                                                        .toCompletableFuture().join();

        final CategoryDraft childDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "child"),
                LocalizedString.of(Locale.ENGLISH, "child"))
            .key("child")
            .parent(parentCreated)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(childDraft)).toCompletableFuture().join();
        //------------------------------------------------------------------------------------------------------------
        // Create a total of 2 categories in the source project

        final CategoryDraft sourceParentDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "new-parent"),
                LocalizedString.of(Locale.ENGLISH, "new-parent"))
            .key("new-parent")
            .parent(sourceProjectRootCategory)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();
        final Category sourceParentCreated = CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(sourceParentDraft))
                                                        .toCompletableFuture().join();

        final CategoryDraft sourceChildDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "child-new-name"),
                LocalizedString.of(Locale.ENGLISH, "child"))
            .key("child")
            .parent(sourceParentCreated)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getMockCustomFieldsJsons()))
            .build();
        CTP_SOURCE_CLIENT.execute(CategoryCreateCommand.of(sourceChildDraft)).toCompletableFuture().join();
        //---------------------------------------------------------------

        // Fetch categories from source project
        final List<Category> categories = CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of()
                                  .withSort(sorting -> sorting.createdAt().sort().asc())
                                  .withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                  .withExpansionPaths(ExpansionPath.of("custom.type"))
                                  .plusExpansionPaths(CategoryExpansionModel::parent)
            )
            .toCompletableFuture().join().getResults();

        // Put the keys in the reference ids to prepare for reference resolution
        final List<CategoryDraft> categoryDrafts = replaceReferenceIdsWithKeys(categories);

        // To simulate the new parent coming in a later draft
        Collections.reverse(categoryDrafts);

        final List<List<CategoryDraft>> batches = batchCategories(categoryDrafts, 1);

        final long startTime = System.currentTimeMillis();
        LOGGER.info("Starting to sync categories:");
        final CategorySyncStatistics syncStatistics = syncBatches(categorySync, batches,
            CompletableFuture.completedFuture(null)).toCompletableFuture().join();
        LOGGER.info(syncStatistics.getReportMessage());
        try {
            LOGGER.info(getStatisticsAsJSONString(syncStatistics));
        } catch (JsonProcessingException exception) {
            LOGGER.error("Failed to build JSON String of summary.", exception);
        }
        final long syncTimeTaken = System.currentTimeMillis() - startTime;
        LOGGER.info("Syncing categories took: " + syncTimeTaken + "ms");

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated and %d categories"
                + " failed to sync).", 3, 1, 1, 0));
        assertThat(callBackErrorResponses).isEmpty();
        assertThat(callBackExceptions).isEmpty();
        assertThat(callBackWarningResponses).isEmpty();
    }
}
