package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.DraftBasedCreateCommand;
import io.sphere.sdk.models.Resource;
import io.sphere.sdk.models.ResourceView;
import io.sphere.sdk.models.WithKey;
import io.sphere.sdk.queries.MetaModelQueryDsl;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @param <T> Resource Draft (e.g. {@link io.sphere.sdk.products.ProductDraft}, {@link
 *     io.sphere.sdk.categories.CategoryDraft}, etc..
 * @param <U> Resource (e.g. {@link io.sphere.sdk.products.Product}, {@link
 *     io.sphere.sdk.categories.Category}, etc..
 * @param <UQ> Resource returned by the query <Q> (e.g. {@link
 *     io.sphere.sdk.products.ProductProjection})
 * @param <S> Subclass of {@link BaseSyncOptions}
 * @param <Q> Query (e.g. {@link io.sphere.sdk.products.queries.ProductQuery}, {@link
 *     io.sphere.sdk.categories.queries.CategoryQuery}, etc..
 * @param <M> Query Model (e.g. {@link io.sphere.sdk.products.queries.ProductQueryModel}, {@link
 *     io.sphere.sdk.categories.queries.CategoryQueryModel}, etc..
 * @param <E> Expansion Model (e.g. {@link io.sphere.sdk.products.expansion.ProductExpansionModel},
 *     {@link io.sphere.sdk.categories.expansion.CategoryExpansionModel}, etc..
 */
abstract class BaseServiceWithKey<
        T extends WithKey,
        U extends Resource<U> & WithKey,
        UQ extends ResourceView<UQ, U> & WithKey,
        S extends BaseSyncOptions,
        Q extends MetaModelQueryDsl<UQ, Q, M, E>,
        M,
        E>
    extends BaseService<T, U, UQ, S, Q, M, E> {

  BaseServiceWithKey(@Nonnull final S syncOptions) {
    super(syncOptions);
  }

  /**
   * Given a resource draft of type {@code T}, this method attempts to create a resource {@code U}
   * based on it in the CTP project defined by the sync options.
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
   * resource's id and key are cached and the method returns a {@link CompletionStage} in which the
   * result of it's completion contains an instance {@link Optional} of the resource which was
   * created.
   *
   * @param draft the resource draft to create a resource based off of.
   * @param createCommand a function to get the create command using the supplied draft.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<U>> createResource(
      @Nonnull final T draft,
      @Nonnull final Function<T, DraftBasedCreateCommand<U, T>> createCommand) {

    return super.createResource(draft, T::getKey, createCommand);
  }

  /**
   * Given a {@code key}, if it is blank (null/empty), a completed future with an empty optional is
   * returned. This method then checks if the cached map of resource keys -&gt; ids contains the
   * key. If it does, then an optional containing the mapped id is returned. If the cache doesn't
   * contain the key; this method attempts to fetch the id of the key from the CTP project, caches
   * it and returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which
   * the result of it's completion could contain an {@link Optional} with the id inside of it or an
   * empty {@link Optional} if no resource was found in the CTP project with this key.
   *
   * @param key the key by which a resource id should be fetched from the CTP project.
   * @param querySupplier supplies the query to fetch the resource with the given key.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of it's completion could contain an {@link Optional} with the id inside of it or an
   *     empty {@link Optional} if no resource was found in the CTP project with this key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key, @Nonnull final Supplier<Q> querySupplier) {

    // Why method reference is not used:
    // http://mail.openjdk.java.net/pipermail/compiler-dev/2015-November/009824.html
    return super.fetchCachedResourceId(key, resource -> resource.getKey(), querySupplier);
  }

  /**
   * Given a set of keys this method caches a mapping of the keys to ids of such keys only for the
   * keys which are not already in the cache.
   *
   * @param keys keys to cache.
   * @param keysRequestMapper function that accepts a set of keys which are not cached and maps it
   *     to a graphQL request object representing the graphql query to CTP on such keys.
   * @return a map of key to ids of the requested keys.
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> keys,
      @Nonnull final Function<Set<String>, ResourceKeyIdGraphQlRequest> keysRequestMapper) {

    // Why method reference is not used:
    // http://mail.openjdk.java.net/pipermail/compiler-dev/2015-November/009824.html
    return super.cacheKeysToIdsUsingGraphQl(
        keys,
        resource -> Collections.singletonMap(resource.getKey(), resource.getId()),
        keysRequestMapper);
  }

  /**
   * Given a {@link Set} of resource keys, this method fetches a set of all the resources, matching
   * this given set of keys in the CTP project, defined in an injected {@link SphereClient}. A
   * mapping of the key to the id of the fetched resources is persisted in an in-memory map.
   *
   * @param keys set of state keys to fetch matching states by
   * @param querySupplier supplies the query to fetch the resources with the given keys.
   * @return {@link CompletionStage}&lt;{@link Set}&lt;{@code U}&gt;&gt; in which the result of it's
   *     completion contains a {@link Set} of all matching resources.
   */
  @Nonnull
  CompletionStage<Set<UQ>> fetchMatchingResources(
      @Nonnull final Set<String> keys, @Nonnull final Function<Set<String>, Q> querySupplier) {

    // Why method reference is not used:
    // http://mail.openjdk.java.net/pipermail/compiler-dev/2015-November/009824.html
    return super.fetchMatchingResources(keys, resource -> resource.getKey(), querySupplier);
  }
}
