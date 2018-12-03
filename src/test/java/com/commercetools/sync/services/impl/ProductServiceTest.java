package com.commercetools.sync.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductServiceTest {

    private ProductServiceImpl service;
    private ProductSyncOptions productSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @Before
    public void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                      .errorCallback((errorMessage, errorException) -> {
                                                          errorMessages.add(errorMessage);
                                                          errorExceptions.add(errorException);
                                                      })
                                                      .build();
        service = new ProductServiceImpl(productSyncOptions);
    }

    @Test
    public void createProduct_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        final Product mock = mock(Product.class);
        when(mock.getId()).thenReturn("productId");
        when(mock.getKey()).thenReturn("productKey");

        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        final ProductDraft draft = mock(ProductDraft.class);
        when(draft.getKey()).thenReturn("productKey");
        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        assertThat(productOptional).isNotEmpty();
        assertThat(productOptional).containsSame(mock);
        verify(productSyncOptions.getCtpClient()).execute(eq(ProductCreateCommand.of(draft)));
    }

    @Test
    public void createProduct_WithUnSuccessfulMockCtpResponse_ShouldNotCreateProduct() {
        // preparation
        final Product mock = mock(Product.class);
        when(mock.getId()).thenReturn("productId");

        when(productSyncOptions.getCtpClient().execute(any()))
            .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

        final ProductDraft draft = mock(ProductDraft.class);
        when(draft.getKey()).thenReturn("productKey");

        // test
        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        // assertion
        assertThat(productOptional).isEmpty();
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message -> {
                assertThat(message).contains("Failed to create draft with key: 'productKey'.");
                assertThat(message).contains("BadRequestException");
            });

        assertThat(errorExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(exception ->
                assertThat(exception).isExactlyInstanceOf(BadRequestException.class));
    }

    @Test
    public void createProduct_WithDraftWithoutKey_ShouldNotCreateProduct() {
        final ProductDraft draft = mock(ProductDraft.class);
        final Optional<Product> productOptional = service.createProduct(draft).toCompletableFuture().join();

        assertThat(productOptional).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages.get(0))
            .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
    }

    @Test
    public void updateProduct_WithMockCtpResponse_ShouldReturnMock() {
        final Product mock = mock(Product.class);
        when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(mock));

        final List<UpdateAction<Product>> updateActions =
            singletonList(ChangeName.of(LocalizedString.of(ENGLISH, "new name")));
        final Product product = service.updateProduct(mock, updateActions).toCompletableFuture().join();

        assertThat(product).isSameAs(mock);
        verify(productSyncOptions.getCtpClient()).execute(eq(ProductUpdateCommand.of(mock, updateActions)));
    }

    @Test
    public void buildProductKeysQueryPredicate_WithEmptyProductKeysSet_ShouldBuildCorrectQuery() {
        final QueryPredicate<Product> queryPredicate = service.buildProductKeysQueryPredicate(new HashSet<>());
        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in ()");
    }

    @Test
    public void buildProductKeysQueryPredicate_WithProductKeysSet_ShouldBuildCorrectQuery() {
        final HashSet<String> productKeys = new HashSet<>();
        productKeys.add("key1");
        productKeys.add("key2");
        final QueryPredicate<Product> queryPredicate = service.buildProductKeysQueryPredicate(productKeys);
        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in (\"key1\", \"key2\")");
    }

    @Test
    public void buildProductKeysQueryPredicate_WithSomeBlankProductKeys_ShouldBuildCorrectQuery() {
        final HashSet<String> productKeys = new HashSet<>();
        productKeys.add("key1");
        productKeys.add("key2");
        productKeys.add("");
        productKeys.add(null);
        final QueryPredicate<Product> queryPredicate = service.buildProductKeysQueryPredicate(productKeys);
        assertThat(queryPredicate.toSphereQuery()).isEqualTo("key in (\"key1\", \"key2\")");
    }
}