package com.commercetools.sync.sdk2.services;

import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public interface CustomObjectService {

  /**
   * Filters out the custom object identifiers which are already cached and fetches only the
   * not-cached custom object identifiers from the CTP project defined in an injected {@link
   * io.sphere.sdk.client.SphereClient} and stores a mapping for every custom object to id in the
   * cached map of keys -&gt; ids and returns this cached map.
   *
   * @param identifiers - a set custom object identifiers to fetch and cache the ids for
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a map of requested custom object identifiers -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers);

  /**
   * Given an {@code identifier}, this method first checks if {@code identifier#toString()} is
   * contained in a cached map of {@link
   * com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier#toString()} -&gt;
   * ids . If it contains, it returns a {@link java.util.concurrent.CompletionStage}&lt;{@link
   * java.util.Optional}&lt;{@link String}&gt;&gt; in which String is what this identifier maps to
   * in the cache. If the cache doesn't contain the identifier, this method attempts to fetch the id
   * of the identifier from the CTP project, caches it and returns a {@link
   * java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&lt;{@link String}&gt;&gt;
   * in which the {@link java.util.Optional} could contain the id inside of it.
   *
   * @param identifier the identifier object containing CustomObject key and container, by which a
   *     {@link io.sphere.sdk.customobjects.CustomObject} id should be fetched from the CTP project.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&lt;{@link
   *     String}&gt;&gt; in which the result of its completion could contain an {@link
   *     java.util.Optional} with the id inside of it or an empty {@link java.util.Optional} if no
   *     {@link io.sphere.sdk.customobjects.CustomObject} was found in the CTP project with this
   *     identifier.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedCustomObjectId(
      @Nonnull CustomObjectCompositeIdentifier identifier);

  /**
   * Given a {@link java.util.Set} of CustomObjectCompositeIdentifier, this method fetches a set of
   * all the CustomObjects, matching this given set of CustomObjectCompositeIdentifiers in the CTP
   * project defined in an injected {@link io.sphere.sdk.client.SphereClient}. A mapping of the
   * CustomObjectCompositeIdentifier to the id of the fetched CustomObject is persisted in an
   * in-memory map.
   *
   * @param identifiers set of CustomObjectCompositeIdentifiers. Each identifier includes key and
   *     container to fetch matching CustomObject.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of its completion contains a {@link java.util.Set} of all matching CustomObjects.
   */
  @Nonnull
  CompletionStage<Set<CustomObject>> fetchMatchingCustomObjects(
      @Nonnull Set<CustomObjectCompositeIdentifier> identifiers);

  /**
   * Given a CustomObjectCompositeIdentifier identify which includes key and container of
   * CustomObject, this method fetches a CustomObject that matches this given identifier in the CTP
   * project defined in an injected {@link io.sphere.sdk.client.SphereClient}. If there is no
   * matching CustomObject an empty {@link java.util.Optional} will be returned in the returned
   * future.
   *
   * @param identifier the identifier of the CustomObject to fetch.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&gt; in which
   *     the result of its completion contains an {@link java.util.Optional} that contains the
   *     matching {@link io.sphere.sdk.customobjects.CustomObject} if exists, otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<CustomObject>> fetchCustomObject(
      @Nonnull CustomObjectCompositeIdentifier identifier);

  /**
   * Given a resource draft of CustomObject {@link io.sphere.sdk.customobjects.CustomObjectDraft},
   * this method attempts to create or update a resource {@link
   * io.sphere.sdk.customobjects.CustomObject} based on it in the CTP project defined by the sync
   * options.
   *
   * <p>A completion stage containing an empty optional and the error callback will be triggered in
   * those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the resource gets created or updated successfully on CTP, then the
   * created resource's id and key/container wrapped by {@link
   * com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier} are cached and
   * the method returns a {@link java.util.concurrent.CompletionStage} in which the result of its
   * completion contains an instance {@link java.util.Optional} of the resource which was created or
   * updated.
   *
   * <p>If an object with the given container/key exists on CTP, the object will be replaced with
   * the new value and the version is incremente.
   *
   * @param customObjectDraft the resource draft to create or update a resource based off of.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the
   *     created/updated resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<CustomObject>> upsertCustomObject(
      @Nonnull CustomObjectDraft customObjectDraft);
}
