package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.TAXCATEGORY_DESCRIPTION_1;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.TAXCATEGORY_KEY;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.TAXCATEGORY_KEY_1;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.TAXCATEGORY_NAME_1;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxRateDraft;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.impl.TaxCategoryServiceImpl;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.errors.DuplicateFieldError;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.commands.updateactions.SetKey;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(TaxCategoryQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
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

    final Optional<TaxCategory> taxCategoryOptional =
        CTP_TARGET_CLIENT
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(queryModel -> queryModel.key().is(TAXCATEGORY_KEY)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(taxCategoryOptional).isNotNull();

    // Change taxCategory old_taxCategory_key on ctp
    final String newKey = "new_taxCategory_key";
    taxCategoryService
        .updateTaxCategory(taxCategoryOptional.get(), Collections.singletonList(SetKey.of(newKey)))
        .toCompletableFuture()
        .join();

    // Fetch cached id by old key
    final Optional<String> cachedTaxCategoryId =
        taxCategoryService.fetchCachedTaxCategoryId(TAXCATEGORY_KEY).toCompletableFuture().join();

    assertThat(cachedTaxCategoryId).isNotEmpty();
    assertThat(cachedTaxCategoryId).contains(taxCategoryOptional.get().getId());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void createTaxCategory_WithValidTaxCategory_ShouldCreateTaxCategoryAndCacheId() {
    final TaxCategoryDraft newTaxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                TAXCATEGORY_NAME_1, singletonList(createTaxRateDraft()), TAXCATEGORY_DESCRIPTION_1)
            .key(TAXCATEGORY_KEY_1)
            .build();

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
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
    final Optional<TaxCategory> createdTaxCategory =
        spyTaxCategoryService.createTaxCategory(newTaxCategoryDraft).toCompletableFuture().join();

    final Optional<TaxCategory> queriedOptional =
        CTP_TARGET_CLIENT
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(
                        taxCategoryQueryModel -> taxCategoryQueryModel.key().is(TAXCATEGORY_KEY_1)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(queriedOptional)
        .hasValueSatisfying(
            queried ->
                assertThat(createdTaxCategory)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());
                          assertThat(created.getDescription()).isEqualTo(queried.getDescription());
                          assertThat(created.getName()).isEqualTo(queried.getName());
                        }));

    // Assert that the created taxCategory is cached
    final Optional<String> taxCategoryId =
        spyTaxCategoryService
            .fetchCachedTaxCategoryId(TAXCATEGORY_KEY_1)
            .toCompletableFuture()
            .join();
    assertThat(taxCategoryId).isPresent();
    verify(spyClient, times(0)).execute(any(TaxCategoryQuery.class));
  }

  @Test
  void createTaxCategory_WithInvalidTaxCategory_ShouldHaveEmptyOptionalAsAResult() {
    // preparation
    final TaxCategoryDraft newTaxCategoryDraft =
        TaxCategoryDraftBuilder.of(
                TAXCATEGORY_NAME_1, singletonList(createTaxRateDraft()), TAXCATEGORY_DESCRIPTION_1)
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
        TaxCategoryDraftBuilder.of(
                TAXCATEGORY_NAME_1, singletonList(createTaxRateDraft()), TAXCATEGORY_DESCRIPTION_1)
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
              assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) exception;

              final List<DuplicateFieldError> fieldErrors =
                  errorResponseException.getErrors().stream()
                      .map(
                          sphereError -> {
                            assertThat(sphereError.getCode()).isEqualTo(DuplicateFieldError.CODE);
                            return sphereError.as(DuplicateFieldError.class);
                          })
                      .collect(toList());
              return fieldErrors.size() == 1;
            });
  }

  @Test
  void updateTaxCategory_WithValidChanges_ShouldUpdateTaxCategoryCorrectly() {
    final Optional<TaxCategory> taxCategoryOptional =
        CTP_TARGET_CLIENT
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(
                        taxCategoryQueryModel -> taxCategoryQueryModel.key().is(TAXCATEGORY_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(taxCategoryOptional).isNotNull();

    final ChangeName changeNameUpdateAction = ChangeName.of("new_taxCategory_name");

    final TaxCategory updatedTaxCategory =
        taxCategoryService
            .updateTaxCategory(taxCategoryOptional.get(), singletonList(changeNameUpdateAction))
            .toCompletableFuture()
            .join();
    assertThat(updatedTaxCategory).isNotNull();

    final Optional<TaxCategory> updatedTaxCategoryOptional =
        CTP_TARGET_CLIENT
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(
                        taxCategoryQueryModel -> taxCategoryQueryModel.key().is(TAXCATEGORY_KEY)))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(taxCategoryOptional).isNotEmpty();
    final TaxCategory fetchedTaxCategory = updatedTaxCategoryOptional.get();
    assertThat(fetchedTaxCategory.getKey()).isEqualTo(updatedTaxCategory.getKey());
    assertThat(fetchedTaxCategory.getDescription()).isEqualTo(updatedTaxCategory.getDescription());
    assertThat(fetchedTaxCategory.getName()).isEqualTo(updatedTaxCategory.getName());
  }

  @Test
  void updateTaxCategory_WithInvalidChanges_ShouldCompleteExceptionally() {
    final Optional<TaxCategory> taxCategoryOptional =
        CTP_TARGET_CLIENT
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(
                        taxCategoryQueryModel -> taxCategoryQueryModel.key().is(TAXCATEGORY_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(taxCategoryOptional).isNotNull();

    final ChangeName changeNameUpdateAction = ChangeName.of(null);
    taxCategoryService
        .updateTaxCategory(taxCategoryOptional.get(), singletonList(changeNameUpdateAction))
        .exceptionally(
            exception -> {
              assertThat(exception).isNotNull();
              assertThat(exception.getMessage())
                  .contains("Request body does not contain valid JSON.");
              return null;
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  void fetchTaxCategory_WithExistingTaxCategoryKey_ShouldFetchTaxCategory() {
    final Optional<TaxCategory> taxCategoryOptional =
        CTP_TARGET_CLIENT
            .execute(
                TaxCategoryQuery.of()
                    .withPredicates(
                        taxCategoryQueryModel -> taxCategoryQueryModel.key().is(TAXCATEGORY_KEY)))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(taxCategoryOptional).isNotNull();

    final Optional<TaxCategory> fetchedTaxCategoryOptional =
        executeBlocking(taxCategoryService.fetchTaxCategory(TAXCATEGORY_KEY));
    assertThat(fetchedTaxCategoryOptional).isEqualTo(taxCategoryOptional);
  }

  @Test
  void fetchTaxCategory_WithBlankKey_ShouldNotFetchTaxCategory() {
    final Optional<TaxCategory> fetchedTaxCategoryOptional =
        executeBlocking(taxCategoryService.fetchTaxCategory(StringUtils.EMPTY));
    assertThat(fetchedTaxCategoryOptional).isEmpty();
  }

  @Test
  void fetchTaxCategory_WithNullKey_ShouldNotFetchTaxCategory() {
    final Optional<TaxCategory> fetchedTaxCategoryOptional =
        executeBlocking(taxCategoryService.fetchTaxCategory(null));
    assertThat(fetchedTaxCategoryOptional).isEmpty();
  }
}
