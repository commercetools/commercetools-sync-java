package com.commercetools.sync.services;

import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ProductService {

  /**
   * Given a {@code key}, if it is blank (null/empty), a completed future with an empty optional is
   * returned. This method then checks if the cached map of product keys -&gt; ids contains the key.
   * If it does, then an optional containing the mapped id is returned. If the cache doesn't contain
   * the key; this method attempts to fetch the id of the key from the CTP project, caches it and
   * returns a {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&lt;{@link
   * String}&gt;&gt; in which the result of it's completion could contain an {@link
   * java.util.Optional} with the id inside of it or an empty {@link java.util.Optional} if no
   * {@link ProductProjection} was found in the CTP project with this key.
   *
   * @param key the key by which a {@link ProductProjection} id should be fetched from the CTP
   *     project.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&lt;{@link
   *     String}&gt;&gt; in which the result of it's completion could contain an {@link
   *     java.util.Optional} with the id inside of it or an empty {@link java.util.Optional} if no
   *     {@link ProductProjection} was found in the CTP project with this key.
   */
  @Nonnull
  CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable String key);

  /**
   * Filters out the keys which are already cached and fetches only the not-cached product keys from
   * the CTP project defined in an injected {@link com.commercetools.api.client.ProjectApiRoot} and
   * stores a mapping for every product to id in the cached map of keys -&gt; ids and returns this
   * cached map.
   *
   * <p>Note: If all the supplied keys are already cached, the cached map is returned right away
   * with no request to CTP.
   *
   * @param productKeys the product keys to fetch and cache the ids for.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a map of all product keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> productKeys);

  /**
   * Given a {@link java.util.Set} of product keys, this method fetches a set of all the products,
   * matching this given set of keys in the CTP project, defined in an injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. A mapping of the key to the id of the fetched
   * products is persisted in an in-memory map.
   *
   * @param productKeys set of product keys to fetch matching products by.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a {@link java.util.Set} of all matching products.
   */
  @Nonnull
  CompletionStage<Set<ProductProjection>> fetchMatchingProductsByKeys(
      @Nonnull Set<String> productKeys);

  /**
   * Given a product key, this method fetches a product that matches this given key in the CTP
   * project defined in a potentially injected {@link com.commercetools.api.client.ProjectApiRoot}.
   * If there is no matching product an empty {@link java.util.Optional} will be returned in the
   * returned future. A mapping of the key to the id of the fetched category is persisted in an in
   * -memory map.
   *
   * @param key the key of the product to fetch.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&gt; in which
   *     the result of it's completion contains an {@link java.util.Optional} that contains the
   *     matching {@link ProductProjection} if exists, otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<ProductProjection>> fetchProduct(@Nullable String key);

  /**
   * Given a resource draft of type {@link ProductDraft}, this method attempts to create a resource
   * {@link ProductProjection} based on it in the CTP project defined by the sync options.
   *
   * <p>A completion stage containing an empty option and the error callback will be triggered in
   * those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the resource gets created successfully on CTP, then the created
   * resource's id and key are cached and the method returns a {@link
   * java.util.concurrent.CompletionStage} in which the result of it's completion contains an
   * instance {@link java.util.Optional} of the resource which was created.
   *
   * @param productDraft the resource draft to create a resource based off of.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<ProductProjection>> createProduct(@Nonnull ProductDraft productDraft);

  /**
   * Given a {@link ProductProjection} and a {@link java.util.List}&lt;{@link
   * com.commercetools.api.models.product.ProductUpdateAction}&gt;, this method issues an update
   * request with these update actions on this {@link ProductProjection} in the CTP project defined
   * in a potentially injected {@link com.commercetools.api.client.ProjectApiRoot}. This method
   * returns {@link java.util.concurrent.CompletionStage}&lt;{@link ProductProjection}&gt; in which
   * the result of it's completion contains an instance of the {@link ProductProjection} which was
   * updated in the CTP project.
   *
   * @param product the {@link ProductProjection} to update.
   * @param updateActions the update actions to update the {@link ProductProjection} with.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link ProductProjection}&gt;
   *     containing as a result of it's completion an instance of the {@link ProductProjection}
   *     which was updated in the CTP project or a {@link java.util.concurrent.CompletionException}.
   */
  @Nonnull
  public CompletionStage<ProductProjection> updateProduct(
      @Nonnull final ProductProjection product,
      @Nonnull final List<ProductUpdateAction> updateActions);
}
