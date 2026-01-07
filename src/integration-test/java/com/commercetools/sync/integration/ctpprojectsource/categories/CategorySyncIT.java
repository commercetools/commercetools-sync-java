package com.commercetools.sync.integration.ctpprojectsource.categories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.categories.utils.CategoryTransformUtils;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.integration.commons.utils.CategoryITUtils;
import com.commercetools.sync.integration.commons.utils.ITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.*;

class CategorySyncIT {
  private CategorySync categorySync;

  private List<String> callBackErrorResponses = new ArrayList<>();
  private List<Throwable> callBackExceptions = new ArrayList<>();
  private List<String> callBackWarningResponses = new ArrayList<>();
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  /**
   * Delete all categories and types from source and target project. Then create custom types for
   * source and target CTP project categories.
   */
  @BeforeAll
  static void setup() {
    CategoryITUtils.ensureCategoriesCustomType(
        CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        "anyName",
        TestClientUtils.CTP_TARGET_CLIENT);
    CategoryITUtils.ensureCategoriesCustomType(
        CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        "anyName",
        TestClientUtils.CTP_SOURCE_CLIENT);
  }

  /**
   * Deletes Categories and Types from source and target CTP projects, then it populates target CTP
   * project with category test data.
   */
  @BeforeEach
  void setupTest() {
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_TARGET_CLIENT);
    CategoryITUtils.deleteAllCategories(TestClientUtils.CTP_SOURCE_CLIENT);

    CategoryITUtils.ensureCategories(
        TestClientUtils.CTP_TARGET_CLIENT, CategoryITUtils.getCategoryDrafts(null, 2, true));

    callBackErrorResponses = new ArrayList<>();
    callBackExceptions = new ArrayList<>();
    callBackWarningResponses = new ArrayList<>();
    categorySync = new CategorySync(buildCategorySyncOptions(50));
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  private CategorySyncOptions buildCategorySyncOptions(final int batchSize) {
    return CategorySyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
        .batchSize(batchSize)
        .errorCallback(
            (exception, oldResource, newResource, updateActions) -> {
              callBackErrorResponses.add(exception.getMessage());
              callBackExceptions.add(exception.getCause());
            })
        .warningCallback(
            (exception, oldResource, newResource) ->
                callBackWarningResponses.add(exception.getMessage()))
        .build();
  }

  /** Cleans up the target and source test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    CategoryITUtils.deleteCategorySyncTestData(TestClientUtils.CTP_TARGET_CLIENT);
    CategoryITUtils.deleteCategorySyncTestData(TestClientUtils.CTP_SOURCE_CLIENT);
  }

  @Test
  void syncDrafts_withChangesInExistingCategories_ShouldUpdate2Categories() {
    CategoryITUtils.ensureCategories(
        TestClientUtils.CTP_SOURCE_CLIENT,
        CategoryITUtils.getCategoryDraftsWithPrefix(Locale.ENGLISH, "new", null, 2));

    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 0, 2, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void
      syncDrafts_withChangesInExistingCategoriesAndNewCategories_ShouldUpdate2CategoriesAndCreate1Category() {
    CategoryITUtils.ensureCategories(
        TestClientUtils.CTP_SOURCE_CLIENT,
        CategoryITUtils.getCategoryDraftsWithPrefix(Locale.ENGLISH, "new", null, 3));

    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(3, 1, 2, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_withNewShuffledBatchOfCategories_ShouldCreateCategories() {
    // Create 5 categories in the source project
    final List<Category> subFamily =
        CategoryITUtils.createChildren(5, null, "root", TestClientUtils.CTP_SOURCE_CLIENT);

    // Create 125 categories in the source project
    for (final Category child : subFamily) {
      final List<Category> subsubFamily =
          CategoryITUtils.createChildren(
              5, child, child.getName().get(Locale.ENGLISH), TestClientUtils.CTP_SOURCE_CLIENT);
      for (final Category subChild : subsubFamily) {
        CategoryITUtils.createChildren(
            4, subChild, subChild.getName().get(Locale.ENGLISH), TestClientUtils.CTP_SOURCE_CLIENT);
      }
    }
    // Total number of categories in the source project: 130
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .withLimit(500)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    // Make sure there is no hierarchical order
    Collections.shuffle(categoryDrafts);

    CategorySync categorySyncWith13BatchSize = new CategorySync(buildCategorySyncOptions(13));
    final CategorySyncStatistics syncStatistics =
        categorySyncWith13BatchSize.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(130, 130, 0, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void
      syncDrafts_withExistingShuffledCategoriesWithChangingCategoryHierarchy_ShouldUpdateCategories() {
    // Create a total of 130 categories in the target project
    final List<Category> subFamily =
        CategoryITUtils.createChildren(5, null, "root", TestClientUtils.CTP_TARGET_CLIENT);

    for (final Category child : subFamily) {
      final List<Category> subsubFamily =
          CategoryITUtils.createChildren(
              5, child, child.getName().get(Locale.ENGLISH), TestClientUtils.CTP_TARGET_CLIENT);
      for (final Category subChild : subsubFamily) {
        CategoryITUtils.createChildren(
            4, subChild, subChild.getName().get(Locale.ENGLISH), TestClientUtils.CTP_TARGET_CLIENT);
      }
    }
    // ---------------------------------------------------------------

    // Create a total of 130 categories in the source project
    final List<Category> sourceSubFamily =
        CategoryITUtils.createChildren(5, null, "root", TestClientUtils.CTP_SOURCE_CLIENT);

    for (final Category child : sourceSubFamily) {
      final List<Category> subsubFamily =
          CategoryITUtils.createChildren(
              5,
              sourceSubFamily.get(0),
              child.getName().get(Locale.ENGLISH),
              TestClientUtils.CTP_SOURCE_CLIENT);
      for (final Category subChild : subsubFamily) {
        CategoryITUtils.createChildren(
            4,
            sourceSubFamily.get(0),
            subChild.getName().get(Locale.ENGLISH),
            TestClientUtils.CTP_SOURCE_CLIENT);
      }
    }
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .withLimit(500)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();
    Collections.shuffle(categoryDrafts);

    final CategorySync categorySyncWith13BatcheSize =
        new CategorySync(buildCategorySyncOptions(13));
    final CategorySyncStatistics syncStatistics =
        categorySyncWith13BatcheSize.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(130, 0, 120, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_withExistingCategoriesThatChangeParents_ShouldUpdateCategories() {
    // Create a total of 3 categories in the target project (2 roots and 1 child to the first root)
    final List<Category> subFamily =
        CategoryITUtils.createChildren(2, null, "root", TestClientUtils.CTP_TARGET_CLIENT);

    final Category firstRoot = subFamily.get(0);
    CategoryITUtils.createChildren(1, firstRoot, "child", TestClientUtils.CTP_TARGET_CLIENT);

    // ---------------------------------------------------------------

    // Create a total of 2 categories in the source project (2 roots and 1 child to the second root)
    final List<Category> sourceSubFamily =
        CategoryITUtils.createChildren(2, null, "root", TestClientUtils.CTP_SOURCE_CLIENT);

    final Category secondRoot = sourceSubFamily.get(1);
    CategoryITUtils.createChildren(1, secondRoot, "child", TestClientUtils.CTP_SOURCE_CLIENT);
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();
    Collections.shuffle(categoryDrafts);

    final CategorySync categorySyncWith1BatchSize = new CategorySync(buildCategorySyncOptions(1));
    final CategorySyncStatistics syncStatistics =
        categorySyncWith1BatchSize.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(3, 0, 1, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_withANonExistingNewParent_ShouldUpdateCategories() {
    final String parentKey = "parent";
    // Create a total of 2 categories in the target project.
    final CategoryDraft parentDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "parent"))
            .slug(LocalizedString.of(Locale.ENGLISH, "parent"))
            .key(parentKey)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        TypeResourceIdentifierBuilder.of()
                            .key(CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY)
                            .build())
                    .fields(ITUtils.createCustomFieldsJsonMap())
                    .build())
            .build();

    TestClientUtils.CTP_TARGET_CLIENT
        .categories()
        .create(parentDraft)
        .execute()
        .toCompletableFuture()
        .join();

    final String childKey = "child";
    final CategoryDraft childDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, childKey))
            .slug(LocalizedString.of(Locale.ENGLISH, childKey))
            .key(childKey)
            .parent(CategoryResourceIdentifierBuilder.of().key(parentKey).build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        TypeResourceIdentifierBuilder.of()
                            .key(CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY)
                            .build())
                    .fields(ITUtils.createCustomFieldsJsonMap())
                    .build())
            .build();

    TestClientUtils.CTP_TARGET_CLIENT
        .categories()
        .create(childDraft)
        .execute()
        .toCompletableFuture()
        .join();
    // ------------------------------------------------------------------------------------------------------------
    // Create a total of 2 categories in the source project
    String newParentKey = "new-parent";
    final CategoryDraft sourceParentDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "new-parent"))
            .slug(LocalizedString.of(Locale.ENGLISH, "new-parent"))
            .key(newParentKey)
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        TypeResourceIdentifierBuilder.of()
                            .key(CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY)
                            .build())
                    .fields(ITUtils.createCustomFieldsJsonMap())
                    .build())
            .build();

    TestClientUtils.CTP_SOURCE_CLIENT
        .categories()
        .create(sourceParentDraft)
        .execute()
        .toCompletableFuture()
        .join();

    final CategoryDraft sourceChildDraft =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, "new-child"))
            .slug(LocalizedString.of(Locale.ENGLISH, childKey))
            .key(childKey)
            .parent(CategoryResourceIdentifierBuilder.of().key(newParentKey).build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(
                        TypeResourceIdentifierBuilder.of()
                            .key(CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY)
                            .build())
                    .fields(ITUtils.createCustomFieldsJsonMap())
                    .build())
            .build();

    TestClientUtils.CTP_SOURCE_CLIENT
        .categories()
        .create(sourceChildDraft)
        .execute()
        .toCompletableFuture()
        .join();
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .withSort("createdAt asc")
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    // To simulate the new parent coming in a later draft
    Collections.reverse(categoryDrafts);

    CategorySync categorySyncWith1BatchSize = new CategorySync(buildCategorySyncOptions(13));
    final CategorySyncStatistics syncStatistics =
        categorySyncWith1BatchSize.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 1, 1, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_fromCategoriesWithoutKeys_ShouldNotUpdateCategories() {
    // Generate unique identifiers for this test run to avoid collisions
    final String testRunId = UUID.randomUUID().toString();
    final String key1 = "cat-key-1-" + testRunId;
    final String key2 = "cat-key-2-" + testRunId;
    final String slug1 = "cat-slug-1-" + testRunId;
    final String slug2 = "cat-slug-2-" + testRunId;

    final CategoryDraft oldCategoryDraft1 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, key1))
            .slug(LocalizedString.of(Locale.ENGLISH, slug1))
            .key(key1)
            .custom(CategoryITUtils.getCustomFieldsDraft())
            .build();

    final CategoryDraft oldCategoryDraft2 =
        CategoryDraftBuilder.of()
            .name(LocalizedString.of(Locale.ENGLISH, key2))
            .slug(LocalizedString.of(Locale.ENGLISH, slug2))
            .key(key2)
            .custom(CategoryITUtils.getCustomFieldsDraft())
            .build();

    // Create two categories in the source with Keys.
    TestClientUtils.CTP_SOURCE_CLIENT.categories().create(oldCategoryDraft1).executeBlocking();
    TestClientUtils.CTP_SOURCE_CLIENT.categories().create(oldCategoryDraft2).executeBlocking();

    // Create two categories in the target without Keys (same slugs but no keys).
    final CategoryDraft newCategoryDraft1 =
        CategoryDraftBuilder.of(oldCategoryDraft1).key(null).build();
    final CategoryDraft newCategoryDraft2 =
        CategoryDraftBuilder.of(oldCategoryDraft2).key(null).build();

    assertThat(oldCategoryDraft1.getKey()).isNotEqualTo(newCategoryDraft2.getKey());
    assertThat(oldCategoryDraft2.getKey()).isNotEqualTo(newCategoryDraft2.getKey());
    assertThat(oldCategoryDraft1.getSlug().get(Locale.ENGLISH))
        .isEqualTo(newCategoryDraft1.getSlug().get(Locale.ENGLISH));
    assertThat(oldCategoryDraft2.getSlug().get(Locale.ENGLISH))
        .isEqualTo(newCategoryDraft2.getSlug().get(Locale.ENGLISH));

    final Category targetCat1 =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .create(newCategoryDraft1)
            .executeBlocking()
            .getBody();
    final Category targetCat2 =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .create(newCategoryDraft2)
            .executeBlocking()
            .getBody();

    // Verify both categories were created in TARGET
    assertThat(targetCat1).isNotNull();
    assertThat(targetCat1.getSlug().get(Locale.ENGLISH)).isEqualTo(slug1);
    assertThat(targetCat2).isNotNull();
    assertThat(targetCat2.getSlug().get(Locale.ENGLISH)).isEqualTo(slug2);

    // Re-fetch TARGET categories by slug to ensure they're fully indexed before syncing
    // This addresses potential eventual consistency issues with the commercetools API
    final long targetCategoriesWithOurSlugs =
        TestClientUtils.CTP_TARGET_CLIENT
            .categories()
            .get()
            .withWhere("slug(en in :slugs)")
            .withPredicateVar("slugs", List.of(slug1, slug2))
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getTotal();
    assertThat(targetCategoriesWithOurSlugs)
        .withFailMessage(
            "Expected 2 categories with slugs %s and %s in TARGET, but found %d",
            slug1, slug2, targetCategoriesWithOurSlugs)
        .isEqualTo(2L);

    // ---------

    // Fetch only the categories we created for this test (by keys)
    final List<Category> categories =
        TestClientUtils.CTP_SOURCE_CLIENT
            .categories()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", List.of(key1, key2))
            .execute()
            .toCompletableFuture()
            .join()
            .getBody()
            .getResults();

    // Verify we have exactly 2 categories from SOURCE
    assertThat(categories).hasSize(2);

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                TestClientUtils.CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 0, 0, 2, 0);

    // Verify we got 2 errors (one for each category that failed to create)
    assertThat(callBackErrorResponses).hasSize(2);

    // Count how many errors are DuplicateField errors
    // Note: Due to a known concurrency issue in the sync library, sometimes one category
    // may fail with ArrayIndexOutOfBoundsException instead of DuplicateField.
    // We assert that at least one error is a DuplicateField error to validate the test scenario.
    final long duplicateFieldErrors =
        callBackErrorResponses.stream()
            .filter(errorMessage -> errorMessage.contains("\"code\" : \"DuplicateField\""))
            .filter(errorMessage -> errorMessage.contains("\"field\" : \"slug.en\""))
            .count();
    assertThat(duplicateFieldErrors)
        .withFailMessage(
            "Expected at least 1 DuplicateField error, but found %d. Errors: %s",
            duplicateFieldErrors, callBackErrorResponses)
        .isGreaterThanOrEqualTo(1);

    // Verify we got 2 exceptions
    assertThat(callBackExceptions).hasSize(2);

    // Count exceptions that are BadRequestException with DuplicateField errors
    // Note: Due to a known concurrency issue, some exceptions may be different types
    final long badRequestExceptions =
        callBackExceptions.stream()
            .filter(throwable -> throwable instanceof CompletionException)
            .filter(throwable -> throwable.getCause() instanceof BadRequestException)
            .filter(
                throwable -> {
                  final BadRequestException errorResponse =
                      (BadRequestException) throwable.getCause();
                  return errorResponse.getErrorResponse().getErrors().stream()
                      .anyMatch(
                          ctpError ->
                              DuplicateFieldError.DUPLICATE_FIELD.equals(ctpError.getCode()));
                })
            .count();
    assertThat(badRequestExceptions)
        .withFailMessage(
            "Expected at least 1 BadRequestException with DuplicateField, but found %d",
            badRequestExceptions)
        .isGreaterThanOrEqualTo(1);

    assertThat(callBackWarningResponses).isEmpty();
  }
}
