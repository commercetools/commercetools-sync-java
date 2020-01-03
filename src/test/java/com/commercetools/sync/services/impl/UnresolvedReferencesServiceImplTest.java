package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        productSyncOptions = ProductSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((errorMessage, errorException) -> {
                errorMessages.add(errorMessage);
                errorExceptions.add(errorException);
            })
            .build();
        service = new UnresolvedReferencesServiceImpl(productSyncOptions);
    }

    @Test
    void fetch_WithEmptyKeySet_ShouldReturnEmptySet() {
        // preparation
        final Set<String> keys = new HashSet<>();

        // test
        final Set<WaitingToBeResolved> result = service
            .fetch(keys)
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(result).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetch_OnSuccess_ShouldReturnMock() {
        // preparation
        final CustomObject customObjectMock = mock(CustomObject.class);
        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn("product-draft-key");

        final WaitingToBeResolved waitingToBeResolved =
            new WaitingToBeResolved(productDraftMock, singleton("test-ref"));
        when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

        final PagedQueryResult result = getMockPagedQueryResult(singletonList(customObjectMock));
        when(productSyncOptions.getCtpClient().execute(any(CustomObjectQuery.class)))
            .thenReturn(completedFuture(result));

        // test
        final Set<WaitingToBeResolved> toBeResolvedOptional = service
            .fetch(singleton("product-draft-key"))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(toBeResolvedOptional).containsOnly(waitingToBeResolved);
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetch_OnSuccess_ShouldRequestHashedKeys() {
        // preparation
        final CustomObject customObjectMock = mock(CustomObject.class);
        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn("product-draft-key");

        final WaitingToBeResolved waitingToBeResolved =
            new WaitingToBeResolved(productDraftMock, singleton("test-ref"));
        when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

        final PagedQueryResult result = getMockPagedQueryResult(singletonList(customObjectMock));
        when(productSyncOptions.getCtpClient().execute(any(CustomObjectQuery.class)))
            .thenReturn(completedFuture(result));
        final ArgumentCaptor<CustomObjectQuery<WaitingToBeResolved>> requestArgumentCaptor =
            ArgumentCaptor.forClass(CustomObjectQuery.class);

        // test
        final Set<String> setOfSpecialCharKeys = asSet(
            "Get a $100 Visa® Reward Card because you’re ordering TV",
            "product$",
            "Visa®",
            "Visa©");
        final Set<WaitingToBeResolved> toBeResolvedOptional = service
            .fetch(setOfSpecialCharKeys)
            .toCompletableFuture()
            .join();

        // assertions
        verify(productSyncOptions.getCtpClient()).execute(requestArgumentCaptor.capture());
        assertThat(toBeResolvedOptional).containsOnly(waitingToBeResolved);
        setOfSpecialCharKeys.forEach(key ->
            assertThat(requestArgumentCaptor.getValue().httpRequestIntent().getPath()).contains(sha1Hex(key)));
    }

    @Test
    void save_OnSuccess_ShouldSaveMock() {
        // preparation
        final CustomObject customObjectMock = mock(CustomObject.class);
        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn("product-draft-key");

        final WaitingToBeResolved waitingToBeResolved =
            new WaitingToBeResolved(productDraftMock, singleton("test-ref"));
        when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(customObjectMock));

        // test
        final Optional<WaitingToBeResolved> result = service
            .save(waitingToBeResolved)
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(result).contains(waitingToBeResolved);
    }

    @SuppressWarnings("unchecked")
    @Test
    void save_OnSuccess_ShouldSaveMockWithSha1HashedKey() {
        // preparation
        final CustomObject customObjectMock = mock(CustomObject.class);
        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn("product-draft-key");

        final WaitingToBeResolved waitingToBeResolved =
            new WaitingToBeResolved(productDraftMock, singleton("test-ref"));
        when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(customObjectMock));
        final ArgumentCaptor<CustomObjectUpsertCommand<WaitingToBeResolved>> requestArgumentCaptor =
            ArgumentCaptor.forClass(CustomObjectUpsertCommand.class);

        // test
        final Optional<WaitingToBeResolved> result = service
            .save(waitingToBeResolved)
            .toCompletableFuture()
            .join();

        // assertions
        verify(productSyncOptions.getCtpClient()).execute(requestArgumentCaptor.capture());
        assertThat(result).contains(waitingToBeResolved);
        assertThat(requestArgumentCaptor.getValue().getDraft().getKey())
            .isEqualTo(sha1Hex(productDraftMock.getKey()));
    }

    @Test
    void save_WithUnsuccessfulMockCtpResponse_ShouldNotSaveMock() {
        // preparation
        final String productKey = "product-draft-key";
        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn(productKey);
        final WaitingToBeResolved waitingToBeResolved =
            new WaitingToBeResolved(productDraftMock, singleton("test-ref"));

        when(productSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        // test
        final Optional<WaitingToBeResolved> result = service
            .save(waitingToBeResolved)
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(result).isEmpty();
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message -> assertThat(message).contains(
                    format("Failed to save CustomObject with key: '%s' (hash of product key: '%s').",
                        sha1Hex(productKey), productKey)));

        assertThat(errorExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(exception ->
                        assertThat(exception).isExactlyInstanceOf(BadRequestException.class));
    }

    @Test
    void delete_WithUnsuccessfulMockCtpResponse_ShouldReturnProperException() {
        // preparation
        final ProductDraft productDraftMock = mock(ProductDraft.class);
        final String key = "product-draft-key";
        when(productDraftMock.getKey()).thenReturn(key);
        when(productSyncOptions.getCtpClient().execute(any()))
                .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        // test
        final Optional<WaitingToBeResolved> toBeResolvedOptional = service
            .delete("product-draft-key").toCompletableFuture().join();

        // assertions
        assertThat(toBeResolvedOptional).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message -> assertThat(message)
                        .contains(format("Failed to delete CustomObject with key: '%s' (hash of product key: '%s')",
                            sha1Hex(key), key)));
        assertThat(errorExceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(exception ->
                        assertThat(exception).isExactlyInstanceOf(BadRequestException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void delete_OnSuccess_ShouldRemoveTheResourceObject() {
        // preparation
        final CustomObject customObjectMock = mock(CustomObject.class);

        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn("product-draft-key");
        final WaitingToBeResolved waitingDraft = new WaitingToBeResolved(productDraftMock, singleton("test-ref"));
        when(customObjectMock.getValue()).thenReturn(waitingDraft);

        when(productSyncOptions.getCtpClient().execute(any(CustomObjectDeleteCommand.class)))
            .thenReturn(completedFuture(customObjectMock));

        // test
        final Optional<WaitingToBeResolved> toBeResolvedOptional = service
            .delete("product-draft-key").toCompletableFuture().join();

        // assertions
        assertThat(toBeResolvedOptional).contains(waitingDraft);
    }

    @SuppressWarnings("unchecked")
    @Test
    void delete_OnSuccess_ShouldMakeRequestWithSha1HashedKey() {
        // preparation
        final CustomObject customObjectMock = mock(CustomObject.class);

        final ProductDraft productDraftMock = mock(ProductDraft.class);
        when(productDraftMock.getKey()).thenReturn("product-draft-key");
        final WaitingToBeResolved waitingDraft = new WaitingToBeResolved(productDraftMock, singleton("test-ref"));
        when(customObjectMock.getValue()).thenReturn(waitingDraft);

        when(productSyncOptions.getCtpClient().execute(any(CustomObjectDeleteCommand.class)))
            .thenReturn(completedFuture(customObjectMock));
        final ArgumentCaptor<CustomObjectDeleteCommand<WaitingToBeResolved>> requestArgumentCaptor =
            ArgumentCaptor.forClass(CustomObjectDeleteCommand.class);

        // test
        final Optional<WaitingToBeResolved> toBeResolvedOptional = service
            .delete("product-draft-key").toCompletableFuture().join();

        // assertions
        verify(productSyncOptions.getCtpClient()).execute(requestArgumentCaptor.capture());
        assertThat(toBeResolvedOptional).contains(waitingDraft);
        final CustomObjectDeleteCommand<WaitingToBeResolved> value = requestArgumentCaptor.getValue();
        assertThat(value.httpRequestIntent().getPath())
            .contains(sha1Hex(productDraftMock.getKey()));
    }

    @Nonnull
    private PagedQueryResult getMockPagedQueryResult(@Nonnull final List results) {
        final PagedQueryResult pagedQueryResult = mock(PagedQueryResult.class);
        when(pagedQueryResult.getResults()).thenReturn(results);
        return pagedQueryResult;
    }
}
