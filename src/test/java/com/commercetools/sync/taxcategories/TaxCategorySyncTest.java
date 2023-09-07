package com.commercetools.sync.taxcategories;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.*;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import com.neovisionaries.i18n.CountryCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TaxCategorySyncTest {

  private final TaxCategoryService taxCategoryService = mock(TaxCategoryService.class);

  @AfterEach
  void cleanup() {
    reset(taxCategoryService);
  }

  @Test
  void sync_WithInvalidDrafts_ShouldApplyErrorCallbackAndIncrementFailed() {
    final List<String> errors = new ArrayList<>();
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback((exception, draft, entry, actions) -> errors.add(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft withoutKeyDraft = TaxCategoryDraftBuilder.of().name("").build();

    final TaxCategorySyncStatistics result =
        sync.sync(asList(null, withoutKeyDraft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(2),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(2),
        () -> assertThat(errors).hasSize(2),
        () ->
            assertThat(errors)
                .contains(
                    "TaxCategoryDraft is null.",
                    "TaxCategoryDraft with name:  doesn't have a key. "
                        + "Please make sure all tax category drafts have keys."));
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldApplyErrorCallbackAndIncrementFailed() {
    final List<String> errors = new ArrayList<>();
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback((exception, draft, entry, actions) -> errors.add(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").key("someKey").build();

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw ExceptionUtils.createBadGatewayException();
                }));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> assertThat(errors).hasSize(1),
        () ->
            assertThat(errors)
                .contains("Failed to fetch existing tax categories with keys: '[someKey]'."));
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithErrorCreating_ShouldIncrementFailedButNotApplyErrorCallback() {
    final List<String> errors = new ArrayList<>();
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback((exception, draft, entry, actions) -> errors.add(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").key("someKey").build();

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));
    when(taxCategoryService.createTaxCategory(any())).thenReturn(completedFuture(empty()));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(1),
        () -> assertThat(errors).isEmpty());
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verify(taxCategoryService, times(1)).createTaxCategory(any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithNoError_ShouldApplyBeforeCreateCallbackAndIncrementCreated() {
    final AtomicBoolean callbackApplied = new AtomicBoolean(false);
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .beforeCreateCallback(
                (draft) -> {
                  callbackApplied.set(true);
                  return draft;
                })
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").key("someKey").build();
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));
    when(taxCategoryService.createTaxCategory(any()))
        .thenReturn(completedFuture(Optional.of(taxCategory)));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getCreated().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(0),
        () -> assertThat(callbackApplied.get()).isTrue());
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verify(taxCategoryService, times(1)).createTaxCategory(any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithErrorUpdating_ShouldApplyErrorCallbackAndIncrementFailed() {
    final List<String> errors = new ArrayList<>();
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback((exception, draft, entry, actions) -> errors.add(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").description("changed").key("someKey").build();
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategory.getKey()).thenReturn("someKey");

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
    when(taxCategoryService.updateTaxCategory(any(), any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw ExceptionUtils.createBadGatewayException();
                }));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getUpdated().get()).isEqualTo(0),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(1),
        () -> assertThat(errors).hasSize(1));
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void
      sync_WithErrorUpdatingAndTryingToRecoverWithFetchException_ShouldApplyErrorCallbackAndIncrementFailed() {
    final List<String> errors = new ArrayList<>();
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback((exception, draft, entry, actions) -> errors.add(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").description("changed").key("someKey").build();
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategory.getKey()).thenReturn("someKey");

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
    when(taxCategoryService.updateTaxCategory(any(), any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw ExceptionUtils.createConcurrentModificationException("CTP Error on update");
                }));
    when(taxCategoryService.fetchTaxCategory(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw ExceptionUtils.createBadGatewayException();
                }));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getUpdated().get()).isEqualTo(0),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(1),
        () -> assertThat(errors).hasSize(1),
        () ->
            assertThat(errors)
                .singleElement(as(STRING))
                .contains(
                    "Failed to fetch from CTP while retrying after concurrency modification."));
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
    verify(taxCategoryService, times(1)).fetchTaxCategory(any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void
      sync_WithErrorUpdatingAndTryingToRecoverWithEmptyResponse_ShouldApplyErrorCallbackAndIncrementFailed() {
    final List<String> errors = new ArrayList<>();
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback((exception, draft, entry, actions) -> errors.add(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").description("changed").key("someKey").build();
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategory.getKey()).thenReturn("someKey");

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
    when(taxCategoryService.updateTaxCategory(any(), any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw ExceptionUtils.createConcurrentModificationException("CTP Error on update");
                }));
    when(taxCategoryService.fetchTaxCategory(any())).thenReturn(completedFuture(Optional.empty()));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getUpdated().get()).isEqualTo(0),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(1),
        () -> assertThat(errors).hasSize(1),
        () ->
            assertThat(errors)
                .singleElement(as(STRING))
                .contains(
                    "Not found when attempting to fetch while retrying after concurrency modification."));
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
    verify(taxCategoryService, times(1)).fetchTaxCategory(any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithNoError_ShouldApplyBeforeUpdateCallbackAndIncrementUpdated() {
    final AtomicBoolean callbackApplied = new AtomicBoolean(false);
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .beforeUpdateCallback(
                (actions, draft, old) -> {
                  callbackApplied.set(true);
                  return actions;
                })
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").description("changed").key("someKey").build();
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategory.getId()).thenReturn("id");
    when(taxCategory.getKey()).thenReturn("someKey");

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
    when(taxCategoryService.updateTaxCategory(any(), any()))
        .thenReturn(completedFuture(taxCategory));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getUpdated().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(0),
        () -> assertThat(callbackApplied.get()).isTrue());
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithFilteredActions_ShouldApplyBeforeUpdateCallbackAndNotIncrementUpdated() {
    final AtomicBoolean callbackApplied = new AtomicBoolean(false);
    final TaxCategorySyncOptions options =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .beforeUpdateCallback(
                (actions, draft, old) -> {
                  callbackApplied.set(true);
                  return emptyList();
                })
            .build();
    final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of().name("someName").description("changed").key("someKey").build();
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategory.getId()).thenReturn("id");
    when(taxCategory.getKey()).thenReturn("someKey");

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getUpdated().get()).isEqualTo(0),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(0),
        () -> assertThat(callbackApplied.get()).isTrue());
    verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
    verifyNoMoreInteractions(taxCategoryService);
  }

  @Test
  void sync_WithDuplicatedState_ShouldNotBuildActionAndTriggerErrorCallback() {
    final String name = "DuplicatedName";
    final TaxRateDraft taxRateGermanyBerlin =
        TaxRateDraftBuilder.of()
            .name(name)
            .amount(2.0)
            .includedInPrice(false)
            .country(CountryCode.DE.getAlpha2())
            .state("BERLIN")
            .build();

    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of()
            .name(name)
            .rates(
                TaxRateDraftBuilder.of()
                    .name(name)
                    .amount(2.0)
                    .includedInPrice(false)
                    .country(CountryCode.FR.getAlpha2())
                    .state("LYON")
                    .build(),
                TaxRateDraftBuilder.of()
                    .name(name)
                    .amount(2.0)
                    .includedInPrice(false)
                    .country(CountryCode.FR.getAlpha2())
                    .state("PARIS")
                    .build(),
                taxRateGermanyBerlin,
                taxRateGermanyBerlin)
            .description("desc")
            .key("someKey")
            .build();

    final AtomicReference<String> callback = new AtomicReference<>(null);
    final TaxCategorySyncOptions syncOptions =
        TaxCategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, taxDraft, entry, actions) -> callback.set(exception.getMessage()))
            .build();
    final TaxCategorySync sync = new TaxCategorySync(syncOptions, taxCategoryService);
    final TaxCategory taxCategory = mock(TaxCategory.class);

    when(taxCategory.getId()).thenReturn("id");
    when(taxCategory.getKey()).thenReturn("someKey");

    when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
        .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
    when(taxCategoryService.updateTaxCategory(any(), any()))
        .thenReturn(completedFuture(taxCategory));

    final TaxCategorySyncStatistics result =
        sync.sync(singletonList(draft)).toCompletableFuture().join();

    assertAll(
        () -> Assertions.assertThat(result.getProcessed().get()).isEqualTo(1),
        () -> Assertions.assertThat(result.getUpdated().get()).isEqualTo(0),
        () -> Assertions.assertThat(result.getFailed().get()).isEqualTo(1),
        () ->
            assertThat(callback.get())
                .contains(
                    format(
                        "Tax rate drafts have duplicated country codes and states. Duplicated "
                            + "tax rate country code: '%s'. state : '%s'. Tax rate country codes and states are "
                            + "expected to be unique inside their tax category.",
                        CountryCode.DE, "BERLIN")));
  }
}
