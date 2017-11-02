package com.commercetools.sync.integration.externalsource.categories;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsJsons;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class CategorySyncIT {
    private CategorySync categorySync;
    private static final String oldCategoryKey = "oldCategoryKey";

    /**
     * Delete all categories and types from target project. Then create custom types for target CTP project categories.
     */
    @BeforeClass
    public static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Categories and Types from target CTP project, then it populates it with category test data.
     */
    @Before
    public void setupTest() {
        deleteAllCategories(CTP_TARGET_CLIENT);

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                  .build();
        categorySync = new CategorySync(categorySyncOptions);

        // Create a mock in the target project.
        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
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
        deleteAllCategories(CTP_TARGET_CLIENT);
    }

    /**
     * Cleans up the entire target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void syncDrafts_WithANewCategoryWithNewSlug_ShouldCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                        .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 1, 0, 0, 0));
    }

    @Test
    public void syncDrafts_WithANewCategoryWithDuplicateSlug_ShouldNotCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                        .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 0, 1, 0));
    }

    @Test
    public void syncDrafts_WithCategoryWithNoChanges_ShouldNotUpdateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                        .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 0, 0, 0));
    }

    @Test
    public void syncDrafts_WithChangedCategory_ShouldUpdateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                                  .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewCategoryVersion() {
        // Category draft coming from external source.
        final LocalizedString newCategoryName = LocalizedString.of(Locale.ENGLISH, "Modern Furniture");
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(newCategoryName, LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncOptions categorySyncOptions =
            CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                      .build();
        final CategorySync categorySync1 = new CategorySync(categorySyncOptions);
        final CategorySync categorySync2 = new CategorySync(categorySyncOptions);
        final CategorySync categorySync3 = new CategorySync(categorySyncOptions);
        final CategorySync categorySync4 = new CategorySync(categorySyncOptions);
        final CategorySync categorySync5 = new CategorySync(categorySyncOptions);

        final CompletableFuture<CategorySyncStatistics> syncFuture1 =
            categorySync1.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture2 =
            categorySync2.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture3 =
            categorySync3.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture4 =
            categorySync4.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture5 =
            categorySync5.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();

        final List<CompletableFuture<CategorySyncStatistics>> futures =
            Arrays.asList(syncFuture1, syncFuture2, syncFuture3, syncFuture4, syncFuture5);

        final CompletableFuture<Void> parallelSyncExecutionFuture =
            allOf(futures.toArray(new CompletableFuture[futures.size()]));

        executeBlocking(
            parallelSyncExecutionFuture.thenAccept(voidResult -> {
                final CompletionStage<PagedQueryResult<Category>> categoryByKeyQuery =
                    CTP_TARGET_CLIENT.execute(CategoryQuery.of().plusPredicates(categoryQueryModel ->
                        categoryQueryModel.key().is(categoryDraft.getKey())));
                final Optional<Category> categoryOptional = executeBlocking(categoryByKeyQuery).head();
                assertThat(categoryOptional).isNotEmpty();
                assertThat(categoryOptional.get().getName()).isEqualTo(newCategoryName);
            }));
    }

    @Test
    public void syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        final SphereClient spyClient = getSpyClientThatFailsToFetchOn6thCall();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> errors = new ArrayList<>();

        final CategorySyncOptions categorySyncOptions =
            CategorySyncOptionsBuilder.of(spyClient)
                                      .setErrorCallBack((errorMessage, error) -> {
                                          errorMessages.add(errorMessage);
                                          errors.add(error);
                                      })
                                      .build();
        final CategorySync categorySync = new CategorySync(categorySyncOptions);

        final CompletableFuture<CategorySyncStatistics> syncFuture1 =
            categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture2 =
            categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture3 =
            categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture4 =
            categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();
        final CompletableFuture<CategorySyncStatistics> syncFuture5 =
            categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture();

        final List<CompletableFuture<CategorySyncStatistics>> futures =
            Arrays.asList(syncFuture1, syncFuture2, syncFuture3, syncFuture4, syncFuture5);

        final CompletableFuture<Void> parallelSyncExecutionFuture =
            allOf(futures.toArray(new CompletableFuture[futures.size()]));

        executeBlocking(
            parallelSyncExecutionFuture.thenAccept(voidResult -> {
                assertThat(errorMessages).contains(
                    format("Failed to fetch Categories with keys: '%s'. Reason: %s",
                        categoryDraft.getKey(), errors.get(0)));
                assertThat(errorMessages).contains(
                    format("Failed to update Category with key: '%s'. Reason: Failed to fetch category on retry.",
                        categoryDraft.getKey()));
                assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
            }));
    }

    private SphereClient getSpyClientThatFailsToFetchOn6thCall() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final CategoryQuery anyCategoryQuery = any(CategoryQuery.class);

        when(spyClient.execute(anyCategoryQuery))
            .thenCallRealMethod() // Test setup delete
            .thenCallRealMethod() // Test setup delete
            .thenCallRealMethod() // cache category keys
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenCallRealMethod() // Call real fetch on fetching matching categories
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        return spyClient;
    }

    @Test
    public void syncDrafts_WithNewCategoryWithExistingParent_ShouldCreateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("newCategory")
            .parent(Category.referenceOfId(oldCategoryKey))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                                  .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 1, 0, 0, 0));
    }

    @Test
    public void syncDraft_withARemovedCustomType_ShouldUpdateCategory() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .build();

        final CategorySyncStatistics syncStatistics = categorySync.sync(Collections.singletonList(categoryDraft))
                                                                  .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithMultipleBatchSyncing_ShouldSync() {
        // Existing array of [1, 2, 3, oldCategoryKey]
        final CategoryDraft oldCategoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"), LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .key("cat1")
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft1))
                         .toCompletableFuture()
                         .join();
        final CategoryDraft oldCategoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"), LocalizedString.of(Locale.ENGLISH, "furniture2"))
            .key("cat2")
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft2))
                         .toCompletableFuture()
                         .join();
        final CategoryDraft oldCategoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"), LocalizedString.of(Locale.ENGLISH, "furniture3"))
            .key("cat3")
            .custom(getCustomFieldsDraft())
            .build();
        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft3))
                         .toCompletableFuture()
                         .join();


        //_-----_-----_-----_-----_-----_PREPARE BATCHES FROM EXTERNAL SOURCE-----_-----_-----_-----_-----_-----
        //_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "oldCategoryKey"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .parent(Category.referenceOfId("cat7"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch1 = new ArrayList<>();
        batch1.add(categoryDraft1);

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat7"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat7")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch2 = new ArrayList<>();
        batch2.add(categoryDraft2);

        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat6"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat6")
            .parent(Category.referenceOfId("cat5"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch3 = new ArrayList<>();
        batch3.add(categoryDraft3);

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat5")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch4 = new ArrayList<>();
        batch4.add(categoryDraft4);

        final CategorySyncStatistics syncStatistics = categorySync.sync(batch1)
                                                        .thenCompose(result -> categorySync.sync(batch2))
                                                        .thenCompose(result -> categorySync.sync(batch3))
                                                        .thenCompose(result -> categorySync.sync(batch4))
                                                        .toCompletableFuture()
                                                        .join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 4, 3, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithMultipleBatchSyncingWithAlreadyProcessedDrafts_ShouldSync() {
        // Category draft coming from external source.
        CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "new name"),
                LocalizedString.of(Locale.ENGLISH, "new-slug"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> batch1 = new ArrayList<>();
        batch1.add(categoryDraft1);

        // Process same draft again in a different batch but with a different name.
        final LocalizedString anotherNewName = LocalizedString.of(Locale.ENGLISH, "another new name");
        categoryDraft1 = CategoryDraftBuilder.of(categoryDraft1)
                                             .name(anotherNewName)
                                             .build();

        final List<CategoryDraft> batch2 = new ArrayList<>();
        batch2.add(categoryDraft1);

        final CategorySyncStatistics syncStatistics = categorySync.sync(batch1)
                                                                  .thenCompose(result -> categorySync.sync(batch2))
                                                                  .toCompletableFuture()
                                                                  .join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 1, 0, 0));

        final Optional<Category> optionalResult = CTP_TARGET_CLIENT.execute(CategoryQuery.of().bySlug(Locale.ENGLISH,
            categoryDraft1.getSlug().get(Locale.ENGLISH))).toCompletableFuture().join().head();
        assertThat(optionalResult).isNotEmpty();
        assertThat(optionalResult.get().getName()).isEqualTo(anotherNewName);
    }

    @Test
    public void syncDrafts_WithOneBatchSyncing_ShouldSync() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();


        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat1")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft5 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft6 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(Category.referenceOfId("cat4"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);
        newCategoryDrafts.add(categoryDraft3);
        newCategoryDrafts.add(categoryDraft4);
        newCategoryDrafts.add(categoryDraft5);
        newCategoryDrafts.add(categoryDraft6);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 6, 5, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithSameSlugDraft_ShouldNotSyncIt() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        // Same slug draft
        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("cat1")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft5 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft6 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(Category.referenceOfId("cat4"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);
        newCategoryDrafts.add(categoryDraft3);
        newCategoryDrafts.add(categoryDraft4);
        newCategoryDrafts.add(categoryDraft5);
        newCategoryDrafts.add(categoryDraft6);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 6, 5, 0, 1, 0));
    }

    @Test
    public void syncDrafts_WithDraftWithInvalidParentKey_ShouldNotSyncIt() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        // Category draft coming from external source.
        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat1")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        // With invalid parent key
        final CategoryDraft categoryDraft3 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(Category.referenceOfId(UUID.randomUUID().toString()))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft4 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(Category.referenceOfId("cat1"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft5 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft6 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(Category.referenceOfId("cat4"))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);
        newCategoryDrafts.add(categoryDraft3);
        newCategoryDrafts.add(categoryDraft4);
        newCategoryDrafts.add(categoryDraft5);
        newCategoryDrafts.add(categoryDraft6);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 6, 4, 1, 1, 0));
    }

    @Test
    public void syncDrafts_WithValidAndInvalidCustomTypeKeys_ShouldSyncCorrectly() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();
        final String newCustomTypeKey = "newKey";
        createCategoriesCustomType(newCustomTypeKey, Locale.ENGLISH, "newCustomTypeName", CTP_TARGET_CLIENT);

        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson("nonExistingKey", getCustomFieldsJsons()))
            .build();

        final CategoryDraft categoryDraft2 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture-2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture-2"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(newCustomTypeKey, getCustomFieldsJsons()))
            .build();

        newCategoryDrafts.add(categoryDraft1);
        newCategoryDrafts.add(categoryDraft2);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 2, 1, 0, 1, 0));
    }

    @Test
    public void syncDrafts_WithValidCustomFieldsChange_ShouldSyncIt() {
        final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false));
        customFieldsJsons
            .put(LOCALISED_STRING_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.objectNode()
                                                                             .put("de", "rot")
                                                                             .put("en", "red")
                                                                             .put("it", "rosso"));

        final CategoryDraft categoryDraft1 = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, customFieldsJsons))
            .build();

        newCategoryDrafts.add(categoryDraft1);

        final CategorySyncStatistics syncStatistics = categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 1, 0, 1, 0, 0));
    }

    @Test
    public void syncDrafts_WithDraftWithAMissingParentKey_ShouldNotSyncIt() {
        // Category draft coming from external source.
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final String nonExistingParentKey = "nonExistingParent";
        final CategoryDraft categoryDraftWithMissingParent = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "new-furniture1"))
            .key("cat1")
            .parent(Category.referenceOfId(nonExistingParentKey))
            .custom(CustomFieldsDraft.ofTypeIdAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, getCustomFieldsJsons()))
            .build();

        final List<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(categoryDraft);
        categoryDrafts.add(categoryDraftWithMissingParent);


        final CategorySyncStatistics syncStatistics = categorySync.sync(categoryDrafts)
                                                                  .toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d categories were processed in total (%d created, %d updated, %d failed to"
                + " sync and %d categories with a missing parent).", 2, 2, 0, 0, 1));

        assertThat(syncStatistics.getCategoryKeysWithMissingParents()).hasSize(1);
        final ArrayList<String> missingParentsChildren = syncStatistics.getCategoryKeysWithMissingParents()
                                                                       .get(nonExistingParentKey);
        assertThat(missingParentsChildren).hasSize(1);
        final String childrenKeys = missingParentsChildren.get(0);
        assertThat(childrenKeys).isEqualTo(categoryDraftWithMissingParent.getKey());


    }
}
