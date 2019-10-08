package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnresolvedReferencesServiceImplTest {

    private UnresolvedReferencesServiceImpl service;
    private ProductSyncOptions productSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                .errorCallback((errorMessage, errorException) -> {
                    errorMessages.add(errorMessage);
                    errorExceptions.add(errorException);
                })
                .build();
        service = new UnresolvedReferencesServiceImpl(productSyncOptions);
    }

    @Test
    void save_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        // preparation
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getContainer()).thenReturn("commercetools-sync-java.UnresolvedReferencesService.productDrafts");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("product-draft-key");
        final Set<String> missingRefs = new HashSet<>();
        missingRefs.add("test-ref");
        WaitingToBeResolved valueObj = new WaitingToBeResolved(productDraft, missingRefs);
        when(mock.getValue()).thenReturn(valueObj);

        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        // test
        final Optional<WaitingToBeResolved> toBeResolvedOptional = service.save(valueObj).toCompletableFuture().join();

        // assertions
        assertThat(toBeResolvedOptional).isNotEmpty();
        assertThat(toBeResolvedOptional).containsSame(valueObj);
        final CustomObjectDraft<WaitingToBeResolved> customObjectDraft = CustomObjectDraft
                .ofUnversionedUpsert(
                        "commercetools-sync-java.UnresolvedReferencesService.productDrafts",
                        valueObj.getProductDraft().getKey(),
                        valueObj,
                        WaitingToBeResolved.class);
        verify(productSyncOptions.getCtpClient()).execute(eq(CustomObjectUpsertCommand.of(customObjectDraft)));
    }

    @Test
    void save_WithUnsuccessfulMockCtpResponse_ShouldNotSaveMock() {
        // preparation
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getContainer()).thenReturn("commercetools-sync-java.UnresolvedReferencesService.productDrafts");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("product-draft-key");
        final Set<String> missingRefs = new HashSet<>();
        missingRefs.add("test-ref");
        WaitingToBeResolved valueObj = new WaitingToBeResolved(productDraft, missingRefs);
        when(mock.getValue()).thenReturn(valueObj);

        when(productSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        // test
        final Optional<WaitingToBeResolved> toBeResolvedOptional = service.save(valueObj).toCompletableFuture().join();

        // assertions
        assertThat(toBeResolvedOptional).isEmpty();
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message -> {
                    assertThat(message)
                            .contains("Failed to save CustomObject with key: 'product-draft-key'.");
                    assertThat(message).contains("BadRequestException");
                });
        assertThat(errorExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(exception ->
                        assertThat(exception).isExactlyInstanceOf(BadRequestException.class));
    }

    @Test
    void fetch_WithEmptyKeySet_ShouldReturnEmptySet() {
        // preparation
        Set<String> keys = new HashSet<>();

        // test
        final Set<WaitingToBeResolved> beResolvedOptional = service
                .fetch(keys).toCompletableFuture().join();

        // assertions
        assertThat(beResolvedOptional).isEmpty();
    }


    @Test
    void fetch_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        // preparation
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getContainer()).thenReturn("commercetools-sync-java.UnresolvedReferencesService.productDrafts");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("product-draft-key");
        final Set<String> missingRefs = new HashSet<>();
        missingRefs.add("test-ref");
        WaitingToBeResolved valueObj = new WaitingToBeResolved(productDraft, missingRefs);
        when(mock.getValue()).thenReturn(valueObj);

        Set<String> keys = new HashSet<>();
        keys.add("product-draft-key");

        PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getCount()).thenReturn(1L);
        when(pagedQueryResult.getOffset()).thenReturn(0L);
        when(pagedQueryResult.getResults()).thenReturn(Collections.singletonList(mock));

        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(pagedQueryResult));

        // test
        final Set<WaitingToBeResolved> toBeResolvedOptional = service.fetch(keys).toCompletableFuture().join();

        // assertions
        assertThat(toBeResolvedOptional).isNotEmpty();
        assertThat(toBeResolvedOptional).containsOnly(valueObj);
    }

    @Test
    void delete_WithUnsuccessfulMockCtpResponse_ShouldReturnProperException() {
        // preparation
        final CustomObject mock = mock(CustomObject.class);
        when(mock.getContainer()).thenReturn("commercetools-sync-java.UnresolvedReferencesService.productDrafts");

        final ProductDraft productDraft = mock(ProductDraft.class);
        when(productDraft.getKey()).thenReturn("product-draft-key");
        final WaitingToBeResolved waitingDraft = new WaitingToBeResolved(productDraft, singleton("test-ref"));
        when(mock.getValue()).thenReturn(waitingDraft);

        when(productSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        // test
        final Optional<WaitingToBeResolved> toBeResolvedOptional = service.delete("product-draft-key")
                .toCompletableFuture().join();

        // assertions
        assertThat(toBeResolvedOptional).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message -> {
                    assertThat(message)
                            .contains("Failed to delete CustomObject with key: 'product-draft-key'.");
                    assertThat(message).contains("BadRequestException");
                });
        assertThat(errorExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(exception ->
                        assertThat(exception).isExactlyInstanceOf(BadRequestException.class));
    }
}
