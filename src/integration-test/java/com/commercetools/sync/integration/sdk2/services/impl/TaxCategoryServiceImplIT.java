package com.commercetools.sync.integration.sdk2.services.impl;

import static com.commercetools.sync.integration.sdk2.commons.utils.TaxCategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyTaxCategoriesGet;
import com.commercetools.api.client.ByProjectKeyTaxCategoriesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryChangeNameAction;
import com.commercetools.api.models.tax_category.TaxCategoryChangeNameActionBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryDraftBuilder;
import com.commercetools.api.models.tax_category.TaxCategorySetKeyActionBuilder;
import com.commercetools.sync.sdk2.services.TaxCategoryService;
import com.commercetools.sync.sdk2.services.impl.TaxCategoryServiceImpl;
import com.commercetools.sync.sdk2.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.sdk2.taxcategories.TaxCategorySyncOptionsBuilder;
import io.sphere.sdk.utils.CompletableFutureUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaxCategoryServiceImplIT {
  private TaxCategoryService taxCategoryService;
  private TaxCategory oldTaxCategory;
  private ArrayList<String> warnings;

  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Deletes tax categories from the target CTP projects, then it populates target CTP project with
   * test data.
   */
  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();

    deleteTaxCategories(CTP_TARGET_CLIENT);
    warnings = new ArrayList<>();
    oldTaxCategory = createTaxCategory(CTP_TARGET_CLIENT);

    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    taxCategoryService = new TaxCategoryServiceImpl(taxCategorySyncOptions);
  }

  /** Cleans up the target and source test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteTaxCategories(CTP_TARGET_CLIENT);
  }

  @Test
  void fetchCachedTaxCategoryId_WithNonExistingTaxCategory_ShouldNotFetchATaxCategory() {
    final Optional<String> taxCategoryId =
        taxCategoryService
            .fetchCachedTaxCategoryId("non-existing-key")
            .toCompletableFuture()
            .join();

    assertThat(taxCategoryId).isEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  void fetchCachedTaxCategoryId_WithExistingTaxCategory_ShouldFetchTaxCategoryAndCache() {
    final Optional<String> taxCategoryId =
        taxCategoryService
            .fetchCachedTaxCategoryId(oldTaxCategory.getKey())
            .toCompletableFuture()
            .join();
    assertThat(taxCategoryId).isNotEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  void fetchCachedTaxCategoryId_WithNullKey_ShouldReturnFutureWithEmptyOptional() {
    final Optional<String> taxCategoryId =
        taxCategoryService.fetchCachedTaxCategoryId(null).toCompletableFuture().join();

    assertThat(taxCategoryId).isEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  void fetchMatchingTaxCategoriesByKeys_WithEmptySetOfKeys_ShouldReturnEmptySet() {
    final Set<String> taxCategoryKeys = new HashSet<>();
    final Set<TaxCategory> matchingTaxCategories =
        taxCategoryService
            .fetchMatchingTaxCategoriesByKeys(taxCategoryKeys)
            .toCompletableFuture()
            .join();

    assertThat(matchingTaxCategories).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTaxCategoriesByKeys_WithNonExistingKeys_ShouldReturnEmptySet() {
    final Set<String> taxCategoryKeys = new HashSet<>();
    taxCategoryKeys.add("taxCategory_key_1");
    taxCategoryKeys.add("taxCategory_key_2");

    final Set<TaxCategory> matchingTaxCategories =
        taxCategoryService
            .fetchMatchingTaxCategoriesByKeys(taxCategoryKeys)
            .toCompletableFuture()
            .join();

    assertThat(matchingTaxCategories).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTaxCategoriesByKeys_WithAnyExistingKeys_ShouldReturnASetOfTaxCategories() {
    final Set<String> taxCategoryKeys = new HashSet<>();
    taxCategoryKeys.add(TAXCATEGORY_KEY);

    final Set<TaxCategory> matchingTaxCategories =
        taxCategoryService
            .fetchMatchingTaxCategoriesByKeys(taxCategoryKeys)
            .toCompletableFuture()
            .join();

    assertThat(matchingTaxCategories).hasSize(1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingTaxCategoriesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.taxCategories()).thenReturn(mock(ByProjectKeyTaxCategoriesRequestBuilder.class));
    final ByProjectKeyTaxCategoriesGet getMock = mock(ByProjectKeyTaxCategoriesGet.class);
    when(spyClient.taxCategories().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                new BadGatewayException(500, "", null, "", null)))
        .thenCallRealMethod();

    final TaxCategorySyncOptions spyOptions =
        TaxCategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TaxCategoryService spyTaxCategoryService = new TaxCategoryServiceImpl(spyOptions);

    final Set<String> keys = new HashSet<>();
    keys.add(TAXCATEGORY_KEY);

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyTaxCategoryService.fetchMatchingTaxCategoriesByKeys(keys))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void
      fetchMatchingTaxCategoriesByKeys_WithAllExistingSetOfKeys_ShouldCacheFetchedTaxCategoryIds() {
    final Set<TaxCategory> fetchedTaxCategories =
        taxCategoryService
            .fetchMatchingTaxCategoriesByKeys(singleton(TAXCATEGORY_KEY))
            .toCompletableFuture()
            .join();

    assertThat(fetchedTaxCategories).hasSize(1);

    final TaxCategory taxCategory =
        CTP_TARGET_CLIENT
            .taxCategories()
            .withKey(TAXCATEGORY_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(taxCategory).isNotNull();

    // Change taxCategory old_taxCategory_key on ctp
    final String newKey = "new_taxCategory_key";
    taxCategoryService
        .updateTaxCategory(
            taxCategory.get(),
            Collections.singletonList(TaxCategorySetKeyActionBuilder.of().key(newKey).build()))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedTaxCategoryId =
        taxCategoryService.fetchCachedTaxCategoryId(TAXCATEGORY_KEY).toCompletableFuture().join();

    assertThat(cachedTaxCategoryId).isNotEmpty();
    assertThat(cachedTaxCategoryId).contains(taxCategory.get().getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createTaxCategory_WithValidTaxCategory_ShouldCreateTaxCategoryAndCacheId() {
    final TaxCategoryDraft newTaxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name(TAXCATEGORY_NAME_1)
            .rates(createTaxRateDraft())
            .description(TAXCATEGORY_DESCRIPTION_1)
            .key(TAXCATEGORY_KEY_1)
            .build();

    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    final TaxCategorySyncOptions spyOptions =
        TaxCategorySyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TaxCategoryService spyTaxCategoryService = new TaxCategoryServiceImpl(spyOptions);

    // test
    final Optional<TaxCategory> createdTaxCategoryOptional =
        spyTaxCategoryService.createTaxCategory(newTaxCategoryDraft).toCompletableFuture().join();

    final TaxCategory queriedTaxCategory =
        CTP_TARGET_CLIENT
            .taxCategories()
            .withKey(TAXCATEGORY_KEY_1)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(createdTaxCategoryOptional)
        .hasValueSatisfying(
            createdTaxCategory -> {
              assertThat(createdTaxCategory.getKey()).isEqualTo(queriedTaxCategory.getKey());

              assertThat(createdTaxCategory.getDescription())
                  .isEqualTo(queriedTaxCategory.getDescription());
              assertThat(createdTaxCategory.getName()).isEqualTo(queriedTaxCategory.getName());
            });

    final ByProjectKeyTaxCategoriesRequestBuilder mock1 =
        mock(ByProjectKeyTaxCategoriesRequestBuilder.class);
    when(spyClient.taxCategories()).thenReturn(mock1);
    final ByProjectKeyTaxCategoriesGet mock2 = mock(ByProjectKeyTaxCategoriesGet.class);
    when(mock1.get()).thenReturn(mock2);
    when(mock2.withWhere(any(String.class))).thenReturn(mock2);
    when(mock2.withPredicateVar(any(String.class), any())).thenReturn(mock2);
    final CompletableFuture<ApiHttpResponse<ProductType>> mock3 = mock(CompletableFuture.class);
    final CompletableFuture<ApiHttpResponse<ProductType>> spy = spy(mock3);

    // Assert that the created taxCategory is cached
    final Optional<String> taxCategoryId =
        spyTaxCategoryService
            .fetchCachedTaxCategoryId(TAXCATEGORY_KEY_1)
            .toCompletableFuture()
            .join();
    assertThat(taxCategoryId).isPresent();

    verify(spy, times(0)).handle(any());
  }

  @Test
  void createTaxCategory_WithInvalidTaxCategory_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final TaxCategoryDraft newTaxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name(TAXCATEGORY_NAME_1)
            .rates(singletonList(createTaxRateDraft()))
            .description(TAXCATEGORY_DESCRIPTION_1)
            .key("")
            .build();

    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TaxCategoryService taxCategoryService = new TaxCategoryServiceImpl(options);

    // test
    final Optional<TaxCategory> result =
        taxCategoryService.createTaxCategory(newTaxCategoryDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
  }

  @Test
  void createTaxCategory_WithDuplicateKey_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final TaxCategoryDraft newTaxCategoryDraft =
        TaxCategoryDraftBuilder.of()
            .name(TAXCATEGORY_NAME_1)
            .rates(singletonList(createTaxRateDraft()))
            .description(TAXCATEGORY_DESCRIPTION_1)
            .key(TAXCATEGORY_KEY)
            .build();

    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final TaxCategoryService taxCategoryService = new TaxCategoryServiceImpl(options);

    // test
    final Optional<TaxCategory> result =
        taxCategoryService.createTaxCategory(newTaxCategoryDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isEmpty();
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("A duplicate value");

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .singleElement()
        .matches(
            exception -> {
              assertThat(exception).isExactlyInstanceOf(CompletionException.class);
              final CompletionException completionException = (CompletionException) exception;

              final BadRequestException badRequestException =
                  (BadRequestException) completionException.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());
              return fieldErrors.size() == 1;
            });
  }

  @Test
  void updateTaxCategory_WithValidChanges_ShouldUpdateTaxCategoryCorrectly() {
    final TaxCategory taxCategory =
        CTP_TARGET_CLIENT
            .taxCategories()
            .withKey(TAXCATEGORY_KEY)
            .get()
            .executeBlocking()
            .getBody();
    assertThat(taxCategory).isNotNull();

    final TaxCategoryChangeNameAction changeNameAction =
        TaxCategoryChangeNameActionBuilder.of().name("new_taxCategory_name").build();

    final TaxCategory updatedTaxCategory =
        taxCategoryService
            .updateTaxCategory(taxCategory, singletonList(changeNameAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedTaxCategory).isNotNull();

    final TaxCategory fetchedTaxCategory =
        CTP_TARGET_CLIENT
            .taxCategories()
            .withKey(TAXCATEGORY_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(fetchedTaxCategory.getKey()).isEqualTo(updatedTaxCategory.getKey());
    assertThat(fetchedTaxCategory.getDescription()).isEqualTo(updatedTaxCategory.getDescription());
    assertThat(fetchedTaxCategory.getName()).isEqualTo(updatedTaxCategory.getName());
  }

  @Test
  void fetchTaxCategory_WithExistingTaxCategoryKey_ShouldFetchTaxCategory() {
    final TaxCategory taxCategory =
        CTP_TARGET_CLIENT
            .taxCategories()
            .withKey(TAXCATEGORY_KEY)
            .get()
            .executeBlocking()
            .getBody();

    assertThat(taxCategory).isNotNull();

    final TaxCategory fetchedTaxCategory =
        taxCategoryService.fetchTaxCategory(TAXCATEGORY_KEY).toCompletableFuture().join().get();
    assertThat(fetchedTaxCategory).isEqualTo(taxCategory);
  }

  @Test
  void fetchTaxCategory_WithBlankKey_ShouldNotFetchTaxCategory() {
    final Optional<TaxCategory> fetchedTaxCategoryOptional =
        taxCategoryService.fetchTaxCategory(StringUtils.EMPTY).toCompletableFuture().join();
    assertThat(fetchedTaxCategoryOptional).isEmpty();
  }

  @Test
  void fetchTaxCategory_WithNullKey_ShouldNotFetchTaxCategory() {
    final Optional<TaxCategory> fetchedTaxCategoryOptional =
        taxCategoryService.fetchTaxCategory(null).toCompletableFuture().join();
    assertThat(fetchedTaxCategoryOptional).isEmpty();
  }
}
