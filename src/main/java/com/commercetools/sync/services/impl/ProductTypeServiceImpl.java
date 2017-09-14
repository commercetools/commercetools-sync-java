package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ProductTypeServiceImpl implements ProductTypeService {
    private final SphereClient ctpClient;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    public ProductTypeServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final Consumer<List<ProductType>> productTypePageConsumer = productTypePage ->
            productTypePage.forEach(type -> keyToIdCache.put(type.getKey(), type.getId()));

        return CtpQueryUtils.queryAll(ctpClient, ProductTypeQuery.of(), productTypePageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key) {
        if (keyToIdCache.isEmpty()) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }
}
