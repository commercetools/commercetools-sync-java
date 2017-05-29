package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
        Product product = service.create(mock(ProductDraft.class)).toCompletableFuture().join();

        assertThat(product).isNotNull();
        verify(ctpClient).execute(any());
    }

    @Test
    public void update() {
        CompletionStage<Void> stage = service.update(null, null);

        assertThat(stage).isNull();
    }

    @Test
    public void fetch() {
        CompletionStage<Optional<Product>> stage = service.fetch(null);

        assertThat(stage).isNull();
    }

}