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
import io.sphere.sdk.producttypes.expansion.ProductTypeExpansionModel;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.producttypes.queries.ProductTypeQueryBuilder;
import io.sphere.sdk.producttypes.queries.ProductTypeQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;

public final class ProductTypeServiceImpl extends BaseServiceWithKey<ProductTypeDraft, ProductType, BaseSyncOptions,
    ProductTypeQuery, ProductTypeQueryModel, ProductTypeExpansionModel<ProductType>> implements ProductTypeService {

    private final Map<String, Map<String, AttributeMetaData>> productsAttributesMetaData = new ConcurrentHashMap<>();

    public ProductTypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> keys) {

        return cacheKeysToIds(
            keys,
            keysNotCached -> ProductTypeQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keysNotCached))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key) {

        return fetchCachedResourceId(key,
            () -> ProductTypeQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(singleton(key)))
                .build());
    }

    @Nonnull
    private static Map<String, AttributeMetaData> getAttributeMetaDataMap(@Nonnull final ProductType productType) {
        return productType
            .getAttributes().stream()
            .map(AttributeMetaData::of)
            .collect(
                Collectors.toMap(AttributeMetaData::getName, attributeMetaData -> attributeMetaData)
            );
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchCachedProductAttributeMetaDataMap(
        @Nonnull final String productTypeId) {

        if (productsAttributesMetaData.isEmpty()) {
            return fetchAndCacheProductMetaData(productTypeId);
        }
        return CompletableFuture.completedFuture(
            Optional.ofNullable(productsAttributesMetaData.get(productTypeId))
        );
    }

    @Nonnull
    private CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchAndCacheProductMetaData(
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

    @Nonnull
    @Override
    public CompletionStage<Set<ProductType>> fetchMatchingProductTypesByKeys(@Nonnull final Set<String> keys) {
        return fetchMatchingResources(keys,
            () -> ProductTypeQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<ProductType>> createProductType(@Nonnull final ProductTypeDraft productTypeDraft) {
        return createResource(productTypeDraft, ProductTypeCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<ProductType> updateProductType(
        @Nonnull final ProductType productType, @Nonnull final List<UpdateAction<ProductType>> updateActions) {

        return updateResource(productType, ProductTypeUpdateCommand::of, updateActions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<ProductType>> fetchProductType(@Nullable final String key) {

        return fetchResource(key,
            () -> ProductTypeQueryBuilder.of().plusPredicates(queryModel -> queryModel.key().is(key)).build());
    }
}
