package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TaxCategoryService {

  /**
   * Filters out the keys which are already cached and fetches only the not-cached tax category keys
   * from the CTP project defined in an injected {@link SphereClient} and stores a mapping for every
   * tax category to id in the cached map of keys -&gt; ids and returns this cached map.
   *
   * @param taxCategoryKeys - a set of tax category keys to fetch and cache the ids for
   * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion
   *     contains a map of requested tax category keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> taxCategoryKeys);

  /**
   * Given a {@code key}, this method first checks if a cached map of TaxCategory keys -&gt; ids is
   * not empty. If not, it returns a completed future that contains an optional that contains what
   * this key maps to in the cache. If the cache is empty, the method populates the cache with the
   * mapping of all TaxCategory keys to ids in the CTP project, by querying the CTP project for all
   * Tax categories.
   *
   * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link
   * String}&gt;&gt; in which the result of it's completion could contain an {@link Optional} with
   * the id inside of it or an empty {@link Optional} if no {@link TaxCategory} was found in the CTP
   * project with this key.
   *
   * @param key the key by which a {@link TaxCategory} id should be fetched from the CTP project.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of its completion could contain an {@link Optional} with the id inside of it or an
   *     empty {@link Optional} if no {@link TaxCategory} was found in the CTP project with this
   *     key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key);

  /**
   * Given a {@link Set} of tax category keys, this method fetches a set of all the taxCategories,
   * matching given set of keys in the CTP project, defined in an injected {@link SphereClient}. A
   * mapping of the key to the id of the fetched taxCategories is persisted in an in-memory map.
   * <br>
   * One must remember key is not required to create TaxCategory but is required to synchronize tax
   * categories.
   *
   * @param keys set of tax category keys to fetch matching taxCategories by.
   * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion
   *     contains a {@link Set} of all matching taxCategories.
   */
  @Nonnull
  CompletionStage<Set<TaxCategory>> fetchMatchingTaxCategoriesByKeys(
      @Nonnull final Set<String> keys);

  /**
   * Given a tax category key, this method fetches a tax category that matches given key in the CTP
   * project defined in a potentially injected {@link SphereClient}. If there is no matching tax
   * category an empty {@link Optional} will be returned in the returned future. A mapping of the
   * key to the id of the fetched category is persisted in an in-memory map.
   *
   * @param key the key of the tax category to fetch.
   * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion
   *     contains an {@link Optional} that contains the matching {@link TaxCategory} if exists,
   *     otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<TaxCategory>> fetchTaxCategory(@Nullable final String key);

  /**
   * Given a resource draft of {@link TaxCategoryDraft}, this method attempts to create a resource
   * {@link TaxCategory} based on it in the CTP project defined by the sync options.
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
   * @param taxCategoryDraft the resource draft to create a resource based off of.
   * @return a {@link CompletionStage} containing an optional with the created resource if
   *     successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<TaxCategory>> createTaxCategory(
      @Nonnull final TaxCategoryDraft taxCategoryDraft);

  /**
   * Given a {@link TaxCategory} and a {@link List}&lt;{@link UpdateAction}&lt;{@link
   * TaxCategory}&gt;&gt;, this method issues an update request with these update actions on this
   * {@link TaxCategory} in the CTP project defined in a potentially injected {@link
   * io.sphere.sdk.client.SphereClient}. This method returns {@link CompletionStage}&lt;{@link
   * TaxCategory}&gt; in which the result of it's completion contains an instance of the {@link
   * TaxCategory} which was updated in the CTP project.
   *
   * @param taxCategory the {@link TaxCategory} to update.
   * @param updateActions the update actions to update the {@link TaxCategory} with.
   * @return {@link CompletionStage}&lt;{@link TaxCategory}&gt; containing as a result of it's
   *     completion an instance of the {@link TaxCategory} which was updated in the CTP project or a
   *     {@link io.sphere.sdk.models.SphereException}.
   */
  @Nonnull
  CompletionStage<TaxCategory> updateTaxCategory(
      @Nonnull final TaxCategory taxCategory,
      @Nonnull final List<UpdateAction<TaxCategory>> updateActions);
}
