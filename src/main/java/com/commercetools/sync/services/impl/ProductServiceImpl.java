package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.reviews.ReviewRatingStatistics;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ProductServiceImpl implements ProductService {
    private final SphereClient ctpClient;

    public ProductServiceImpl(final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Override
    public CompletionStage<Optional<Product>> fetch(final String productKey) {
        return null;
    }

    @Override
    public CompletionStage<Product> create(final ProductDraft productDraft) {
        ctpClient.execute(null);
        return CompletableFuture.completedFuture(new Product() {
            @Override
            public ProductCatalogData getMasterData() {
                return null;
            }

            @Nullable
            @Override
            public Reference<State> getState() {
                return null;
            }

            @Nullable
            @Override
            public ReviewRatingStatistics getReviewRatingStatistics() {
                return null;
            }

            @Override
            public Reference<ProductType> getProductType() {
                return null;
            }

            @Nullable
            @Override
            public Reference<TaxCategory> getTaxCategory() {
                return null;
            }

            @Nullable
            @Override
            public String getKey() {
                return null;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public Long getVersion() {
                return null;
            }

            @Override
            public ZonedDateTime getCreatedAt() {
                return null;
            }

            @Override
            public ZonedDateTime getLastModifiedAt() {
                return null;
            }
        });
    }

    @Override
    public CompletionStage<Void> update(final Product product, final List<UpdateAction<Product>> updateActions) {
        return null;
    }
}
