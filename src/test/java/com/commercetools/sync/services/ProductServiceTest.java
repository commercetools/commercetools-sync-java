package com.commercetools.sync.services;

import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.products.Product;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductServiceTest {
    @Test
    public void of() {
        assertThat(ProductService.of()).isInstanceOf(ProductServiceImpl.class);
    }

    @Test
    public void create() {
        ProductService service = ProductService.of();

        CompletionStage<Void> stage = service.create(null);

        assertThat(stage).isNull();
    }

    @Test
    public void update() {
        ProductService service = ProductService.of();

        CompletionStage<Void> stage = service.update(null, null);

        assertThat(stage).isNull();
    }

    @Test
    public void fetch() {
        ProductService service = ProductService.of();

        CompletionStage<Optional<Product>> stage = service.fetch(null);

        assertThat(stage).isNull();
    }

}