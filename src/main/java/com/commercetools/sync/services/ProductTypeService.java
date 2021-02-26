package com.commercetools.sync.services;

import com.commercetools.sync.products.AttributeMetaData;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ProductTypeService {

  /**
   * Filters out the keys which are already cached and fetches only the not-cached product type keys
   * from the CTP project defined in an injected {@link SphereClient} and stores a mapping for every
   * productType to id in the cached map of keys -&gt; ids and returns this cached map.
   *
   * <p>Note: If all the supplied keys are already cached, the cached map is returned right away
   * with no request to CTP.
   *
   * @param keys the productType keys to fetch and cache the ids for.
   * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion
   *     contains a map of all productType keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> keys);

  /**
   * Given a {@code key}, this method first checks if a cached map of ProductType keys -&gt; ids
   * contains the key. If not, it returns a completed future that contains an {@link Optional} that
   * contains what this key maps to in the cache. If the cache doesn't contain the key; this method
   * attempts to fetch the id of the key from the CTP project, caches it and returns a {@link
   * CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
   * completion could contain an {@link Optional} with the id inside of it or an empty {@link
   * Optional} if no {@link ProductType} was found in the CTP project with this key.
   *
   * @param key the key by which a {@link ProductType} id should be fetched from the CTP project.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of its completion could contain an {@link Optional} with the id inside of it or an
   *     empty {@link Optional} if no {@link ProductType} was found in the CTP project with this
   *     key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedProductTypeId(@Nonnull String key);

  /**
   * Given a {@code productType}, this method first checks if a cached map of ProductType ids -&gt;
   * map of {@link AttributeMetaData} is not empty. If not, it returns a completed future that
   * contains an optional that contains what this productType id maps to in the cache. If the cache
   * is empty, the method populates the cache with the mapping of all ProductType ids to maps of
   * each product type's attributes' {@link AttributeMetaData} in the CTP project, by querying the
   * CTP project for all ProductTypes.
   *
   * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link
   * String}&gt;&gt; in which the result of it's completion could contain an {@link Optional} with a
   * map of the attributes names -&gt; {@link AttributeMetaData} inside of it or an empty {@link
   * Optional} if no {@link ProductType} was found in the CTP project with this id.
   *
   * @param productTypeId the id by which a a map of the attributes names -&gt; {@link
   *     AttributeMetaData} corresponding to the product type should be fetched from the CTP
   *     project.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of its completion could contain an {@link Optional} with the id inside of it or an
   *     empty {@link Optional} if no {@link ProductType} was found in the CTP project with this
   *     key.
   */
  @Nonnull
  CompletionStage<Optional<Map<String, AttributeMetaData>>> fetchCachedProductAttributeMetaDataMap(
      @Nonnull String productTypeId);

  /**
   * Given a {@link Set} of ProductType keys, this method fetches a set of all the ProductTypes,
   * matching this given set of keys in the CTP project, defined in an injected {@link
   * io.sphere.sdk.client.SphereClient}. A mapping of the key to the id of the fetched ProductType
   * is persisted in an in-memory map.
   *
   * @param keys set of ProductType keys to fetch matching ProductTypes by.
   * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion
   *     contains a {@link Set} of all matching ProductType.
   */
  @Nonnull
  CompletionStage<Set<ProductType>> fetchMatchingProductTypesByKeys(@Nonnull Set<String> keys);

  /**
   * Given a resource draft of type {@link ProductTypeDraft}, this method attempts to create a
   * resource {@link ProductType} based on it in the CTP project defined by the sync options.
   *
   * <p>A completion stage containing an empty optional and the error callback will be triggered in
   * those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the resource gets created successfully on CTP, then the created
   * resource's id and key are cached and the method returns a {@link CompletionStage} in which the
   * result of it's completion contains an instance {@link Optional} of the resource which was
   * created.
   *
   * @param productTypeDraft the resource draft to create a resource based off of.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<ProductType>> createProductType(
      @Nonnull ProductTypeDraft productTypeDraft);

  /**
   * Updates existing product type with {@code updateActions}.
   *
   * @param productType product type that should be updated
   * @param updateActions {@link List} of actions that should be applied to {@code productType}
   * @return {@link CompletionStage} with updated {@link ProductType}.
   */
  @Nonnull
  CompletionStage<ProductType> updateProductType(
      @Nonnull ProductType productType, @Nonnull List<UpdateAction<ProductType>> updateActions);

  /**
   * Given a productType key, this method fetches a productType that matches this given key in the
   * CTP project defined in an injected {@link SphereClient}. If there is no matching productType an
   * empty {@link Optional} will be returned in the returned future.
   *
   * @param key the key of the product type to fetch.
   * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion
   *     contains an {@link Optional} that contains the matching {@link ProductType} if exists,
   *     otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<ProductType>> fetchProductType(@Nullable String key);
}
