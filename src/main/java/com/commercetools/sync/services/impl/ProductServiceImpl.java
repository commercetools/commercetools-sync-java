package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.lang.String.format;
import static java.util.Collections.singleton;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductLike;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.expansion.ProductProjectionExpansionModel;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.products.queries.ProductProjectionQueryModel;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public final class ProductServiceImpl
    extends BaseServiceWithKey<
        ProductDraft,
        Product,
        ProductProjection,
        ProductSyncOptions,
        ProductProjectionQuery,
        ProductProjectionQueryModel,
        ProductProjectionExpansionModel<ProductProjection>>
    implements ProductService {

  public ProductServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable final String key) {

    return fetchCachedResourceId(
        key,
        ProductLike::getKey,
        () ->
            ProductProjectionQuery.ofStaged()
                .withPredicates(buildProductKeysQueryPredicate(singleton(key))));
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> productKeys) {

    return cacheKeysToIds(
        productKeys,
        keysNotCached ->
            new ResourceKeyIdGraphQlRequest(keysNotCached, GraphQlQueryResources.PRODUCTS));
  }

  QueryPredicate<ProductProjection> buildProductKeysQueryPredicate(
      @Nonnull final Set<String> productKeys) {
    final List<String> keysSurroundedWithDoubleQuotes =
        productKeys.stream()
            .filter(StringUtils::isNotBlank)
            .map(productKey -> format("\"%s\"", productKey))
            .collect(Collectors.toList());
    String keysQueryString = keysSurroundedWithDoubleQuotes.toString();
    // Strip square brackets from list string. For example: ["key1", "key2"] -> "key1", "key2"
    keysQueryString = keysQueryString.substring(1, keysQueryString.length() - 1);
    return QueryPredicate.of(format("key in (%s)", keysQueryString));
  }

  @Nonnull
  @Override
  public CompletionStage<Set<ProductProjection>> fetchMatchingProductsByKeys(
      @Nonnull final Set<String> productKeys) {

    return fetchMatchingResources(
        productKeys,
        ProductProjection::getKey,
        (keysNotCached) ->
            ProductProjectionQuery.ofStaged()
                .withPredicates(buildProductKeysQueryPredicate(keysNotCached)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductProjection>> fetchProduct(@Nullable final String key) {

    return fetchResource(
        key,
        () ->
            ProductProjectionQuery.ofStaged()
                .withPredicates(buildProductKeysQueryPredicate(singleton(key))));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductProjection>> createProduct(
      @Nonnull final ProductDraft productDraft) {

    return createResource(productDraft, ProductCreateCommand::of)
        .thenApply(product -> product.map(opt -> opt.toProjection(STAGED)));
  }

  @Nonnull
  @Override
  public CompletionStage<ProductProjection> updateProduct(
      @Nonnull final ProductProjection productProjection,
      @Nonnull final List<UpdateAction<Product>> updateActions) {

    return updateProductAndMapToProductProjection(productProjection, updateActions);
  }

  @Nonnull
  private CompletionStage<ProductProjection> updateProductAndMapToProductProjection(
      @Nonnull final ProductProjection productProjection,
      @Nonnull final List<UpdateAction<Product>> updateActions) {

    final List<List<UpdateAction<Product>>> batches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ProductProjection> resultStage =
        CompletableFuture.completedFuture(productProjection);

    for (final List<UpdateAction<Product>> batch : batches) {
      resultStage =
          resultStage.thenCompose(
              updatedProductProjection ->
                  syncOptions
                      .getCtpClient()
                      .execute(ProductUpdateCommand.of(updatedProductProjection, batch))
                      .thenApply(p -> p.toProjection(STAGED)));
    }
    return resultStage;
  }
}
