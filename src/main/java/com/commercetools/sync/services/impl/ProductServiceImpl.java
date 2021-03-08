package com.commercetools.sync.services.impl;

import static java.lang.String.format;
import static java.util.Collections.singleton;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.ProductService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.products.queries.ProductQueryModel;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
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

    return fetchCachedResourceId(
        key,
        () -> ProductQuery.of().withPredicates(buildProductKeysQueryPredicate(singleton(key))));
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

  QueryPredicate<Product> buildProductKeysQueryPredicate(@Nonnull final Set<String> productKeys) {
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
  public CompletionStage<Set<Product>> fetchMatchingProductsByKeys(
      @Nonnull final Set<String> productKeys) {

    return fetchMatchingResources(
        productKeys,
        (keysNotCached) ->
            ProductQuery.of().withPredicates(buildProductKeysQueryPredicate(keysNotCached)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Product>> fetchProduct(@Nullable final String key) {

    return fetchResource(
        key,
        () -> ProductQuery.of().withPredicates(buildProductKeysQueryPredicate(singleton(key))));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Product>> createProduct(
      @Nonnull final ProductDraft productDraft) {
    return createResource(productDraft, ProductCreateCommand::of);
  }

  @Nonnull
  @Override
  public CompletionStage<Product> updateProduct(
      @Nonnull final Product product, @Nonnull final List<UpdateAction<Product>> updateActions) {
    return updateResource(product, ProductUpdateCommand::of, updateActions);
  }
}
