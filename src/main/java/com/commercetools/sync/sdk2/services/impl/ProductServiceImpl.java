package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyProductProjectionsGet;
import com.commercetools.api.client.ByProjectKeyProductProjectionsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyProductsPost;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductUpdateBuilder;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.services.ProductService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProductServiceImpl
    extends BaseServiceWithKey<
        ProductSyncOptions,
        ProductProjection,
        ProductDraft,
        ByProjectKeyProductProjectionsGet,
        ProductProjectionPagedQueryResponse,
        ByProjectKeyProductProjectionsKeyByKeyGet,
        Product,
        ByProjectKeyProductsPost>
    implements ProductService {

  public ProductServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable final String key) {
    if (key == null) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final ByProjectKeyProductProjectionsGet query =
        syncOptions
            .getCtpClient()
            .productProjections()
            .get()
            .withWhere("key = :key")
            .withPredicateVar("key", key);

    return fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> productKeys) {
    return super.cacheKeysToIdsUsingGraphQl(productKeys, GraphQlQueryResource.PRODUCTS);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<ProductProjection>> fetchMatchingProductsByKeys(
      @Nonnull final Set<String> productKeys) {
    return super.fetchMatchingResources(
        productKeys,
        ProductProjection::getKey,
        (keysNotCached) ->
            syncOptions
                .getCtpClient()
                .productProjections()
                .get()
                .withStaged(true)
                .withWhere("key in :keys")
                .withPredicateVar("keys", keysNotCached));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductProjection>> fetchProduct(@Nullable final String key) {
    final ByProjectKeyProductProjectionsKeyByKeyGet query =
        syncOptions.getCtpClient().productProjections().withKey(key).get();
    return super.fetchResource(key, query);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductProjection>> createProduct(
      @Nonnull final ProductDraft productDraft) {
    return super.createResource(
        productDraft,
        ProductDraft::getKey,
        Product::getId,
        product -> ProductMixin.toProjection(product, ProductProjectionType.STAGED),
        () -> syncOptions.getCtpClient().products().create(productDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<ProductProjection> updateProduct(
      @Nonnull final ProductProjection productProjection,
      @Nonnull final List<ProductUpdateAction> updateActions) {
    return updateProductAndMapToProductProjection(productProjection, updateActions);
  }

  @Nonnull
  private CompletionStage<ProductProjection> updateProductAndMapToProductProjection(
      @Nonnull final ProductProjection productProjection,
      @Nonnull final List<ProductUpdateAction> updateActions) {
    final List<List<ProductUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ProductProjection> resultStage =
        CompletableFuture.completedFuture(productProjection);

    for (final List<ProductUpdateAction> batch : actionBatches) {

      resultStage =
          resultStage
              .thenCompose(
                  updatedProduct ->
                      syncOptions
                          .getCtpClient()
                          .products()
                          .withId(updatedProduct.getId())
                          .post(
                              ProductUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedProduct.getVersion())
                                  .build())
                          .execute())
              .thenApply(ApiHttpResponse::getBody)
              .thenApply(
                  product -> ProductMixin.toProjection(product, ProductProjectionType.STAGED));
    }

    return resultStage;
  }
}
