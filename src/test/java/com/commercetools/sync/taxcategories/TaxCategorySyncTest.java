package com.commercetools.sync.taxcategories;

import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.taxcategories.helpers.TaxCategorySyncStatistics;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TaxCategorySyncTest {

    private final TaxCategoryService taxCategoryService = mock(TaxCategoryService.class);

    @AfterEach
    void cleanup() {
        reset(taxCategoryService);
    }

    @Test
    void sync_WithInvalidDrafts_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft withoutKeyDraft = TaxCategoryDraftBuilder.of(null, emptyList(), null).build();

        final TaxCategorySyncStatistics result = sync.sync(asList(null, withoutKeyDraft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(2),
            () -> assertThat(result.getFailed().get()).isEqualTo(2),
            () -> assertThat(errors).hasSize(2),
            () -> assertThat(errors).contains("Failed to process null tax category draft.",
                "Failed to process tax category draft without key.")
        );
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), null).key("someKey").build();

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1),
            () -> assertThat(errors).contains("Failed to fetch existing tax categories with keys: '[someKey]'.")
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithErrorCreating_ShouldIncrementFailedButNotApplyErrorCallback() {
        final List<String> errors = new ArrayList<>();
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), null).key("someKey").build();

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(taxCategoryService.createTaxCategory(any())).thenReturn(completedFuture(empty()));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).isEmpty()
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verify(taxCategoryService, times(1)).createTaxCategory(any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithNoError_ShouldApplyBeforeCreateCallbackAndIncrementCreated() {
        final AtomicBoolean callbackApplied = new AtomicBoolean(false);
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeCreateCallback((draft) -> {
                callbackApplied.set(true);
                return draft;
            })
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), null).key("someKey").build();
        final TaxCategory taxCategory = mock(TaxCategory.class);

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));
        when(taxCategoryService.createTaxCategory(any())).thenReturn(completedFuture(Optional.of(taxCategory)));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getCreated().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(0),
            () -> assertThat(callbackApplied.get()).isTrue()
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verify(taxCategoryService, times(1)).createTaxCategory(any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithErrorUpdating_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), "changed")
            .key("someKey").build();
        final TaxCategory taxCategory = mock(TaxCategory.class);

        when(taxCategory.getKey()).thenReturn("someKey");

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
        when(taxCategoryService.updateTaxCategory(any(), any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1)
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithErrorUpdatingAndTryingToRecoverWithFetchException_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), "changed")
            .key("someKey").build();
        final TaxCategory taxCategory = mock(TaxCategory.class);

        when(taxCategory.getKey()).thenReturn("someKey");

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
        when(taxCategoryService.updateTaxCategory(any(), any())).thenReturn(supplyAsync(() -> {
            throw new io.sphere.sdk.client.ConcurrentModificationException();
        }));
        when(taxCategoryService.fetchTaxCategory(any())).thenReturn(supplyAsync(() -> {
            throw new SphereException();
        }));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1),
            () -> assertThat(errors).hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                .contains("Failed to fetch from CTP while retrying after concurrency modification."))
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
        verify(taxCategoryService, times(1)).fetchTaxCategory(any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithErrorUpdatingAndTryingToRecoverWithEmptyResponse_ShouldApplyErrorCallbackAndIncrementFailed() {
        final List<String> errors = new ArrayList<>();
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((msg, error) -> errors.add(msg))
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), "changed")
            .key("someKey").build();
        final TaxCategory taxCategory = mock(TaxCategory.class);

        when(taxCategory.getKey()).thenReturn("someKey");

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
        when(taxCategoryService.updateTaxCategory(any(), any())).thenReturn(supplyAsync(() -> {
            throw new ConcurrentModificationException();
        }));
        when(taxCategoryService.fetchTaxCategory(any())).thenReturn(completedFuture(Optional.empty()));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(1),
            () -> assertThat(errors).hasSize(1),
            () -> assertThat(errors).hasOnlyOneElementSatisfying(msg -> assertThat(msg)
                .contains("Not found when attempting to fetch while retrying after concurrency modification."))
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
        verify(taxCategoryService, times(1)).fetchTaxCategory(any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithNoError_ShouldApplyBeforeUpdateCallbackAndIncrementUpdated() {
        final AtomicBoolean callbackApplied = new AtomicBoolean(false);
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeUpdateCallback((actions, draft, old) -> {
                callbackApplied.set(true);
                return actions;
            })
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), "changed")
            .key("someKey").build();
        final TaxCategory taxCategory = mock(TaxCategory.class);

        when(taxCategory.getId()).thenReturn("id");
        when(taxCategory.getKey()).thenReturn("someKey");

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));
        when(taxCategoryService.updateTaxCategory(any(), any())).thenReturn(completedFuture(taxCategory));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(1),
            () -> assertThat(result.getFailed().get()).isEqualTo(0),
            () -> assertThat(callbackApplied.get()).isTrue()
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verify(taxCategoryService, times(1)).updateTaxCategory(any(), any());
        verifyNoMoreInteractions(taxCategoryService);
    }

    @Test
    void sync_WithFilteredActions_ShouldApplyBeforeUpdateCallbackAndNotIncrementUpdated() {
        final AtomicBoolean callbackApplied = new AtomicBoolean(false);
        final TaxCategorySyncOptions options = TaxCategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .beforeUpdateCallback((actions, draft, old) -> {
                callbackApplied.set(true);
                return emptyList();
            })
            .build();
        final TaxCategorySync sync = new TaxCategorySync(options, taxCategoryService);
        final TaxCategoryDraft draft = TaxCategoryDraftBuilder.of("someName", emptyList(), "changed")
            .key("someKey").build();
        final TaxCategory taxCategory = mock(TaxCategory.class);

        when(taxCategory.getId()).thenReturn("id");
        when(taxCategory.getKey()).thenReturn("someKey");

        when(taxCategoryService.fetchMatchingTaxCategoriesByKeys(any()))
            .thenReturn(completedFuture(new HashSet<>(singletonList(taxCategory))));

        final TaxCategorySyncStatistics result = sync.sync(singletonList(draft)).toCompletableFuture().join();

        assertAll(
            () -> assertThat(result.getProcessed().get()).isEqualTo(1),
            () -> assertThat(result.getUpdated().get()).isEqualTo(0),
            () -> assertThat(result.getFailed().get()).isEqualTo(0),
            () -> assertThat(callbackApplied.get()).isTrue()
        );
        verify(taxCategoryService, times(1)).fetchMatchingTaxCategoriesByKeys(any());
        verifyNoMoreInteractions(taxCategoryService);
    }

}
