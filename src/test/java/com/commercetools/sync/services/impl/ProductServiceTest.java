package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;

import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.queries.QueryPredicate;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductServiceImpl service;
    private ProductSyncOptions productSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        initMockService(mock(SphereClient.class));

    }

    @Test
    void createProduct_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        final Product mock = mock(Product.class);
        when(mock.getId()).thenReturn("productId");
        when(mock.getKey()).thenReturn("productKey");
        final FakeClient createProductClient =
                new FakeClient(mock);

        final ProductDraft draft = mock(ProductDraft.class);
        when(draft.getKey()).thenReturn("productKey");

        initMockService(createProductClient);

        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        assertThat(productOptional).isNotEmpty();
        assertThat(productOptional).containsSame(mock);
        assertThat(createProductClient.isExecuted()).isTrue();
    }

    @Test
    void createProduct_WithUnSuccessfulMockCtpResponse_ShouldNotCreateProduct() {
        // preparation
        final Product mock = mock(Product.class);
        when(mock.getId()).thenReturn("productId");

        final FakeClient createProductClient =
                new FakeClient(new BadRequestException("bad request"));

        final ProductDraft draft = mock(ProductDraft.class);
        when(draft.getKey()).thenReturn("productKey");

        initMockService(createProductClient);

        // test
        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        // assertion
        assertThat(productOptional).isEmpty();
        assertThat(errorMessages)
                .hasSize(1)
                .singleElement().satisfies(message -> {
            assertThat(message).contains("Failed to create draft with key: 'productKey'.");
            assertThat(message).contains("BadRequestException");
        });

        assertThat(errorExceptions)
                .hasSize(1)
                .singleElement().satisfies(exception ->
                assertThat(exception).isExactlyInstanceOf(BadRequestException.class));
    }

    @Test
    void createProduct_WithDraftWithoutKey_ShouldNotCreateProduct() {
        final ProductDraft draft = mock(ProductDraft.class);
        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        assertThat(productOptional).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages.get(0))
                .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
    }

    @Test
    void updateProduct_WithMockCtpResponse_ShouldReturnMock() {
        final Product mock = mock(Product.class);
        final FakeClient updateProductClient = new FakeClient(mock);

        initMockService(updateProductClient);

        final List<UpdateAction<Product>> updateActions =
                singletonList(ChangeName.of(LocalizedString.of(ENGLISH, "new name")));
        final Product product = service.updateProduct(mock, updateActions).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        assertThat(updateProductClient.isExecuted()).isTrue();
    }

    @Test
    void buildProductKeysQueryPredicate_WithEmptyProductKeysSet_ShouldBuildCorrectQuery() {
        final QueryPredicate<Product> queryPredicate = service.buildProductKeysQueryPredicate(new HashSet<>());
        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in ()");
    }

    @Test
    void buildProductKeysQueryPredicate_WithProductKeysSet_ShouldBuildCorrectQuery() {
        final HashSet<String> productKeys = new HashSet<>();
        productKeys.add("key1");
        productKeys.add("key2");
        final QueryPredicate<Product> queryPredicate = service.buildProductKeysQueryPredicate(productKeys);
        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in (\"key1\", \"key2\")");
    }

    @Test
    void buildProductKeysQueryPredicate_WithSomeBlankProductKeys_ShouldBuildCorrectQuery() {
        final HashSet<String> productKeys = new HashSet<>();
        productKeys.add("key1");
        productKeys.add("key2");
        productKeys.add("");
        productKeys.add(null);
        final QueryPredicate<Product> queryPredicate = service.buildProductKeysQueryPredicate(productKeys);
        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in (\"key1\", \"key2\")");
    }

    @Test
    void fetchMatchingProductsByKeys_WithBadGateWayExceptionAlways_ShouldFail() {
        final FakeClient fakeProductClient = new FakeClient(new BadGatewayException());

        initMockService(fakeProductClient);

        final Set<String> keys =  new HashSet<>();
        keys.add("productKey1");

        CompletionStage<Set<Product>> future = service.fetchMatchingProductsByKeys(keys);

        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(future.toCompletableFuture())
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

    @Test
    void fetchProduct_WithBadGatewayException_ShouldFail() {
        final FakeClient<Product> fakeProductClient = new FakeClient(new BadGatewayException());

        initMockService(fakeProductClient);

        CompletionStage<Optional<Product>> future = service.fetchProduct("productKey1");

        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(future.toCompletableFuture())
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);

    }

    private void initMockService(
            @Nonnull final SphereClient mockSphereClient) {

        productSyncOptions = ProductSyncOptionsBuilder
                .of(mockSphereClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    errorExceptions.add(exception.getCause());
                })
                .build();

        service = new ProductServiceImpl(productSyncOptions);
    }
}