package com.commercetools.sync.services.impl;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.TaxCategoryCreateCommand;
import io.sphere.sdk.taxcategories.commands.TaxCategoryUpdateCommand;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import io.sphere.sdk.taxcategories.queries.TaxCategoryQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategoryServiceImplTest {

  private SphereClient client = mock(SphereClient.class);
  private TaxCategoryServiceImpl service;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  private String taxCategoryId;
  private String taxCategoryName;
  private String taxCategoryKey;

  @BeforeEach
  void setup() {
    taxCategoryId = RandomStringUtils.random(15);
    taxCategoryName = RandomStringUtils.random(15);
    taxCategoryKey = RandomStringUtils.random(15);

    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(client)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception.getCause());
                })
            .build();
    service = new TaxCategoryServiceImpl(taxCategorySyncOptions);
  }

  @AfterEach
  void cleanup() {
    reset(client);
  }

  private interface TaxCategoryPagedQueryResult extends PagedQueryResult<TaxCategory> {}

  @Test
  void fetchCachedTaxCategoryId_WithKey_ShouldFetchTaxCategory() {
    final String key = RandomStringUtils.random(15);
    final String id = RandomStringUtils.random(15);

    final TaxCategory mock = mock(TaxCategory.class);
    when(mock.getId()).thenReturn(id);
    when(mock.getKey()).thenReturn(key);

    final TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
    when(result.getResults()).thenReturn(Collections.singletonList(mock));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    final Optional<String> fetchedId =
        service.fetchCachedTaxCategoryId(key).toCompletableFuture().join();

    assertThat(fetchedId).contains(id);
  }

  @Test
  void fetchMatchingTaxCategoriesByKeys_WithKeySet_ShouldFetchTaxCategories() {
    final String key1 = RandomStringUtils.random(15);
    final String key2 = RandomStringUtils.random(15);

    final HashSet<String> taxCategoryKeys = new HashSet<>();
    taxCategoryKeys.add(key1);
    taxCategoryKeys.add(key2);

    final TaxCategory mock1 = mock(TaxCategory.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);

    final TaxCategory mock2 = mock(TaxCategory.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);

    final TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
    when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    final Set<TaxCategory> taxCategories =
        service.fetchMatchingTaxCategoriesByKeys(taxCategoryKeys).toCompletableFuture().join();

    assertAll(
        () -> assertThat(taxCategories).contains(mock1, mock2),
        () -> assertThat(service.keyToIdCache.asMap()).containsKeys(key1, key2));
    verify(client).execute(any(TaxCategoryQuery.class));
  }

  @Test
  void fetchTaxCategory_WithKey_ShouldFetchTaxCategory() {
    final TaxCategory mock = mock(TaxCategory.class);
    when(mock.getId()).thenReturn(taxCategoryId);
    when(mock.getKey()).thenReturn(taxCategoryKey);
    final TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
    when(result.head()).thenReturn(Optional.of(mock));

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

    final Optional<TaxCategory> taxCategoryOptional =
        service.fetchTaxCategory(taxCategoryKey).toCompletableFuture().join();

    assertAll(
        () -> assertThat(taxCategoryOptional).containsSame(mock),
        () ->
            assertThat(service.keyToIdCache.asMap().get(taxCategoryKey)).isEqualTo(taxCategoryId));
    verify(client).execute(any(TaxCategoryQuery.class));
  }

  @Test
  void createTaxCategory_WithDraft_ShouldCreateTaxCategory() {
    final TaxCategory mock = mock(TaxCategory.class);
    when(mock.getId()).thenReturn(taxCategoryId);
    when(mock.getKey()).thenReturn(taxCategoryKey);

    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));

    final TaxCategoryDraft draft =
        TaxCategoryDraftBuilder.of(taxCategoryName, Collections.emptyList(), "description")
            .key(taxCategoryKey)
            .build();
    final Optional<TaxCategory> taxCategoryOptional =
        service.createTaxCategory(draft).toCompletableFuture().join();

    assertThat(taxCategoryOptional).containsSame(mock);
    verify(client).execute(eq(TaxCategoryCreateCommand.of(draft)));
  }

  @Test
  void createTaxCategory_WithRequestException_ShouldNotCreateTaxCategory() {
    final TaxCategory mock = mock(TaxCategory.class);
    when(mock.getId()).thenReturn(taxCategoryId);

    when(client.execute(any()))
        .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

    final TaxCategoryDraft draft = mock(TaxCategoryDraft.class);
    when(draft.getKey()).thenReturn(taxCategoryKey);

    final Optional<TaxCategory> taxCategoryOptional =
        service.createTaxCategory(draft).toCompletableFuture().join();

    assertAll(
        () -> assertThat(taxCategoryOptional).isEmpty(),
        () ->
            assertThat(errorMessages)
                .singleElement(as(STRING))
                .contains("Failed to create draft with key: '" + taxCategoryKey + "'.")
                .contains("BadRequestException"),
        () ->
            assertThat(errorExceptions)
                .singleElement(as(THROWABLE))
                .isExactlyInstanceOf(BadRequestException.class));
  }

  @Test
  void createTaxCategory_WithDraftHasNoKey_ShouldNotCreateTaxCategory() {
    final TaxCategoryDraft draft = mock(TaxCategoryDraft.class);

    final Optional<TaxCategory> taxCategoryOptional =
        service.createTaxCategory(draft).toCompletableFuture().join();

    assertAll(
        () -> assertThat(taxCategoryOptional).isEmpty(),
        () -> assertThat(errorMessages).hasSize(1),
        () -> assertThat(errorExceptions).hasSize(1),
        () ->
            assertThat(errorMessages)
                .contains("Failed to create draft with key: 'null'. Reason: Draft key is blank!"));
  }

  @Test
  void updateTaxCategory_WithNoError_ShouldUpdateTaxCategory() {
    final TaxCategory mock = mock(TaxCategory.class);
    when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));
    final List<UpdateAction<TaxCategory>> updateActions =
        Collections.singletonList(ChangeName.of("name"));

    final TaxCategory taxCategory =
        service.updateTaxCategory(mock, updateActions).toCompletableFuture().join();

    assertThat(taxCategory).isSameAs(mock);
    verify(client).execute(eq(TaxCategoryUpdateCommand.of(mock, updateActions)));
  }
}
