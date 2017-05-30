package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductServiceTest {

    private ProductService service;
    private SphereClient ctpClient;

    @Before
    public void setUp() throws Exception {
        ctpClient = spy(SphereClient.class);
        service = ProductService.of(ctpClient);
    }

    @Test
    public void create() {
        when(ctpClient.execute(any())).thenReturn(completedFuture(mock(Product.class)));

        Product product = service.create(mock(ProductDraft.class)).toCompletableFuture().join();

        assertThat(product).isNotNull();
        verify(ctpClient).execute(any());
    }

    @Test
    public void update() {
        when(ctpClient.execute(any())).thenReturn(completedFuture(mock(Product.class)));

        Product product = service.update(mock(Product.class), emptyList()).toCompletableFuture().join();

        assertThat(product).isNotNull();
        verify(ctpClient).execute(any());
    }

    @Test
    public void fetch_missing() {
        PagedQueryResult result = mock(PagedQueryResult.class);
        when(result.head()).thenReturn(Optional.empty());
        when(ctpClient.execute(any())).thenReturn(completedFuture(result));

        Optional<Product> productOptional = service.fetch(null).toCompletableFuture().join();

        assertThat(productOptional).isNotPresent();
    }

    @Test
    public void fetch_existing() {
        PagedQueryResult result = mock(PagedQueryResult.class);
        Product product = mock(Product.class);
        when(result.head()).thenReturn(Optional.of(product));
        when(ctpClient.execute(any())).thenReturn(completedFuture(result));

        Optional<Product> productOptional = service.fetch(null).toCompletableFuture().join();

        assertThat(productOptional).hasValue(product);
    }

}