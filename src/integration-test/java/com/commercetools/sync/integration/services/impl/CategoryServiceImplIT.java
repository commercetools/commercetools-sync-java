package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyCategoriesGet;
import com.commercetools.api.client.ByProjectKeyCategoriesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyCategoriesKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyCategoriesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryChangeNameAction;
import com.commercetools.api.models.category.CategoryChangeNameActionBuilder;
import com.commercetools.api.models.category.CategoryChangeSlugAction;
import com.commercetools.api.models.category.CategoryChangeSlugActionBuilder;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryPagedQueryResponse;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.CategoryService;
import com.commercetools.sync.sdk2.services.impl.CategoryServiceImpl;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceImplIT {
  private CategoryService categoryService;
  private Category oldCategory;
  private static final String oldCategoryKey = "oldCategoryKey";

  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Delete all categories and types from target project. Then create custom types for target CTP
   * project categories.
   */
  @BeforeAll
  static void setup() {
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_TARGET_CLIENT);
  }

  /**
   * Deletes Categories and Types from target CTP projects, then it populates target CTP project
   * with category test data.
   */
  @BeforeEach
  void setupTest() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    deleteAllCategories(CTP_TARGET_CLIENT);

    final CategorySyncOptions categorySyncOptions =
        CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    // Create a mock new category in the target project.

    final CategoryDraft oldCategoryDraft =
        CategoryDraftBuilder.of()
            .name(
                LocalizedStringBuilder.of()
                    .addValue(Locale.ENGLISH.toLanguageTag(), "furniture")
                    .build())
            .slug(
                LocalizedStringBuilder.of()
                    .addValue(Locale.ENGLISH.toLanguageTag(), "furniture")
                    .build())
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
    oldCategory =
        CTP_TARGET_CLIENT
            .categories()
            .post(oldCategoryDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    categoryService = new CategoryServiceImpl(categorySyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteAllCategories(CTP_TARGET_CLIENT);
    deleteTypes(CTP_TARGET_CLIENT);
  }

  @Test
  void cacheKeysToIds_ShouldCacheCategoryKeysOnlyFirstTime() {
    Map<String, String> cache =
        categoryService
            .cacheKeysToIds(Collections.singleton(oldCategoryKey))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    // Create new category without caching
    final String newCategoryKey = "newCategoryKey";
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.ENGLISH.toLanguageTag(), "classic furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.ENGLISH.toLanguageTag(), "classic-furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.GERMAN.toLanguageTag(), "klassische-moebel"))
            .key(newCategoryKey)
            .build();
    CTP_TARGET_CLIENT.categories().create(categoryDraft).execute().toCompletableFuture().join();

    cache =
        categoryService
            .cacheKeysToIds(Collections.singleton(oldCategoryKey))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCategoriesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<Category> fetchedCategories =
        categoryService
            .fetchMatchingCategoriesByKeys(Collections.emptySet())
            .toCompletableFuture()
            .join();
    assertThat(fetchedCategories).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCategoriesByKeys_WithAllExistingSetOfKeys_ShouldReturnSetOfCategories() {
    final Set<String> keys = new HashSet<>();
    keys.add(oldCategoryKey);
    final Set<Category> fetchedCategories =
        categoryService.fetchMatchingCategoriesByKeys(keys).toCompletableFuture().join();
    assertThat(fetchedCategories).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCategoriesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.categories()).thenReturn(mock(ByProjectKeyCategoriesRequestBuilder.class));
    final ByProjectKeyCategoriesGet getMock = mock(ByProjectKeyCategoriesGet.class);
    when(spyClient.categories().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
        .thenCallRealMethod();
    final CategorySyncOptions spyOptions =
        CategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CategoryService categoryService = new CategoryServiceImpl(spyOptions);

    final Set<String> keys = new HashSet<>();
    keys.add(oldCategoryKey);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(categoryService.fetchMatchingCategoriesByKeys(keys))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void fetchMatchingCategoriesByKeys_WithSomeExistingSetOfKeys_ShouldReturnSetOfCategories() {
    final Set<String> keys = new HashSet<>();
    keys.add(oldCategoryKey);
    keys.add("new-key");
    final Set<Category> fetchedCategories =
        categoryService.fetchMatchingCategoriesByKeys(keys).toCompletableFuture().join();
    assertThat(fetchedCategories).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCachedCategoryId_WithNonExistingCategory_ShouldNotFetchACategory() {
    final Optional<String> categoryId =
        categoryService
            .fetchCachedCategoryId("non-existing-category-key")
            .toCompletableFuture()
            .join();
    assertThat(categoryId).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCachedCategoryId_WithExistingCategory_ShouldFetchCategoryAndCache() {
    final Optional<String> categoryId =
        categoryService.fetchCachedCategoryId(oldCategory.getKey()).toCompletableFuture().join();
    assertThat(categoryId).contains(oldCategory.getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCachedCategoryId_WithBlankKey_ShouldReturnFutureContainingEmptyOptional() {
    // test
    final Optional<String> result =
        categoryService.fetchCachedCategoryId("").toCompletableFuture().join();

    // assertions
    assertThat(result).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createCategory_WithValidCategory_ShouldCreateCategoryAndCacheId() {
    // preparation
    final String newCategoryKey = "newCategoryKey";
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.ENGLISH.toLanguageTag(), "classic furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.ENGLISH.toLanguageTag(), "classic-furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.GERMAN.toLanguageTag(), "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    final CategorySyncOptions spyOptions =
        CategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CategoryService spyProductService = new CategoryServiceImpl(spyOptions);

    // test
    final Category createdCategory =
        spyProductService.createCategory(categoryDraft).toCompletableFuture().join().get();

    // assertion
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    // assert CTP state
    final Category fetchedCategory =
        CTP_TARGET_CLIENT
            .categories()
            .withKey(newCategoryKey)
            .get()
            .execute()
            .thenApply(categoryApiHttpResponse -> categoryApiHttpResponse.getBody())
            .join();

    assertThat(fetchedCategory.getName()).isEqualTo(createdCategory.getName());
    assertThat(fetchedCategory.getSlug()).isEqualTo(createdCategory.getSlug());
    assertThat(fetchedCategory.getCustom()).isNotNull();
    assertThat(fetchedCategory.getKey()).isEqualTo(newCategoryKey);

    final ByProjectKeyCategoriesRequestBuilder mock1 =
        mock(ByProjectKeyCategoriesRequestBuilder.class);
    when(spyClient.categories()).thenReturn(mock1);
    final ByProjectKeyCategoriesGet mock2 = mock(ByProjectKeyCategoriesGet.class);
    when(mock1.get()).thenReturn(mock2);
    when(mock2.withWhere(any(String.class))).thenReturn(mock2);
    when(mock2.withPredicateVar(any(String.class), any())).thenReturn(mock2);
    final CompletableFuture<ApiHttpResponse<ProductType>> mock3 = mock(CompletableFuture.class);
    final CompletableFuture<ApiHttpResponse<ProductType>> spy = spy(mock3);

    // Assert that the created product type is cached
    final Optional<String> productTypeId =
        spyProductService.fetchCachedCategoryId(newCategoryKey).toCompletableFuture().join();
    assertThat(productTypeId).isPresent();
    verify(spy, times(0)).handle(any());
  }

  @Test
  void createCategory_WithBlankKey_ShouldNotCreateCategory() {
    // preparation
    final String newCategoryKey = "";
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.ENGLISH.toLanguageTag(), "classic furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.ENGLISH.toLanguageTag(), "classic-furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(
                        Locale.GERMAN.toLanguageTag(), "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    // test
    final Optional<Category> createdOptional =
        categoryService.createCategory(categoryDraft).toCompletableFuture().join();

    // assertion
    assertThat(createdOptional).isEmpty();
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
  }

  @Test
  void createCategory_WithDuplicateSlug_ShouldNotCreateCategory() {
    // preparation
    final String newCategoryKey = "newCat";
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(Locale.ENGLISH.toLanguageTag(), "furniture"))
            .slug(
                localizedStringBuilder ->
                    localizedStringBuilder.addValue(Locale.ENGLISH.toLanguageTag(), "furniture"))
            .key(newCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

    // test
    final Optional<Category> createdCategoryOptional =
        categoryService.createCategory(categoryDraft).toCompletableFuture().join();
    // assertion
    assertThat(createdCategoryOptional).isEmpty();

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .allSatisfy(
            exception -> {
              BadRequestException badRequestException = (BadRequestException) exception.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue()).isEqualTo("furniture");
                      });
            });

    assertThat(errorCallBackMessages)
        .hasSize(1)
        .allSatisfy(
            errorMessage -> {
              assertThat(errorMessage).contains("\"code\" : \"DuplicateField\"");
              assertThat(errorMessage).contains("\"field\" : \"slug.en\"");
              assertThat(errorMessage).contains("\"duplicateValue\" : \"furniture\"");
            });

    // assert CTP state

    final CategoryPagedQueryResponse response =
        CTP_TARGET_CLIENT
            .categories()
            .get()
            .withWhere("key = :key")
            .withPredicateVar("key", newCategoryKey)
            .execute()
            .toCompletableFuture()
            .thenApply(ApiHttpResponse::getBody)
            .join();
    assertThat(response.getCount()).isEqualTo(0L);
  }

  @Test
  void updateCategory_WithValidChanges_ShouldUpdateCategoryCorrectly() {
    final Category category =
        CTP_TARGET_CLIENT
            .categories()
            .withKey(oldCategory.getKey())
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final String newCategoryName = "This is my new name!";
    CategoryChangeNameAction changeNameAction =
        CategoryChangeNameActionBuilder.of()
            .name(
                LocalizedStringBuilder.of()
                    .addValue(Locale.GERMAN.toLanguageTag(), newCategoryName)
                    .build())
            .build();

    final Category updatedCategory =
        categoryService
            .updateCategory(category, Collections.singletonList(changeNameAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedCategory).isNotNull();

    // assert CTP state
    final Category fetchedCategory =
        CTP_TARGET_CLIENT
            .categories()
            .withKey(oldCategory.getKey())
            .get()
            .execute()
            .thenApply(categoryApiHttpResponse -> categoryApiHttpResponse.getBody())
            .toCompletableFuture()
            .join();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(fetchedCategory.getName()).isEqualTo(updatedCategory.getName());
    assertThat(fetchedCategory.getSlug()).isEqualTo(updatedCategory.getSlug());
    assertThat(fetchedCategory.getParent()).isEqualTo(updatedCategory.getParent());
    assertThat(fetchedCategory.getCustom()).isEqualTo(updatedCategory.getCustom());
    assertThat(fetchedCategory.getKey()).isEqualTo(updatedCategory.getKey());
  }

  @Test
  void updateCategory_WithInvalidChanges_ShouldNotUpdateCategory() {
    // Create a mock new category in the target project.
    final CategoryDraft newCategoryDraft =
        CategoryDraftBuilder.of()
            .name(
                LocalizedStringBuilder.of()
                    .addValue(Locale.ENGLISH.toLanguageTag(), "furniture")
                    .build())
            .slug(
                LocalizedStringBuilder.of()
                    .addValue(Locale.ENGLISH.toLanguageTag(), "furniture1")
                    .build())
            .key("newCategory")
            .custom(getCustomFieldsDraft())
            .build();

    final Category newCategory =
        CTP_TARGET_CLIENT
            .categories()
            .create(newCategoryDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    LocalizedString newSlug =
        LocalizedStringBuilder.of().addValue(Locale.ENGLISH.toLanguageTag(), "furniture").build();
    final CategoryChangeSlugAction changeSlugAction =
        CategoryChangeSlugActionBuilder.of().slug(newSlug).build();

    categoryService
        .updateCategory(newCategory, Collections.singletonList(changeSlugAction))
        .exceptionally(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);

              final BadRequestException badRequestException =
                  ((BadRequestException) exception.getCause());

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) sphereError;
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue()).isEqualTo("furniture");
                      });
              return null;
            })
        .toCompletableFuture()
        .join();

    // assert CTP state
    final Category fetchedCategory =
        CTP_TARGET_CLIENT
            .categories()
            .withKey(newCategory.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .thenApply(ApiHttpResponse::getBody)
            .join();

    assertThat(fetchedCategory.getSlug()).isNotEqualTo(newSlug);
  }

  @Test
  void fetchCategory_WithExistingCategoryKey_ShouldFetchCategory() {
    final Optional<Category> fetchedCategoryOptional =
        executeBlocking(categoryService.fetchCategory(oldCategoryKey));
    assertThat(fetchedCategoryOptional).contains(oldCategory);
  }

  @Test
  void fetchCategory_WithBlankKey_ShouldNotFetchCategory() {
    final Optional<Category> fetchedCategoryOptional =
        executeBlocking(categoryService.fetchCategory(StringUtils.EMPTY));
    assertThat(fetchedCategoryOptional).isEmpty();
  }

  @Test
  void fetchCategory_WithNullKey_ShouldNotFetchCategory() {
    final Optional<Category> fetchedCategoryOptional =
        executeBlocking(categoryService.fetchCategory(null));
    assertThat(fetchedCategoryOptional).isEmpty();
  }

  @Test
  void fetchCategory_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.categories()).thenReturn(mock(ByProjectKeyCategoriesRequestBuilder.class));

    final ByProjectKeyCategoriesKeyByKeyRequestBuilder builder =
        mock(ByProjectKeyCategoriesKeyByKeyRequestBuilder.class);
    when(spyClient.categories().withKey(any())).thenReturn(builder);

    final ByProjectKeyCategoriesKeyByKeyGet getMock = mock(ByProjectKeyCategoriesKeyByKeyGet.class);
    when(builder.get()).thenReturn(getMock);

    when(getMock.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
        .thenCallRealMethod();
    final CategorySyncOptions spyOptions =
        CategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CategoryService spyCategoryService = new CategoryServiceImpl(spyOptions);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyCategoryService.fetchCategory(oldCategoryKey))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }
}
