package com.commercetools.sync.services;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeUpdateAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TypeService {

  /**
   * Filters out the keys which are already cached and fetches only the not-cached type keys from
   * the CTP project defined in an injected {@link ProjectApiRoot} and stores a mapping for every
   * type to id in the cached map of keys -&gt; ids and returns this cached map.
   *
   * @param typeKeys - a set type keys to fetch and cache the ids for
   * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion
   *     contains a map of requested type keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> typeKeys);

  /**
   * Given a {@code key}, this method first checks if a cached map of Type keys -&gt; ids contains
   * the key. If not, it returns a completed future that contains an {@link Optional} that contains
   * what this key maps to in the cache. If the cache doesn't contain the key; this method attempts
   * to fetch the id of the key from the CTP project, caches it and returns a {@link
   * CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
   * completion could contain an {@link Optional} with the id inside of it or an empty {@link
   * Optional} if no {@link Type} was found in the CTP project with this key.
   *
   * @param key the key by which a {@link Type} id should be fetched from the CTP project.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of its completion could contain an {@link Optional} with the id inside of it or an
   *     empty {@link Optional} if no {@link Type} was found in the CTP project with this key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull String key);

  @Nonnull
  CompletionStage<Set<Type>> fetchMatchingTypesByKeys(@Nonnull Set<String> keys);

  /**
   * Given a type key, this method fetches a type that matches this given key in the CTP project
   * defined in an injected {@link ProjectApiRoot}. If there is no matching type an empty {@link
   * Optional} will be returned in the returned future.
   *
   * @param key the key of the type to fetch.
   * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of its completion
   *     contains an {@link Optional} that contains the matching {@link Type} if exists, otherwise
   *     empty.
   */
  @Nonnull
  CompletionStage<Optional<Type>> fetchTypeByKey(@Nullable String key);

  /**
   * Given a resource draft of type {@link TypeDraft}, this method attempts to create a resource
   * {@link Type} based on it in the CTP project defined by the sync options.
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
   * result of its completion contains an instance {@link Optional} of the resource which was
   * created.
   *
   * @param typeDraft the resource draft to create a resource based off of.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<Type>> createType(@Nonnull final TypeDraft typeDraft);

  /**
   * Given a {@link Type} and a {@link List}&lt;{@link TypeUpdateAction}&lt;{@link Type}&gt;&gt;,
   * this method issues an update request with these update actions on this {@link Type} in the CTP
   * project defined in an injected {@link ProjectApiRoot}. This method returns {@link
   * CompletionStage}&lt;{@link Type}&gt; in which the result of its completion contains an instance
   * of the {@link Type} which was updated in the CTP project.
   *
   * @param type the {@link Type} to update.
   * @param updateActions the update actions to update the {@link Type} with.
   * @return {@link CompletionStage}&lt;{@link Type}&gt; containing as a result of it's completion
   *     an instance of the {@link Type} which was updated in the CTP project.
   */
  @Nonnull
  CompletionStage<Type> updateType(
      @Nonnull Type type, @Nonnull List<TypeUpdateAction> updateActions);
}
