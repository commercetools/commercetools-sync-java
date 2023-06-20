package com.commercetools.sync.integration.sdk2.externalsource.categories;

import static com.commercetools.sync.integration.sdk2.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldContainerBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.categories.CategorySync;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.categories.helpers.CategorySyncStatistics;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.*;

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
    ensureCategoriesCustomType(
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
  }

  /** Cleans up the target test data that were built in each test. */
  @AfterEach
  void tearDownTest() {
    deleteAllCategories(CTP_TARGET_CLIENT);
  }

  /** Cleans up the entire target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteCategorySyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void syncDrafts_WithANewCategoryWithNewSlug_ShouldCreateCategory() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(getCustomFieldsDraft())
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
  }

  @Test
  void syncDrafts_WithANewCategoryWithDuplicateSlug_ShouldNotCreateCategory() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key("newCategoryKey")
            .custom(getCustomFieldsDraft())
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  void syncDrafts_WithCategoryWithNoChanges_ShouldNotUpdateCategory() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
  }

  @Test
  void syncDrafts_WithChangesInExistingCategoryNameAndSlug_ShouldUpdateCategory() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewCategoryWithSuccess() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    final Category oldCategory =
        ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft)).get(0);
    // Preparation
    final ProjectApiRoot mockClient =
        buildClientWithConcurrentModificationUpdate(oldCategory.getId());

    final LocalizedString newCategoryName = LocalizedString.of(Locale.ENGLISH, "Modern Furniture");
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(newCategoryName)
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mockClient).build();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);

    // Test
    final CategorySyncStatistics statistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    // Assertion
    assertThat(statistics).hasValues(1, 0, 1, 0);

    // Assert CTP state.

    final ApiHttpResponse<Category> queryResult =
        CTP_TARGET_CLIENT
            .categories()
            .withKey(categoryDraft.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join();

    assertThat(queryResult.getBody()).isNotNull();
    assertThat(queryResult.getBody().getName()).isEqualTo(newCategoryName);
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdate(
      @Nonnull final String oldCategoryId) {

    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger requestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("categories/" + oldCategoryId)
                      && ApiHttpMethod.POST.equals(method)) {
                    if (requestInvocationCounter.getAndIncrement() == 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createConcurrentModificationException());
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  @Test
  void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    final Category oldCategory =
        ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft)).get(0);

    // Preparation
    final ProjectApiRoot mockClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry(oldCategory);

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);
    final CategorySyncStatistics statistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(statistics).hasValues(1, 0, 0, 1);
    assertThat(errorMessages).hasSize(1);
    assertThat(errors).hasSize(1);

    assertThat(errors.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorMessages.get(0))
        .contains(
            "Reason: Failed to fetch from CTP while retrying after concurrency modification.");
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry(
      @Nonnull final Category oldCategory) {
    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger requestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("categories/key=" + oldCategory.getKey())
                      && ApiHttpMethod.GET.equals(method)) {
                    if (requestInvocationCounter.getAndIncrement() > 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createBadGatewayException());
                    }
                  } else if (uri.contains("categories/" + oldCategory.getId())
                      && ApiHttpMethod.POST.equals(method)) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  @Test
  void
      syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    final Category oldCategory =
        ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft)).get(0);
    // Preparation
    final ProjectApiRoot mockClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry(oldCategory);

    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errors.add(exception.getCause());
                })
            .build();

    final CategorySync categorySync = new CategorySync(categorySyncOptions);
    final CategorySyncStatistics statistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

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
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry(
      @Nonnull final Category oldCategory) {
    final AtomicInteger requestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("categories/key=" + oldCategory.getKey())
                      && ApiHttpMethod.GET.equals(method)) {
                    if (requestInvocationCounter.getAndIncrement() > 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createNotFoundException());
                    }
                  }
                  if (uri.contains("categories/" + oldCategory.getId())
                      && ApiHttpMethod.POST.equals(method)) {
                    return CompletableFutureUtils.exceptionallyCompletedFuture(
                        createConcurrentModificationException());
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  @Test
  void syncDrafts_WithNewCategoryWithExistingParent_ShouldCreateCategory() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("newCategory")
            .custom(getCustomFieldsDraft())
            .parent(CategoryResourceIdentifierBuilder.of().key(oldCategoryKey).build())
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
  }

  @Test
  void syncDraft_withARemovedCustomType_ShouldUpdateCategory() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .build();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(singletonList(categoryDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithMultipleBatchSyncing_ShouldSync() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Existing array of [1, 2, 3, oldCategoryKey]
    final CategoryDraft oldCategoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat1"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .key("cat1")
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT.categories().post(oldCategoryDraft1).execute().toCompletableFuture().join();
    final CategoryDraft oldCategoryDraft2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture2"))
            .key("cat2")
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT.categories().post(oldCategoryDraft2).execute().toCompletableFuture().join();
    final CategoryDraft oldCategoryDraft3 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat3"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture3"))
            .key("cat3")
            .custom(getCustomFieldsDraft())
            .build();
    CTP_TARGET_CLIENT.categories().post(oldCategoryDraft3).execute().toCompletableFuture().join();

    // _-----_-----_-----_-----_-----_PREPARE BATCHES FROM EXTERNAL
    // SOURCE-----_-----_-----_-----_-----_-----
    // _-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----_-----

    // Category draft coming from external source.
    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "oldCategoryKey"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .parent(CategoryResourceIdentifierBuilder.of().key("cat7").build())
            .custom(getCustomFieldsDraft())
            .build();

    final List<CategoryDraft> batch1 = new ArrayList<>();
    batch1.add(categoryDraft1);

    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat7"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat7")
            .custom(getCustomFieldsDraft())
            .build();

    final List<CategoryDraft> batch2 = new ArrayList<>();
    batch2.add(categoryDraft2);

    final CategoryDraft categoryDraft3 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat6"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat6")
            .parent(CategoryResourceIdentifierBuilder.of().key("cat5").build())
            .custom(getCustomFieldsDraft())
            .build();

    final List<CategoryDraft> batch3 = new ArrayList<>();
    batch3.add(categoryDraft3);

    final CategoryDraft categoryDraft4 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat5"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat5")
            .custom(getCustomFieldsDraft())
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
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "new name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "new-slug"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
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

    assertThat(syncStatistics).hasValues(2, 0, 2, 0, 0);

    final CategoryPagedQueryResponse queryResponse =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .withQueryParam("slug", categoryDraft1.getSlug().get(Locale.ENGLISH))
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();
    assertThat(queryResponse).isNotNull();
    assertThat(queryResponse.getResults()).hasSize(1);
    assertThat(queryResponse.getResults().get(0).getName()).isEqualTo(anotherNewName);
  }

  @Test
  void syncDrafts_WithOneBatchSyncing_ShouldSync() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

    // Category draft coming from external source.
    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat1"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture1"))
            .key("cat1")
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft3 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .custom(getCustomFieldsDraft())
            .parent(CategoryResourceIdentifierBuilder.of().key("cat1").build())
            .build();

    final CategoryDraft categoryDraft4 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat3"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(CategoryResourceIdentifierBuilder.of().key("cat1").build())
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft5 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat4"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft6 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat5"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .custom(getCustomFieldsDraft())
            .parent(CategoryResourceIdentifierBuilder.of().key("cat4").build())
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
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

    // Category draft coming from external source.
    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    // Same slug draft
    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat1"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key("cat1")
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft3 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture2"))
            .key("cat2")
            .custom(getCustomFieldsDraft())
            .parent(CategoryResourceIdentifierBuilder.of().key("cat1").build())
            .build();

    final CategoryDraft categoryDraft4 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat3"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture3"))
            .key("cat3")
            .parent(CategoryResourceIdentifierBuilder.of().key("cat1").build())
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft5 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat4"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture4"))
            .key("cat4")
            .custom(getCustomFieldsDraft())
            .build();

    final CategoryDraft categoryDraft6 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "cat5"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture5"))
            .key("cat5")
            .parent(CategoryResourceIdentifierBuilder.of().key("cat4").build())
            .custom(getCustomFieldsDraft())
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
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();
    final String newCustomTypeKey = "newKey";
    ensureCategoriesCustomType(
        newCustomTypeKey, Locale.ENGLISH, "newCustomTypeName", CTP_TARGET_CLIENT);

    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("nonExistingKey").build())
                    .fields(createCustomFieldsJsonMap())
                    .build())
            .build();

    final CategoryDraft categoryDraft2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture-2"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture-2"))
            .key("newCategoryKey")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key(newCustomTypeKey).build())
                    .fields(createCustomFieldsJsonMap())
                    .build())
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
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    final List<CategoryDraft> newCategoryDrafts = new ArrayList<>();

    final FieldContainerBuilder fieldContainerBuilder = FieldContainerBuilder.of();
    fieldContainerBuilder.addValue(
        BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false));
    fieldContainerBuilder.addValue(
        LOCALISED_STRING_CUSTOM_FIELD_NAME,
        JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red").put("it", "rosso"));

    final CategoryDraft categoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "Modern Furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "modern-furniture"))
            .key(oldCategoryKey)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        TypeResourceIdentifierBuilder.of()
                            .key(OLD_CATEGORY_CUSTOM_TYPE_KEY)
                            .build())
                    .fields(fieldContainerBuilder.build())
                    .build())
            .build();

    newCategoryDrafts.add(categoryDraft1);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(newCategoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
  }

  @Test
  void syncDrafts_WithDraftWithAMissingParentKey_ShouldNotSyncIt() {
    // Create a mock in the target project.
    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    ensureCategories(CTP_TARGET_CLIENT, singletonList(oldCategoryDraft));

    // Category draft coming from external source.
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "new-furniture"))
            .key("newCategoryKey")
            .custom(getCustomFieldsDraft())
            .build();

    final String nonExistingParentKey = "nonExistingParent";
    final CategoryDraft categoryDraftWithMissingParent =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "furniture"))
            .slug(LocalizedString.of(Locale.ENGLISH, "new-furniture1"))
            .key("cat1")
            .parent(CategoryResourceIdentifierBuilder.of().key(nonExistingParentKey).build())
            .custom(getCustomFieldsDraft())
            .build();

    final List<CategoryDraft> categoryDrafts = new ArrayList<>();
    categoryDrafts.add(categoryDraft);
    categoryDrafts.add(categoryDraftWithMissingParent);

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 1, 0, 0, 1);
  }
}
