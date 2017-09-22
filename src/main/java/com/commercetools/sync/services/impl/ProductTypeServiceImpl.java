package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ProductTypeServiceImpl implements ProductTypeService {
    private final ProductSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AttributeMetaData>> productsAttributesMetaData = new ConcurrentHashMap<>();
    private static final String PRODUCT_TYPE_KEY_NOT_SET = "ProductType with id: '%s' has no key set. "
        + "Keys are required for productType matching.";

    public ProductTypeServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key) {
        if (keyToIdCache.isEmpty()) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final Consumer<List<ProductType>> productTypePageConsumer = productTypePage ->
            productTypePage.forEach(type -> {
                final String fetchedTypekey = type.getKey();
                final String id = type.getId();
                productsAttributesMetaData.put(id, getAttributeMetaDataMap(type));
                if (StringUtils.isNotBlank(fetchedTypekey)) {
                    keyToIdCache.put(fetchedTypekey, id);
                } else {
                    syncOptions.applyWarningCallback(format(PRODUCT_TYPE_KEY_NOT_SET, id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), ProductTypeQuery.of(), productTypePageConsumer)
                            .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private static Map<String, AttributeMetaData> getAttributeMetaDataMap(@Nonnull final ProductType productType) {
        return productType.getAttributes().stream()
                          .map(AttributeMetaData::of)
                          .collect(
                              Collectors.toMap(AttributeMetaData::getName, attributeMetaData -> attributeMetaData));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchCachedProductAttributeMetaDataMap(
        @Nonnull final String productTypeId) {
        if (productsAttributesMetaData.isEmpty()) {
            return cacheAndFetchProductMetaData(productTypeId);
        }
        return CompletableFuture.completedFuture(
            Optional.ofNullable(productsAttributesMetaData.get(productTypeId))
        );
    }

    @Nonnull
    private CompletionStage<Optional<Map<String, AttributeMetaData>>> cacheAndFetchProductMetaData(
        @Nonnull final String productTypeId) {
        final Consumer<List<ProductType>> productTypePageConsumer = productTypePage ->
            productTypePage.forEach(type -> {
                final String id = type.getId();
                productsAttributesMetaData.put(id, getAttributeMetaDataMap(type));
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), ProductTypeQuery.of(), productTypePageConsumer)
                            .thenApply(result ->
                                Optional.ofNullable(productsAttributesMetaData.get(productTypeId)));
    }
}
