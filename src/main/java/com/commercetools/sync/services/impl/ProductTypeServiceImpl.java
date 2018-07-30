package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQueryBuilder;
import io.sphere.sdk.queries.QueryExecutionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ProductTypeServiceImpl implements ProductTypeService {
    private final BaseSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean isCached = false;
    private final Map<String, Map<String, AttributeMetaData>> productsAttributesMetaData = new ConcurrentHashMap<>();

    public ProductTypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key) {
        if (!isCached) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final Consumer<List<ProductType>> productTypePageConsumer = productTypePage ->
                productTypePage.forEach(type -> {
                    final String fetchedTypeKey = type.getKey();
                    final String id = type.getId();
                    productsAttributesMetaData.put(id, getAttributeMetaDataMap(type));
                    if (StringUtils.isNotBlank(fetchedTypeKey)) {
                        keyToIdCache.put(fetchedTypeKey, id);
                    } else {
                        syncOptions.applyWarningCallback(format("ProductType with id: '%s' has no key set. Keys are"
                                + " required for productType matching.", id));
                    }
                });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), ProductTypeQuery.of(), productTypePageConsumer)
                .thenAccept(result -> isCached = true)
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
    @Override
    public CompletionStage<List<ProductType>> fetchMatchingProductsTypesByKeys(@Nonnull final Set<String> keys) {
        final ProductTypeQuery query = ProductTypeQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .build();

        return QueryExecutionUtils.queryAll(syncOptions.getCtpClient(), query);
    }

    @Nonnull
    @Override
    public CompletionStage<ProductType> createProductType(@Nonnull final ProductTypeDraft productTypeDraft) {
        return syncOptions.getCtpClient().execute(ProductTypeCreateCommand.of(productTypeDraft));
    }

    @Nonnull
    @Override
    public CompletionStage<ProductType> updateProductType(
            @Nonnull final ProductType productType, @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        return syncOptions.getCtpClient().execute(ProductTypeUpdateCommand.of(productType, updateActions));
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
