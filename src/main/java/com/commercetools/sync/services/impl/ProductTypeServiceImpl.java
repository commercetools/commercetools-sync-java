package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyProductTypesGet;
import com.commercetools.api.client.ByProjectKeyProductTypesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyProductTypesPost;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypePagedQueryResponse;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.api.models.product_type.ProductTypeUpdateBuilder;
import com.commercetools.api.predicates.query.product_type.ProductTypeQueryBuilderDsl;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.services.ProductTypeService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProductTypeServiceImpl
    extends BaseService<
        BaseSyncOptions,
        ProductType,
        ProductTypeDraft,
        ByProjectKeyProductTypesGet,
        ProductTypePagedQueryResponse,
        ByProjectKeyProductTypesKeyByKeyGet,
        ProductType,
        ProductTypeQueryBuilderDsl,
        ByProjectKeyProductTypesPost>
    implements ProductTypeService {

  private final Map<String, Map<String, AttributeMetaData>> productsAttributesMetaData =
      new ConcurrentHashMap<>();

  public ProductTypeServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> categoryKeys) {
    return super.cacheKeysToIdsUsingGraphQl(categoryKeys, GraphQlQueryResource.PRODUCT_TYPES);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull final String key) {
    final ByProjectKeyProductTypesGet query =
        syncOptions
            .getCtpClient()
            .productTypes()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return fetchCachedResourceId(key, query);
  }

  @Nonnull
  private static Map<String, AttributeMetaData> getAttributeMetaDataMap(
      @Nonnull final ProductType productType) {
    return productType.getAttributes().stream()
        .map(AttributeMetaData::of)
        .collect(
            Collectors.toMap(AttributeMetaData::getName, attributeMetaData -> attributeMetaData));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Map<String, AttributeMetaData>>>
      fetchCachedProductAttributeMetaDataMap(@Nonnull final String productTypeId) {

    if (productsAttributesMetaData.isEmpty()) {
      return fetchAndCacheProductMetaData(productTypeId);
    }
    return CompletableFuture.completedFuture(
        Optional.ofNullable(productsAttributesMetaData.get(productTypeId)));
  }

  @Nonnull
  private CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchAndCacheProductMetaData(
      @Nonnull final String productTypeId) {
    final Consumer<List<ProductType>> productTypePageConsumer =
        productTypePage ->
            productTypePage.forEach(
                type -> {
                  final String id = type.getId();
                  productsAttributesMetaData.put(id, getAttributeMetaDataMap(type));
                });
    final ByProjectKeyProductTypesGet byProjectKeyProductTypesGet =
        this.syncOptions.getCtpClient().productTypes().get();

    return QueryUtils.queryAll(byProjectKeyProductTypesGet, productTypePageConsumer)
        .thenApply(result -> Optional.ofNullable(productsAttributesMetaData.get(productTypeId)));
  }

  @Nonnull
  @Override
  public CompletionStage<Set<ProductType>> fetchMatchingProductTypesByKeys(
      @Nonnull final Set<String> keys) {
    return fetchMatchingResources(
        keys,
        ProductType::getKey,
        (keysNotCached) ->
            syncOptions
                .getCtpClient()
                .productTypes()
                .get()
                .withWhere("key in :keys")
                .withPredicateVar("keys", keysNotCached));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductType>> createProductType(
      @Nonnull final ProductTypeDraft productTypeDraft) {
    return super.createResource(
        productTypeDraft,
        ProductTypeDraft::getKey,
        ProductType::getId,
        Function.identity(),
        () -> syncOptions.getCtpClient().productTypes().post(productTypeDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<ProductType> updateProductType(
      @Nonnull final ProductType productType,
      @Nonnull final List<ProductTypeUpdateAction> updateActions) {

    final List<List<ProductTypeUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<ProductType>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, productType));

    for (final List<ProductTypeUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedProductType ->
                      syncOptions
                          .getCtpClient()
                          .productTypes()
                          .withId(updatedProductType.getId())
                          .post(
                              ProductTypeUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedProductType.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductType>> fetchProductType(@Nullable final String key) {
    return super.fetchResource(key, syncOptions.getCtpClient().productTypes().withKey(key).get());
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key, @Nonnull final ByProjectKeyProductTypesGet query) {
    return super.fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }
}
