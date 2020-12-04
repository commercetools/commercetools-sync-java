package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptions;
import com.commercetools.sync.taxcategories.TaxCategorySyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import io.sphere.sdk.taxcategories.TaxCategoryDraftBuilder;
import io.sphere.sdk.taxcategories.commands.updateactions.ChangeName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class TaxCategoryServiceImplTest {

    private static final String TAX_CATEGORY_KEY = "old_tax_category_key";
    private SphereClient client = mock(SphereClient.class);
    private TaxCategoryServiceImpl service;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;
    private TaxCategorySyncOptions taxCategorySyncOptions;

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
    }

    @AfterEach
    void cleanup() {
        reset(client);
    }

    private interface TaxCategoryPagedQueryResult extends PagedQueryResult<TaxCategory> {
    }

    @Test
    void fetchCachedTaxCategoryId_WithKey_ShouldFetchTaxCategory() {
        final String key = RandomStringUtils.random(15);
        final String id = RandomStringUtils.random(15);

        final TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getKey()).thenReturn(key);

        final TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
        when(result.getResults()).thenReturn(Collections.singletonList(mock));

        final FakeClient<TaxCategoryPagedQueryResult> fakeTaxCategoryClient = new FakeClient<>(result);
        initMockService(fakeTaxCategoryClient);

        final Optional<String> fetchedId = service.fetchCachedTaxCategoryId(key).toCompletableFuture().join();

        assertThat(fetchedId).contains(id);
        assertThat(fakeTaxCategoryClient.isExecuted()).isTrue();
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

        final FakeClient<TaxCategoryPagedQueryResult> fakeTaxCategoryClient = new FakeClient<>(result);
        initMockService(fakeTaxCategoryClient);

        final Set<TaxCategory> taxCategories = service.fetchMatchingTaxCategoriesByKeys(taxCategoryKeys)
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategories).contains(mock1, mock2),
            () -> assertThat(service.keyToIdCache.asMap()).containsKeys(key1, key2)
        );
        assertThat(fakeTaxCategoryClient.isExecuted()).isTrue();
    }

    @Test
    void fetchTaxCategory_WithKey_ShouldFetchTaxCategory() {
        final TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(taxCategoryId);
        when(mock.getKey()).thenReturn(taxCategoryKey);

        final TaxCategoryPagedQueryResult result = mock(TaxCategoryPagedQueryResult.class);
        when(result.head()).thenReturn(Optional.of(mock));

        final FakeClient<TaxCategoryPagedQueryResult> fakeTaxCategoryClient = new FakeClient<>(result);
        initMockService(fakeTaxCategoryClient);

        final Optional<TaxCategory> taxCategoryOptional = service.fetchTaxCategory(taxCategoryKey)
            .toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).containsSame(mock),
            () -> assertThat(service.keyToIdCache.asMap().get(taxCategoryKey)).isEqualTo(taxCategoryId)
        );
        assertThat(fakeTaxCategoryClient.isExecuted()).isTrue();
    }

    @Test
    void createTaxCategory_WithDraft_ShouldCreateTaxCategory() {
        final TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(taxCategoryId);
        when(mock.getKey()).thenReturn(taxCategoryKey);

        final FakeClient<TaxCategory> fakeTaxCategoryClient = new FakeClient<>(mock);
        initMockService(fakeTaxCategoryClient);

        final TaxCategoryDraft draft = TaxCategoryDraftBuilder
            .of(taxCategoryName, Collections.emptyList(), "description")
            .key(taxCategoryKey)
            .build();
        final Optional<TaxCategory> taxCategoryOptional = service.createTaxCategory(draft).toCompletableFuture().join();

        assertThat(taxCategoryOptional).containsSame(mock);
        assertThat(fakeTaxCategoryClient.isExecuted()).isTrue();
    }

    @Test
    void createTaxCategory_WithRequestException_ShouldNotCreateTaxCategory() {
        final TaxCategory mock = mock(TaxCategory.class);
        when(mock.getId()).thenReturn(taxCategoryId);

        final FakeClient<Throwable> fakeStateClient = new FakeClient<>(new BadRequestException("bad request"));
        initMockService(fakeStateClient);

        final TaxCategoryDraft draft = mock(TaxCategoryDraft.class);
        when(draft.getKey()).thenReturn(taxCategoryKey);

        final Optional<TaxCategory> taxCategoryOptional = service.createTaxCategory(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).isEmpty(),
            () -> assertThat(errorMessages).singleElement().satisfies(message -> {
                assertThat(message).contains("Failed to create draft with key: '" + taxCategoryKey + "'.");
                assertThat(message).contains("BadRequestException");
            }),
            () -> assertThat(errorExceptions).singleElement().satisfies(exception ->
                assertThat(exception).isExactlyInstanceOf(BadRequestException.class))
        );
    }

    @Test
    void createTaxCategory_WithDraftHasNoKey_ShouldNotCreateTaxCategory() {
        initMockService(mock(SphereClient.class));
        final TaxCategoryDraft draft = mock(TaxCategoryDraft.class);

        final Optional<TaxCategory> taxCategoryOptional = service.createTaxCategory(draft).toCompletableFuture().join();

        assertAll(
            () -> assertThat(taxCategoryOptional).isEmpty(),
            () -> assertThat(errorMessages).hasSize(1),
            () -> assertThat(errorExceptions).hasSize(1),
            () -> assertThat(errorMessages)
                .contains("Failed to create draft with key: 'null'. Reason: Draft key is blank!")
        );
    }

    @Test
    void updateTaxCategory_WithNoError_ShouldUpdateTaxCategory() {
        final TaxCategory mock = mock(TaxCategory.class);
        final FakeClient<TaxCategory> fakeTaxCategoryClient = new FakeClient<>(mock);
        initMockService(fakeTaxCategoryClient);
        final List<UpdateAction<TaxCategory>> updateActions = Collections.singletonList(ChangeName.of("name"));

        final TaxCategory taxCategory = service.updateTaxCategory(mock, updateActions).toCompletableFuture().join();

        assertThat(taxCategory).isSameAs(mock);
        assertThat(fakeTaxCategoryClient.isExecuted()).isTrue();
    }

    @Test
    void fetchMatchingTaxCategoriesByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        // Mock sphere client to return BadGatewayException on any request.
        final FakeClient<Throwable> fakeCustomObjectClient = new FakeClient<>(new BadGatewayException());
        final List<String> errorCallBackMessages = new ArrayList<>();
        final List<Throwable> errorCallBackExceptions = new ArrayList<>();

        final TaxCategorySyncOptions spyOptions =
            TaxCategorySyncOptionsBuilder.of(fakeCustomObjectClient)
                                         .errorCallback((exception, oldResource, newResource, updateActions) -> {
                                             errorCallBackMessages.add(exception.getMessage());
                                             errorCallBackExceptions.add(exception.getCause());
                                         })
                                         .build();

        service = new TaxCategoryServiceImpl(spyOptions);

        final Set<String> keys = new HashSet<>();
        keys.add(TAX_CATEGORY_KEY);

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(service.fetchMatchingTaxCategoriesByKeys(keys))
            .failsWithin(1, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

    private void initMockService(@Nonnull final SphereClient fakeTaxCategoryClient) {
        taxCategorySyncOptions = TaxCategorySyncOptionsBuilder
            .of(fakeTaxCategoryClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                errorExceptions.add(exception.getCause());
            })
            .build();
        service = new TaxCategoryServiceImpl(taxCategorySyncOptions);
    }

}
