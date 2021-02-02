package com.commercetools.sync.integration.externalsource.categories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
import io.sphere.sdk.categories.commands.CategoryUpdateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorySyncIT {
  private CategorySync categorySync;
  private static final String oldCategoryKey = "oldCategoryKey";
  private List<String> errorCallBackMessages = new ArrayList<>();
  private List<Throwable> errorCallBackExceptions = new ArrayList<>();

  /**
   * Delete all categories and types from target project. Then create custom types for target CTP
   * project categories.
   */
  @BeforeAll
  static void setup() {
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
  }

  /**
   * Deletes Categories and Types from target CTP project, then it populates it with category test
   * data.
   */
  @BeforeEach
  void setupTest() {
    deleteAllCategories(CTP_TARGET_CLIENT);

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    categorySync = new CategorySync(categorySyncOptions);

    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT
        .execute(CategoryCreateCommand.of(oldCategoryDraft))
        .toCompletableFuture()
        .join();
  }

  /** Cleans up the target test data that were built in each test. */
  @AfterEach
  void tearDownTest() {
    deleteAllCategories(CTP_TARGET_CLIENT);
  }

  /** Cleans up the entire target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void syncDrafts_WithANewCategoryWithNewSlug_ShouldCreateCategory() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
  }

  @Test
  void syncDrafts_WithANewCategoryWithDuplicateSlug_ShouldNotCreateCategory() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("newCategoryKey")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void syncDrafts_WithCategoryWithNoChanges_ShouldNotUpdateCategory() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
  }

  @Test
  void syncDrafts_WithChangedCategory_ShouldUpdateCategory() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewCategoryWithSuccess() {
    // Preparation
    final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

    final LocalizedString newCategoryName = LocalizedString.of(Locale.ENGLISH, "Modern Furniture");
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                newCategoryName, LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(spyClient).build();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);

    // Test
    final CategorySyncStatistics statistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 1, 0);

    // Assert CTP state.
    final PagedQueryResult<Category> queryResult =
        CTP_TARGET_CLIENT
            .execute(
                CategoryQuery.of()
                    .plusPredicates(
                        categoryQueryModel -> categoryQueryModel.key().is(categoryDraft.getKey())))
            .toCompletableFuture()
            .join();

    assertThat(queryResult.head())
        .hasValueSatisfying(category -> assertThat(category.getName()).isEqualTo(newCategoryName));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdate() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

    final CategoryUpdateCommand anyCategoryUpdate = any(CategoryUpdateCommand.class);
    when(spyClient.execute(anyCategoryUpdate))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new ConcurrentModificationException()))
        .thenCallRealMethod();

    return spyClient;
  }

  @Test
  void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // Preparation
    final SphereClient spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);
    final CategorySyncStatistics statistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);
    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update Category with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                categoryDraft.getKey()));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final CategoryQuery anyCategoryQuery = any(CategoryQuery.class);

    when(spyClient.execute(anyCategoryQuery))
        .thenCallRealMethod() // cache category keys
        .thenCallRealMethod() // Call real fetch on fetching matching categories
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

    final CategoryUpdateCommand anyCategoryUpdate = any(CategoryUpdateCommand.class);
    when(spyClient.execute(anyCategoryUpdate))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new ConcurrentModificationException()));

    return spyClient;
  }

  @Test
  void
      syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // Preparation
    final SphereClient spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);
    final CategorySyncStatistics statistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);
    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);

    assertThat(errorMessages.get(0))
        .contains(
            format(
                "Failed to update Category with key: '%s'. Reason: Not found when attempting to fetch while"
                    + " retrying after concurrency modification.",
                categoryDraft.getKey()));
  }

  @Nonnull
  private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final CategoryQuery anyCategoryQuery = any(CategoryQuery.class);

    when(spyClient.execute(anyCategoryQuery))
        .thenCallRealMethod() // cache category keys
        .thenCallRealMethod() // Call real fetch on fetching matching categories
        .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

    final CategoryUpdateCommand anyCategoryUpdate = any(CategoryUpdateCommand.class);
    when(spyClient.execute(anyCategoryUpdate))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new ConcurrentModificationException()));

    return spyClient;
  }

  @Test
  void syncDrafts_WithNewCategoryWithExistingParent_ShouldCreateCategory() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("newCategory")
            .parent(ResourceIdentifier.ofKey(oldCategoryKey))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
  }

  @Test
  void syncDraft_withARemovedCustomType_ShouldUpdateCategory() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(Collections.singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithMultipleBatchSyncing_ShouldSync() {
    // Existing array of [1, 2, 3, oldCategoryKey]
    final CategoryDraft oldCategoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .key("cat1")
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT
        .execute(CategoryCreateCommand.of(oldCategoryDraft1))
        .toCompletableFuture()
        .join();
    final CategoryDraft oldCategoryDraft2 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "furniture2"))
            .key("cat2")
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT
        .execute(CategoryCreateCommand.of(oldCategoryDraft2))
        .toCompletableFuture()
        .join();
    final CategoryDraft oldCategoryDraft3 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "furniture3"))
            .key("cat3")
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT
        .execute(CategoryCreateCommand.of(oldCategoryDraft3))
        .toCompletableFuture()
        .join();

    // _-----_-----_-----_-----_-----_PREPARE BATCHES FROM EXTERNAL
    // SOURCE-----_-----_-----_-----_-----_-----
    // _-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----

    // Category draft coming from external source.
    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "oldCategoryKey"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .parent(ResourceIdentifier.ofKey("cat7"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<CategoryDraft> batch1 = new ArrayList<>();
    batch1.add(categoryDraft1);

    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat7"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat7")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<CategoryDraft> batch2 = new ArrayList<>();
    batch2.add(categoryDraft2);

    final CategoryDraft categoryDraft3 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat6"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat6")
            .parent(ResourceIdentifier.ofKey("cat5"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<CategoryDraft> batch3 = new ArrayList<>();
    batch3.add(categoryDraft3);

    final CategoryDraft categoryDraft4 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat5")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<CategoryDraft> batch4 = new ArrayList<>();
    batch4.add(categoryDraft4);

    final CategorySyncStatistics syncStatistics =
        categorySync
            .sync(batch1)
            .thenCompose(result -> categorySync.sync(batch2))
            .thenCompose(result -> categorySync.sync(batch3))
            .thenCompose(result -> categorySync.sync(batch4))
            .toCompletableFuture()
            .join();

    assertThat(syncStatistics).hasValues(4, 3, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithMultipleBatchSyncingWithAlreadyProcessedDrafts_ShouldSync() {
    // Category draft coming from external source.
    CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "new name"),
                LocalizedString.of(Locale.ENGLISH, "new-slug"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<CategoryDraft> batch1 = new ArrayList<>();
    batch1.add(categoryDraft1);

    // Process same draft again in a different batch but with a different name.
    final LocalizedString anotherNewName = LocalizedString.of(Locale.ENGLISH, "another new name");
    categoryDraft1 = CategoryDraftBuilder.of(categoryDraft1).name(anotherNewName).build();

    final List<CategoryDraft> batch2 = new ArrayList<>();
    batch2.add(categoryDraft1);

    final CategorySyncStatistics syncStatistics =
        categorySync
            .sync(batch1)
            .thenCompose(result -> categorySync.sync(batch2))
            .toCompletableFuture()
            .join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    final Optional<Category> optionalResult =
        CTP_TARGET_CLIENT
            .execute(
                CategoryQuery.of()
                    .bySlug(Locale.ENGLISH, categoryDraft1.getSlug().get(Locale.ENGLISH)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(optionalResult).isNotEmpty();
    assertThat(optionalResult.get().getName()).isEqualTo(anotherNewName);
  }

  @Test
  void syncDrafts_WithOneBatchSyncing_ShouldSync() {
    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

    // Category draft coming from external source.
    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat1")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft3 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(ResourceIdentifier.ofKey("cat1"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft4 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(ResourceIdentifier.ofKey("cat1"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft5 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft6 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(ResourceIdentifier.ofKey("cat4"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    newCategoryDrafts.add(categoryDraft1);
    newCategoryDrafts.add(categoryDraft2);
    newCategoryDrafts.add(categoryDraft3);
    newCategoryDrafts.add(categoryDraft4);
    newCategoryDrafts.add(categoryDraft5);
    newCategoryDrafts.add(categoryDraft6);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(6, 5, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithSameSlugDraft_ShouldNotSyncIt() {
    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

    // Category draft coming from external source.
    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    // Same slug draft
    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("cat1")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft3 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .parent(ResourceIdentifier.ofKey("cat1"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft4 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat3"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(ResourceIdentifier.ofKey("cat1"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft5 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat4"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft6 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat5"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(ResourceIdentifier.ofKey("cat4"))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    newCategoryDrafts.add(categoryDraft1);
    newCategoryDrafts.add(categoryDraft2);
    newCategoryDrafts.add(categoryDraft3);
    newCategoryDrafts.add(categoryDraft4);
    newCategoryDrafts.add(categoryDraft5);
    newCategoryDrafts.add(categoryDraft6);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(6, 5, 0, 1, 0);
  }

  @Test
  void syncDrafts_WithValidAndInvalidCustomTypeKeys_ShouldSyncCorrectly() {
    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();
    final String newCustomTypeKey = "newKey";
    createCategoriesCustomType(
        newCustomTypeKey, Locale.ENGLISH, "newCustomTypeName", CTP_TARGET_CLIENT);

    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson("nonExistingKey", createCustomFieldsJsonMap()))
            .build();

    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture-2"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture-2"))
            .key("newCategoryKey")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(newCustomTypeKey, createCustomFieldsJsonMap()))
            .build();

    newCategoryDrafts.add(categoryDraft1);
    newCategoryDrafts.add(categoryDraft2);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 1, 0, 1, 0);

    final String expectedMessage = format(TYPE_DOES_NOT_EXIST, "nonExistingKey");
    assertThat(errorCallBackMessages.get(0)).contains(expectedMessage);
  }

  @Test
  void syncDrafts_WithValidCustomFieldsChange_ShouldSyncIt() {
    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

    final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
    customFieldsJsons.put(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false));
    customFieldsJsons.put(
        LOCALISED_STRING_CUSTOM_FIELD_NAME,
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red").put("it", "rosso"));

    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "Modern Furniture"),
                LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(OLD_CATEGORY_CUSTOM_TYPE_KEY, customFieldsJsons))
            .build();

    newCategoryDrafts.add(categoryDraft1);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithDraftWithAMissingParentKey_ShouldNotSyncIt() {
    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final String nonExistingParentKey = "nonExistingParent";
    final CategoryDraft categoryDraftWithMissingParent =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "new-furniture1"))
            .key("cat1")
            .parent(ResourceIdentifier.ofKey(nonExistingParentKey))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    final List<CategoryDraft> categoryDrafts = new ArrayList<>();
    categoryDrafts.add(categoryDraft);
    categoryDrafts.add(categoryDraftWithMissingParent);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 1, 0, 0, 1);

    final Map<String, Set<String>> categoryKeysWithMissingParents =
        syncStatistics.getCategoryKeysWithMissingParents();
    assertThat(categoryKeysWithMissingParents).hasSize(1);

    final Set<String> missingParentsChildren =
        categoryKeysWithMissingParents.get(nonExistingParentKey);
    assertThat(missingParentsChildren).hasSize(1);

    final String childKey = missingParentsChildren.iterator().next();
    assertThat(childKey).isEqualTo(categoryDraftWithMissingParent.getKey());
  }
}
