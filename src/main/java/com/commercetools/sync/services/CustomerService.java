package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CustomerService {

  /**
   * Given a set of keys this method caches in-memory a mapping of key -&gt; id only for those keys
   * which are not already cached.
   *
   * @param keysToCache a set of keys to cache.
   * @return a map of key to ids of the requested keys.
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> keysToCache);

  /**
   * Given a {@link Set} of customer keys, this method fetches a set of all the customers, matching
   * the given set of keys in the CTP project, defined in an injected {@link SphereClient}. A
   * mapping of the key to the id of the fetched customers is persisted in an in-memory map.
   *
   * @param customerKeys set of customer keys to fetch matching resources by.
   * @return {@link CompletionStage}&lt;{@link Set}&lt;{@link Customer}&gt;&gt; in which the result
   *     of it's completion contains a {@link Set} of all matching customers.
   */
  @Nonnull
  CompletionStage<Set<Customer>> fetchMatchingCustomersByKeys(@Nonnull Set<String> customerKeys);

  /**
   * Given a customer key, this method fetches a customer that matches this given key in the CTP
   * project defined in a potentially injected {@link SphereClient}. If there is no matching
   * resource an empty {@link Optional} will be returned in the returned future. A mapping of the
   * key to the id of the fetched customer is persisted in an in -memory map.
   *
   * @param key the key of the resource to fetch
   * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion
   *     contains an {@link Optional} that contains the matching {@link Customer} if exists,
   *     otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<Customer>> fetchCustomerByKey(@Nullable String key);

  /**
   * Given a {@code key}, if it is blank (null/empty), a completed future with an empty optional is
   * returned. Otherwise this method checks if the cached map of resource keys -&gt; ids contains
   * the given key. If it does, an optional containing the matching id is returned. If the cache
   * doesn't contain the key; this method attempts to fetch the id of the key from the CTP project,
   * caches it and returns a {@link CompletionStage}&lt; {@link Optional}&lt;{@link String}&gt;&gt;
   * in which the result of it's completion could contain an {@link Optional} holding the id or an
   * empty {@link Optional} if no customer was found in the CTP project with this key.
   *
   * @param key the key by which a customer id should be fetched from the CTP project.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of it's completion could contain an {@link Optional} holding the id or an empty
   *     {@link Optional} if no customer was found in the CTP project with this key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedCustomerId(@Nonnull String key);

  /**
   * Given a resource draft of type {@link CustomerDraft}, this method attempts to create a resource
   * {@link Customer} based on the draft, in the CTP project defined by the sync options.
   *
   * <p>A completion stage containing an empty option and the error callback will be triggered in
   * those cases:
   *
   * <ul>
   *   <li>the draft has a blank key
   *   <li>the create request fails on CTP
   * </ul>
   *
   * <p>On the other hand, if the resource gets created successfully on CTP, the created resource's
   * id and key are cached and the method returns a {@link CompletionStage} in which the result of
   * it's completion contains an instance {@link Optional} of the resource which was created.
   *
   * @param customerDraft the resource draft to create a resource based off of.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<Customer>> createCustomer(@Nonnull CustomerDraft customerDraft);

  /**
   * Given a {@link Customer} and a {@link List}&lt;{@link UpdateAction}&lt;{@link
   * Customer}&gt;&gt;, this method issues an update request with these update actions on this
   * {@link Customer} in the CTP project defined in a potentially injected {@link SphereClient}.
   * This method returns {@link CompletionStage}&lt;{@link Customer}&gt; in which the result of it's
   * completion contains an instance of the {@link Customer} which was updated in the CTP project.
   *
   * @param customer the {@link Customer} to update.
   * @param updateActions the update actions to update the {@link Customer} with.
   * @return {@link CompletionStage}&lt;{@link Customer}&gt; containing as a result of it's
   *     completion an instance of the {@link Customer} which was updated in the CTP project or a
   *     {@link io.sphere.sdk.models.SphereException}.
   */
  @Nonnull
  CompletionStage<Customer> updateCustomer(
      @Nonnull Customer customer, @Nonnull List<UpdateAction<Customer>> updateActions);
}
