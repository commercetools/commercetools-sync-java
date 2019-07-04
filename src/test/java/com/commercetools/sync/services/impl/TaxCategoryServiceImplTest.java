package com.commercetools.sync.services.impl;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.services.impl.ServiceImplUtils.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        taxCategoryId = randomString();
        taxCategoryName = randomString();
        taxCategoryKey = randomString();

        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder.of(client)
            .errorCallback((errorMessage, errorException) -> {
                errorMessages.add(errorMessage);
                errorExceptions.add(errorException);
            })
            .build();
        service = new TaxCategoryServiceImpl(taxCategorySyncOptions);
    }

    @AfterEach
    void cleanup() {
        reset(client);
    }

    private interface TaxCategoryPagedQueryResult extends PagedQueryResult<TaxCategory> {
    }

    @Test
    void fetchCachedTaxCategoryId_WithKey_ShouldFetchTaxCategory() {
        final String key = randomString();
        final String id = randomString();

        TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getKey()).thenReturn(key);

        TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        Optional<String> fetchedId = service.fetchCachedTaxCategoryId(key).toCompletableFuture().join();

        assertThat(fetchedId).isNotEmpty();
        assertThat(fetchedId.get()).isEqualTo(id);
    }

    @Test
    void fetchMatchingTaxCategoriesByKeys_WithKeySet_ShouldFetchTaxCategories() {
        String key1 = randomString();
        String key2 = randomString();

        HashSet<String> taxCategoryKeys = new HashSet<>();
        taxCategoryKeys.add(key1);
        taxCategoryKeys.add(key2);

        TaxCategory mock1 = mock(TaxCategory.class);
        when(mock1.getId()).thenReturn(randomString());
        when(mock1.getKey()).thenReturn(key1);

        TaxCategory mock2 = mock(TaxCategory.class);
        when(mock2.getId()).thenReturn(randomString());
        when(mock2.getKey()).thenReturn(key2);

        TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
        when(result.getResults()).thenReturn(Arrays.asList(mock1, mock2));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        Set<TaxCategory> taxCategories = service.fetchMatchingTaxCategoriesByKeys(taxCategoryKeys)
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategories).as("Should return resources").isNotEmpty(),
            () -> assertThat(taxCategories).as("Should return prepared resources").contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache).as("Should cache resources ids")
                .containsKeys(key1, key2)
        );
        verify(client).execute(any(TaxCategoryQuery.class));
    }

    @Test
    void fetchTaxCategory_WithKey_ShouldFetchTaxCategory() {
        TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(taxCategoryId);
        when(mock.getKey()).thenReturn(taxCategoryKey);
        TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(result));

        Optional<TaxCategory> taxCategoryOptional = service.fetchTaxCategory(taxCategoryKey)
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).as("Should return resource").isNotEmpty(),
            () -> assertThat(taxCategoryOptional).as("Should return prepared resource").containsSame(mock),
            () -> assertThat(service.keyToIdCache.get(taxCategoryKey)).as("Resource's id should be cached")
                .isEqualTo(taxCategoryId)
        );
        verify(client).execute(any(TaxCategoryQuery.class));
    }

    @Test
    void createTaxCategory_WithDraft_ShouldCreateTaxCategory() {
        TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(taxCategoryId);
        when(mock.getKey()).thenReturn(taxCategoryKey);

        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));

        TaxCategoryDraft draft = TaxCategoryDraftBuilder.of(taxCategoryName, Collections.emptyList(), "description")
            .key(taxCategoryKey)
            .build();
        Optional<TaxCategory> taxCategoryOptional = service.createTaxCategory(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).as("Resource should be created").isNotEmpty(),
            () -> assertThat(taxCategoryOptional).containsSame(mock)
        );
        verify(client).execute(eq(TaxCategoryCreateCommand.of(draft)));
    }

    @Test
    void createTaxCategory_WithRequestException_ShouldNotCreateTaxCategory() {
        TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(taxCategoryId);

        when(client.execute(any())).thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        TaxCategoryDraft draft = mock(TaxCategoryDraft.class);
        when(draft.getKey()).thenReturn(taxCategoryKey);

        Optional<TaxCategory> taxCategoryOptional = service.createTaxCategory(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).as("Result should be empty").isEmpty(),
            () -> assertThat(errorMessages).as("There should be 1 error message").hasSize(1),
            () -> assertThat(errorMessages).hasOnlyOneElementSatisfying(message -> {
                assertThat(message).as("There should be proper error message")
                    .contains("Failed to create draft with key: '" + taxCategoryKey + "'.");
                assertThat(message).as("Exception message should contain type of exception")
                    .contains("BadRequestException");
            }),
            () -> assertThat(errorExceptions).as("There should be 1 error message").hasSize(1),
            () -> assertThat(errorExceptions).hasOnlyOneElementSatisfying(exception ->
                assertThat(exception).as("Exception should be instance of 'BadRequestException'")
                    .isExactlyInstanceOf(BadRequestException.class))
        );
    }

    @Test
    void createTaxCategory_WithDraftHasNoKey_ShouldNotCreateTaxCategory() {
        TaxCategoryDraft draft = mock(TaxCategoryDraft.class);

        Optional<TaxCategory> taxCategoryOptional = service.createTaxCategory(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).as("Result should be empty").isEmpty(),
            () -> assertThat(errorMessages).as("There should be 1 error message").hasSize(1),
            () -> assertThat(errorExceptions).as("There should be 1 exception").hasSize(1),
            () -> assertThat(errorMessages.get(0))
                .as("Error message should contain proper description")
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!")
        );
    }

    @Test
    void updateTaxCategory_WithNoError_ShouldUpdateTaxCategory() {
        TaxCategory mock = mock(TaxCategory.class);
        when(client.execute(any())).thenReturn(CompletableFuture.completedFuture(mock));
        List<UpdateAction<TaxCategory>> updateActions = Collections.singletonList(ChangeName.of("name"));

        TaxCategory taxCategory = service.updateTaxCategory(mock, updateActions).toCompletableFuture().join();

        assertThat(taxCategory).isSameAs(mock);
        verify(client).execute(eq(TaxCategoryUpdateCommand.of(mock, updateActions)));
    }

}
