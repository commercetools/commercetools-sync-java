package com.commercetools.sync.integration.ctpprojectsource.categories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createChildren;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDraftsWithPrefix;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypesFromTargetAndSource;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.categories.CategorySync;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.commercetools.sync.categories.utils.CategoryTransformUtils;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteAllCategories(CTP_SOURCE_CLIENT);
    deleteTypesFromTargetAndSource();
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_TARGET_CLIENT);
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_SOURCE_CLIENT);
  }

  /**
   * Deletes Categories and Types from source and target CTP projects, then it populates target CTP
   * project with category test data.
   */
  @BeforeEach
  void setupTest() {
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteAllCategories(CTP_SOURCE_CLIENT);

    createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));

    callBackErrorResponses = new ArrayList<>();
    callBackExceptions = new ArrayList<>();
    callBackWarningResponses = new ArrayList<>();
    categorySync = new CategorySync(buildCategorySyncOptions(50));
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  private CategorySyncOptions buildCategorySyncOptions(final int batchSize) {
    return CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
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
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteAllCategories(CTP_SOURCE_CLIENT);
    deleteTypesFromTargetAndSource();
  }

  @Test
  void syncDrafts_withChangesOnly_ShouldUpdateCategories() {
    createCategories(
        CTP_SOURCE_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new", null, 2));

    final List<Category> categories =
        CTP_SOURCE_CLIENT.execute(CategoryQuery.of()).toCompletableFuture().join().getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 0, 2, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_withNewCategories_ShouldCreateCategories() {
    createCategories(
        CTP_SOURCE_CLIENT, getCategoryDraftsWithPrefix(Locale.ENGLISH, "new", null, 3));

    final List<Category> categories =
        CTP_SOURCE_CLIENT.execute(CategoryQuery.of()).toCompletableFuture().join().getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
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
    // -----------------Test Setup------------------------------------
    // Delete all categories in target project
    deleteAllCategories(CTP_TARGET_CLIENT);

    // Create a total of 130 categories in the source project
    final List<Category> subFamily = createChildren(5, null, "root", CTP_SOURCE_CLIENT);

    for (final Category child : subFamily) {
      final List<Category> subsubFamily =
          createChildren(5, child, child.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
      for (final Category subChild : subsubFamily) {
        createChildren(4, subChild, subChild.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
      }
    }
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of().withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE))
            .toCompletableFuture()
            .join()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
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
    // -----------------Test Setup------------------------------------
    // Delete all categories in target project
    deleteAllCategories(CTP_TARGET_CLIENT);

    // Create a total of 130 categories in the target project
    final List<Category> subFamily = createChildren(5, null, "root", CTP_TARGET_CLIENT);

    for (final Category child : subFamily) {
      final List<Category> subsubFamily =
          createChildren(5, child, child.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);
      for (final Category subChild : subsubFamily) {
        createChildren(4, subChild, subChild.getName().get(Locale.ENGLISH), CTP_TARGET_CLIENT);
      }
    }
    // ---------------------------------------------------------------

    // Create a total of 130 categories in the source project
    final List<Category> sourceSubFamily = createChildren(5, null, "root", CTP_SOURCE_CLIENT);

    for (final Category child : sourceSubFamily) {
      final List<Category> subsubFamily =
          createChildren(
              5, sourceSubFamily.get(0), child.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
      for (final Category subChild : subsubFamily) {
        createChildren(
            4, sourceSubFamily.get(0), subChild.getName().get(Locale.ENGLISH), CTP_SOURCE_CLIENT);
      }
    }
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of().withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE))
            .toCompletableFuture()
            .join()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();
    Collections.shuffle(categoryDrafts);

    CategorySync categorySyncWith13BatcheSize = new CategorySync(buildCategorySyncOptions(13));
    final CategorySyncStatistics syncStatistics =
        categorySyncWith13BatcheSize.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(130, 0, 120, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_withExistingCategoriesThatChangeParents_ShouldUpdateCategories() {
    // -----------------Test Setup------------------------------------
    // Delete all categories in target project
    deleteAllCategories(CTP_TARGET_CLIENT);

    // Create a total of 3 categories in the target project (2 roots and 1 child to the first root)
    final List<Category> subFamily = createChildren(2, null, "root", CTP_TARGET_CLIENT);

    final Category firstRoot = subFamily.get(0);
    createChildren(1, firstRoot, "child", CTP_TARGET_CLIENT);

    // ---------------------------------------------------------------

    // Create a total of 2 categories in the source project (2 roots and 1 child to the second root)
    final List<Category> sourceSubFamily = createChildren(2, null, "root", CTP_SOURCE_CLIENT);

    final Category secondRoot = sourceSubFamily.get(1);
    createChildren(1, secondRoot, "child", CTP_SOURCE_CLIENT);
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        CTP_SOURCE_CLIENT.execute(CategoryQuery.of()).toCompletableFuture().join().getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();
    Collections.shuffle(categoryDrafts);

    CategorySync categorySyncWith1BatchSize = new CategorySync(buildCategorySyncOptions(1));
    final CategorySyncStatistics syncStatistics =
        categorySyncWith1BatchSize.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(3, 0, 1, 0, 0);
    assertThat(callBackErrorResponses).isEmpty();
    assertThat(callBackExceptions).isEmpty();
    assertThat(callBackWarningResponses).isEmpty();
  }

  @Test
  void syncDrafts_withANonExistingNewParent_ShouldUpdateCategories() {
    // -----------------Test Setup------------------------------------
    // Delete all categories in target project
    deleteAllCategories(CTP_TARGET_CLIENT);
    String parentKey = "parent";
    // Create a total of 2 categories in the target project.
    final CategoryDraft parentDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "parent"),
                LocalizedString.of(Locale.ENGLISH, "parent"))
            .key(parentKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();

    CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(parentDraft)).toCompletableFuture().join();

    final CategoryDraft childDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "child"),
                LocalizedString.of(Locale.ENGLISH, "child"))
            .key("child")
            .parent(ResourceIdentifier.ofKey(parentKey))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();
    CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(childDraft)).toCompletableFuture().join();
    // ------------------------------------------------------------------------------------------------------------
    // Create a total of 2 categories in the source project
    String newParentKey = "new-parent";
    final CategoryDraft sourceParentDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "new-parent"),
                LocalizedString.of(Locale.ENGLISH, "new-parent"))
            .key(newParentKey)
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();
    CTP_SOURCE_CLIENT
        .execute(CategoryCreateCommand.of(sourceParentDraft))
        .toCompletableFuture()
        .join();

    final CategoryDraft sourceChildDraft =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "child-new-name"),
                LocalizedString.of(Locale.ENGLISH, "child"))
            .key("child")
            .parent(ResourceIdentifier.ofKey(newParentKey))
            .custom(
                CustomFieldsDraft.ofTypeKeyAndJson(
                    OLD_CATEGORY_CUSTOM_TYPE_KEY, createCustomFieldsJsonMap()))
            .build();
    CTP_SOURCE_CLIENT
        .execute(CategoryCreateCommand.of(sourceChildDraft))
        .toCompletableFuture()
        .join();
    // ---------------------------------------------------------------

    // Fetch categories from source project
    final List<Category> categories =
        CTP_SOURCE_CLIENT
            .execute(CategoryQuery.of().withSort(sorting -> sorting.createdAt().sort().asc()))
            .toCompletableFuture()
            .join()
            .getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
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
    final CategoryDraft oldCategoryDraft1 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat1"),
                LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .custom(getCustomFieldsDraft())
            .key("newKey1")
            .build();

    final CategoryDraft oldCategoryDraft2 =
        CategoryDraftBuilder.of(
                LocalizedString.of(Locale.ENGLISH, "cat2"),
                LocalizedString.of(Locale.ENGLISH, "furniture2"))
            .custom(getCustomFieldsDraft())
            .key("newKey2")
            .build();

    // Create two categories in the source with Keys.
    List<CompletableFuture<Category>> futureCreations = new ArrayList<>();
    futureCreations.add(
        CTP_SOURCE_CLIENT
            .execute(CategoryCreateCommand.of(oldCategoryDraft1))
            .toCompletableFuture());
    futureCreations.add(
        CTP_SOURCE_CLIENT
            .execute(CategoryCreateCommand.of(oldCategoryDraft2))
            .toCompletableFuture());
    CompletableFuture.allOf(futureCreations.toArray(new CompletableFuture[futureCreations.size()]))
        .join();

    // Create two categories in the target without Keys.
    futureCreations = new ArrayList<>();
    final CategoryDraft newCategoryDraft1 =
        CategoryDraftBuilder.of(oldCategoryDraft1).key(null).build();
    final CategoryDraft newCategoryDraft2 =
        CategoryDraftBuilder.of(oldCategoryDraft2).key(null).build();
    futureCreations.add(
        CTP_TARGET_CLIENT
            .execute(CategoryCreateCommand.of(newCategoryDraft1))
            .toCompletableFuture());
    futureCreations.add(
        CTP_TARGET_CLIENT
            .execute(CategoryCreateCommand.of(newCategoryDraft2))
            .toCompletableFuture());

    CompletableFuture.allOf(futureCreations.toArray(new CompletableFuture[futureCreations.size()]))
        .join();

    // ---------

    final List<Category> categories =
        CTP_SOURCE_CLIENT.execute(CategoryQuery.of()).toCompletableFuture().join().getResults();

    final List<CategoryDraft> categoryDrafts =
        CategoryTransformUtils.toCategoryDrafts(
                CTP_SOURCE_CLIENT, referenceIdToKeyCache, categories)
            .join();

    final CategorySyncStatistics syncStatistics =
        categorySync.sync(categoryDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 0, 0, 2, 0);

    assertThat(callBackErrorResponses)
        .hasSize(2)
        .allSatisfy(
            errorMessage -> {
              assertThat(errorMessage).contains("\"code\" : \"DuplicateField\"");
              assertThat(errorMessage).contains("\"field\" : \"slug.en\"");
            });

    assertThat(callBackExceptions)
        .hasSize(2)
        .allSatisfy(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponse = ((ErrorResponseException) exception);

              final List<DuplicateFieldError> fieldErrors =
                  errorResponse.getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                            return sphereError.as(DuplicateFieldError.class);
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(error -> assertThat(error.getField()).isEqualTo("slug.en"));
            });

    assertThat(callBackWarningResponses).isEmpty();
  }
}
