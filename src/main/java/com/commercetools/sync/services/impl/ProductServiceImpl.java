package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ChunkUtils;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.client.SphereRequest;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.products.queries.ProductQueryModel;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public final class ProductServiceImpl
    extends BaseServiceWithKey<
        ProductDraft,
        Product,
        ProductSyncOptions,
        ProductQuery,
        ProductQueryModel,
        ProductExpansionModel<Product>>
    implements ProductService {

  public ProductServiceImpl(@Nonnull final ProductSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable final String key) {

    return fetchCachedResourceIdInternal(
        key,
        resource -> resource.getKey(),
        () ->
            ProductProjectionQuery.ofStaged()
                .withPredicates(buildProductKeysQueryPredicate(singleton(key))));
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceIdInternal(
      @Nullable final String key,
      @Nonnull final Function<ProductProjection, String> keyMapper,
      @Nonnull final Supplier<ProductProjectionQuery> querySupplier) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }
    final Consumer<List<ProductProjection>> pageConsumer =
        page ->
            page.forEach(resource -> keyToIdCache.put(keyMapper.apply(resource), resource.getId()));

    return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), querySupplier.get(), pageConsumer)
        .thenApply(result -> Optional.ofNullable(keyToIdCache.getIfPresent(key)));
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

    return fetchMatchingResourcesInternal(
        productKeys,
        resource -> resource.getKey(),
        (keysNotCached) ->
            ProductProjectionQuery.ofStaged()
                .withPredicates(buildProductKeysQueryPredicate(keysNotCached)));
  }

  <Q extends SphereRequest<PagedQueryResult<T>>, T extends ProductProjection>
      CompletionStage<Set<ProductProjection>> fetchMatchingResourcesInternal(
          @Nonnull final Set<String> keys,
          @Nonnull final Function<T, String> keyMapper,
          @Nonnull final Function<Set<String>, Q> keysQueryMapper) {

    if (keys.isEmpty()) {
      return CompletableFuture.completedFuture(Collections.emptySet());
    }

    final List<List<String>> chunkedKeys = ChunkUtils.chunk(keys, CHUNK_SIZE);

    List<Q> keysQueryMapperList =
        chunkedKeys.stream()
            .map(_keys -> keysQueryMapper.apply(new HashSet<>(_keys)))
            .collect(toList());

    return ChunkUtils.executeChunks(syncOptions.getCtpClient(), keysQueryMapperList)
        .thenApply(ChunkUtils::flattenPagedQueryResults)
        .thenApply(
            chunk -> {
              chunk.forEach(
                  resource -> keyToIdCache.put(keyMapper.apply(resource), resource.getId()));
              return new HashSet<>(chunk);
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductProjection>> fetchProduct(@Nullable final String key) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    return syncOptions
        .getCtpClient()
        .execute(
            ProductProjectionQuery.ofStaged()
                .withPredicates(buildProductKeysQueryPredicate(singleton(key))))
        .thenApply(
            pagedQueryResult ->
                pagedQueryResult
                    .head()
                    .map(
                        resource -> {
                          keyToIdCache.put(key, resource.getId());
                          return resource;
                        }));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ProductProjection>> createProduct(
      @Nonnull final ProductDraft productDraft) {
    return createResource(productDraft, ProductCreateCommand::of)
        .thenApply(product -> product.map(opt -> opt.toProjection(STAGED)));
  }

  @Nonnull
  public CompletionStage<ProductProjection> updateProduct(
      @Nonnull final ProductProjection productProjection,
      @Nonnull final List<UpdateAction<Product>> updateActions) {

    return updateResourceByKey(productProjection, updateActions);
  }

  @Nonnull
  CompletionStage<ProductProjection> updateResourceByKey(
      @Nonnull final ProductProjection resource,
      @Nonnull final List<UpdateAction<Product>> updateActions) {

    final List<List<UpdateAction<Product>>> batches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);
    CompletionStage<ProductProjection> resultStage = CompletableFuture.completedFuture(resource);
    for (final List<UpdateAction<Product>> batch : batches) {
      resultStage =
          resultStage.thenCompose(
              productProjection ->
                  syncOptions
                      .getCtpClient()
                      .execute(
                          ProductUpdateCommand.ofKey(
                              productProjection.getKey(), resource.getVersion(), batch))
                      .thenApply(p -> p.toProjection(STAGED)));
    }
    return resultStage;
  }
}
