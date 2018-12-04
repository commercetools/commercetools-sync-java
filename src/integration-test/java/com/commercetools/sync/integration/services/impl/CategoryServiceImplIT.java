package com.commercetools.sync.integration.services.impl;


import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
import io.sphere.sdk.categories.commands.updateactions.ChangeName;
import io.sphere.sdk.categories.commands.updateactions.ChangeSlug;
import io.sphere.sdk.categories.queries.CategoryQuery;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCustomFieldsDraft;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CategoryServiceImplIT {
    private CategoryService categoryService;
    private Category oldCategory;
    private static final String oldCategoryKey = "oldCategoryKey";

    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    /**
     * Delete all categories and types from target project. Then create custom types for target CTP project categories.
     */
    @BeforeClass
    public static void setup() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH, "anyName", CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Categories and Types from target CTP projects, then it populates target CTP project with category test
     * data.
     */
    @Before
    public void setupTest() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        deleteAllCategories(CTP_TARGET_CLIENT);

        final CategorySyncOptions categorySyncOptions = CategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                                  .errorCallback(
                                                                                      (errorMessage, exception) -> {
                                                                                          errorCallBackMessages
                                                                                              .add(errorMessage);
                                                                                          errorCallBackExceptions
                                                                                              .add(exception);
                                                                                      })
                                                                                  .warningCallback(
                                                                                      warningMessage ->
                                                                                          warningCallBackMessages
                                                                                              .add(warningMessage))
                                                                                  .build();

        // Create a mock new category in the target project.

        final CategoryDraft oldCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(oldCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();
        oldCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(oldCategoryDraft))
                                       .toCompletableFuture()
                                       .join();

        categoryService = new CategoryServiceImpl(categorySyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void cacheKeysToIds_ShouldCacheCategoryKeysOnlyFirstTime() {
        Map<String, String> cache = categoryService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(1);

        // Create new category without caching
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .build();

        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        cache = categoryService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void cacheKeysToIds_WithTargetCategoriesWithNoKeys_ShouldGiveAWarningAboutKeyNotSetAndNotCacheKey() {
        // Create new category without key
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .build();

        final Category createdCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft))
                                                          .toCompletableFuture().join();

        final Map<String, String> cache = categoryService.cacheKeysToIds().toCompletableFuture().join();
        assertThat(cache).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).hasSize(1);
        assertThat(warningCallBackMessages.get(0)).isEqualTo(format("Category with id: '%s' has no key set. Keys are"
            + " required for category matching.", createdCategory.getId()));
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
        final Set<Category> fetchedCategories = categoryService.fetchMatchingCategoriesByKeys(Collections.emptySet())
                                                               .toCompletableFuture().join();
        assertThat(fetchedCategories).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithAllExistingSetOfKeys_ShouldReturnSetOfCategories() {
        final Set<String> keys =  new HashSet<>();
        keys.add(oldCategoryKey);
        final Set<Category> fetchedCategories = categoryService.fetchMatchingCategoriesByKeys(keys)
                                                               .toCompletableFuture().join();
        assertThat(fetchedCategories).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CategoryQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
            .thenCallRealMethod();
        final CategorySyncOptions spyOptions = CategorySyncOptionsBuilder.of(spyClient)
                                                                         .errorCallback(
                                                                             (errorMessage, exception) -> {
                                                                                 errorCallBackMessages
                                                                                     .add(errorMessage);
                                                                                 errorCallBackExceptions
                                                                                     .add(exception);
                                                                             })
                                                                         .build();
        final CategoryService spyCategoryService = new CategoryServiceImpl(spyOptions);


        final Set<String> keys =  new HashSet<>();
        keys.add(oldCategoryKey);

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyCategoryService.fetchMatchingCategoriesByKeys(keys))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(BadGatewayException.class);
    }

    @Test
    public void fetchMatchingCategoriesByKeys_WithSomeExistingSetOfKeys_ShouldReturnSetOfCategories() {
        final Set<String> keys =  new HashSet<>();
        keys.add(oldCategoryKey);
        keys.add("new-key");
        final Set<Category> fetchedCategories = categoryService.fetchMatchingCategoriesByKeys(keys)
                                                               .toCompletableFuture().join();
        assertThat(fetchedCategories).hasSize(1);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithNonExistingCategory_ShouldNotFetchACategory() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId("non-existing-category-key")
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithExistingCategory_ShouldFetchCategoryAndCache() {
        final Optional<String> categoryId = categoryService.fetchCachedCategoryId(oldCategory.getKey())
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(categoryId).isNotEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_ShouldCacheCategoryKeysOnlyFirstTime() {
        // Fetch any category to populate cache
        categoryService.fetchCachedCategoryId("anyKey").toCompletableFuture().join();

        // Create new category
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .build();

        CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(categoryDraft)).toCompletableFuture().join();

        final Optional<String> newCategoryId =
            categoryService.fetchCachedCategoryId(newCategoryKey).toCompletableFuture().join();

        assertThat(newCategoryId).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void fetchCachedCategoryId_WithBlankKey_ShouldReturnFutureContainingEmptyOptional() {
        // test
        final Optional<String> result = categoryService.fetchCachedCategoryId("").toCompletableFuture().join();

        // assertions
        assertThat(result).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    public void createCategory_WithValidCategory_ShouldCreateCategory() {
        // preparation
        final String newCategoryKey = "newCategoryKey";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final CategorySyncOptions spyOptions = CategorySyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, exception) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(exception);
            })
            .build();

        final CategoryService spyProductService = new CategoryServiceImpl(spyOptions);

        // test
        final Optional<Category> createdOptional = categoryService
            .createCategory(categoryDraft)
            .toCompletableFuture().join();


        // assertion
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();


        //assert CTP state
        final Optional<Category> queriedOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of()
                                  .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(newCategoryKey)))
            .toCompletableFuture().join().head();

        assertThat(queriedOptional)
            .hasValueSatisfying(queried -> assertThat(createdOptional)
                .hasValueSatisfying(created -> {
                    assertThat(queried.getName()).isEqualTo(created.getName());
                    assertThat(queried.getSlug()).isEqualTo(created.getSlug());
                    assertThat(queried.getCustom()).isNotNull();
                    assertThat(queried.getKey()).isEqualTo(newCategoryKey);
                }));

        // Assert that the created category is cached
        final Optional<String> productId =
            spyProductService
                .fetchCachedCategoryId(newCategoryKey)
                .toCompletableFuture().join();
        assertThat(productId).isPresent();
        verify(spyClient, times(0)).execute(any(ProductTypeQuery.class));
    }

    @Test
    public void createCategory_WithBlankKey_ShouldCreateCategory() {
        // preparation
        final String newCategoryKey = "";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "classic furniture"),
                LocalizedString.of(Locale.ENGLISH, "classic-furniture", Locale.GERMAN, "klassische-moebel"))
            .key(newCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

        // test
        final Optional<Category> createdOptional = categoryService
            .createCategory(categoryDraft)
            .toCompletableFuture().join();

        // assertion
        assertThat(createdOptional).isEmpty();
        assertThat(errorCallBackMessages)
            .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
    }

    @Test
    public void createCategory_WithDuplicateSlug_ShouldNotCreateCategory() {
        // preparation
        final String newCategoryKey = "newCat";
        final CategoryDraft categoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"),
                LocalizedString.of(Locale.ENGLISH, "furniture"))
            .key(newCategoryKey)
            .custom(getCustomFieldsDraft())
            .build();

        // test
        final Optional<Category> createdCategoryOptional = categoryService.createCategory(categoryDraft)
                                                                          .toCompletableFuture().join();
        // assertion
        assertThat(createdCategoryOptional).isEmpty();

        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .allSatisfy(exception -> {
                assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponse = ((ErrorResponseException)exception);

                final List<DuplicateFieldError> fieldErrors = errorResponse
                    .getErrors()
                    .stream()
                    .map(sphereError -> {
                        assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                        return sphereError.as(DuplicateFieldError.class);
                    })
                    .collect(toList());
                assertThat(fieldErrors).hasSize(1);
                assertThat(fieldErrors).allSatisfy(error -> {
                    assertThat(error.getField()).isEqualTo("slug.en");
                    assertThat(error.getDuplicateValue()).isEqualTo("furniture");
                });
            });

        assertThat(errorCallBackMessages)
            .hasSize(1)
            .allSatisfy(errorMessage -> {
                assertThat(errorMessage).contains("\"code\" : \"DuplicateField\"");
                assertThat(errorMessage).contains("\"field\" : \"slug.en\"");
                assertThat(errorMessage).contains("\"duplicateValue\" : \"furniture\"");
            });

        //assert CTP state
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery.of()
                                  .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(newCategoryKey)))
            .toCompletableFuture().join().head();
        assertThat(categoryOptional).isEmpty();
    }

    @Test
    public void updateCategory_WithValidChanges_ShouldUpdateCategoryCorrectly() {
        final Optional<Category> categoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(oldCategory.getKey())))
            .toCompletableFuture().join().head();

        final String newCategoryName = "This is my new name!";
        final ChangeName changeNameUpdateAction = ChangeName
            .of(LocalizedString.of(Locale.GERMAN, newCategoryName));

        final Category updatedCategory = categoryService
            .updateCategory(categoryOptional.get(), Collections.singletonList(changeNameUpdateAction))
            .toCompletableFuture().join();
        assertThat(updatedCategory).isNotNull();

        //assert CTP state
        final Optional<Category> fetchedCategoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(oldCategory.getKey())))
            .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(fetchedCategoryOptional).isNotEmpty();
        final Category fetchedCategory = fetchedCategoryOptional.get();
        assertThat(fetchedCategory.getName()).isEqualTo(updatedCategory.getName());
        assertThat(fetchedCategory.getSlug()).isEqualTo(updatedCategory.getSlug());
        assertThat(fetchedCategory.getParent()).isEqualTo(updatedCategory.getParent());
        assertThat(fetchedCategory.getCustom()).isEqualTo(updatedCategory.getCustom());
        assertThat(fetchedCategory.getKey()).isEqualTo(updatedCategory.getKey());
    }

    @Test
    public void updateCategory_WithInvalidChanges_ShouldNotUpdateCategory() {
        // Create a mock new category in the target project.
        final CategoryDraft newCategoryDraft = CategoryDraftBuilder
            .of(LocalizedString.of(Locale.ENGLISH, "furniture"), LocalizedString.of(Locale.ENGLISH, "furniture1"))
            .key("newCategory")
            .custom(getCustomFieldsDraft())
            .build();
        final Category newCategory = CTP_TARGET_CLIENT.execute(CategoryCreateCommand.of(newCategoryDraft))
                                                      .toCompletableFuture().join();


        final LocalizedString newSlug = LocalizedString.of(Locale.ENGLISH, "furniture");
        final ChangeSlug changeSlugUpdateAction = ChangeSlug.of(newSlug);

        categoryService
            .updateCategory(newCategory, Collections.singletonList(changeSlugUpdateAction))
            .exceptionally(exception -> {
                assertThat(exception).isExactlyInstanceOf(CompletionException.class);
                assertThat(exception.getCause()).isExactlyInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponse = ((ErrorResponseException) exception.getCause());

                final List<DuplicateFieldError> fieldErrors = errorResponse
                    .getErrors()
                    .stream()
                    .map(sphereError -> {
                        assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                        return sphereError.as(DuplicateFieldError.class);
                    })
                    .collect(toList());
                assertThat(fieldErrors).hasSize(1);
                assertThat(fieldErrors).allSatisfy(error -> {
                    assertThat(error.getField()).isEqualTo("slug.en");
                    assertThat(error.getDuplicateValue()).isEqualTo("furniture");
                });
                return null;
            })
            .toCompletableFuture().join();


        //assert CTP state
        final Optional<Category> fetchedCategoryOptional = CTP_TARGET_CLIENT
            .execute(CategoryQuery
                .of()
                .withPredicates(categoryQueryModel -> categoryQueryModel.key().is(newCategory.getKey())))
            .toCompletableFuture().join().head();

        assertThat(fetchedCategoryOptional).isNotEmpty();
        final Category fetchedCategory = fetchedCategoryOptional.get();
        assertThat(fetchedCategory.getSlug()).isNotEqualTo(newSlug);
    }

    @Test
    public void fetchCategory_WithExistingCategoryKey_ShouldFetchCategory() {
        final Optional<Category> fetchedCategoryOptional =
            executeBlocking(categoryService.fetchCategory(oldCategoryKey));
        assertThat(fetchedCategoryOptional).contains(oldCategory);
    }

    @Test
    public void fetchCategory_WithBlankKey_ShouldNotFetchCategory() {
        final Optional<Category> fetchedCategoryOptional =
            executeBlocking(categoryService.fetchCategory(StringUtils.EMPTY));
        assertThat(fetchedCategoryOptional).isEmpty();
    }

    @Test
    public void fetchCategory_WithNullKey_ShouldNotFetchCategory() {
        final Optional<Category> fetchedCategoryOptional =
            executeBlocking(categoryService.fetchCategory(null));
        assertThat(fetchedCategoryOptional).isEmpty();
    }

    @Test
    public void fetchCategory_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CategoryQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
            .thenCallRealMethod();
        final CategorySyncOptions spyOptions = CategorySyncOptionsBuilder.of(spyClient)
                                                                         .errorCallback(
                                                                             (errorMessage, exception) -> {
                                                                                 errorCallBackMessages
                                                                                     .add(errorMessage);
                                                                                 errorCallBackExceptions
                                                                                     .add(exception);
                                                                             })
                                                                         .build();
        final CategoryService spyCategoryService = new CategoryServiceImpl(spyOptions);

        // test and assertion
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyCategoryService.fetchCategory(oldCategoryKey))
            .hasFailedWithThrowableThat()
            .isExactlyInstanceOf(BadGatewayException.class);
    }
}