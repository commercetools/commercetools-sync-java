package com.commercetools.sync.sdk2.services;

import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
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
   * from the CTP project defined in an injected {@link com.commercetools.api.client.ProjectApiRoot}
   * and stores a mapping for every tax category to id in the cached map of keys -&gt; ids and
   * returns this cached map.
   *
   * @param taxCategoryKeys - a set of tax category keys to fetch and cache the ids for
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a map of requested tax category keys -&gt; ids
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
   * <p>After that, the method returns a {@link java.util.concurrent.CompletionStage}&lt;{@link
   * java.util.Optional}&lt;{@link String}&gt;&gt; in which the result of it's completion could
   * contain an {@link java.util.Optional} with the id inside of it or an empty {@link
   * java.util.Optional} if no {@link TaxCategory} was found in the CTP project with this key.
   *
   * @param key the key by which a {@link TaxCategory} id should be fetched from the CTP project.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&lt;{@link
   *     String}&gt;&gt; in which the result of its completion could contain an {@link
   *     java.util.Optional} with the id inside of it or an empty {@link java.util.Optional} if no
   *     {@link TaxCategory} was found in the CTP project with this key.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedTaxCategoryId(@Nullable final String key);

  /**
   * Given a {@link java.util.Set} of tax category keys, this method fetches a set of all the
   * taxCategories, matching given set of keys in the CTP project, defined in an injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. A mapping of the key to the id of the fetched
   * taxCategories is persisted in an in-memory map. <br>
   * One must remember key is not required to create TaxCategory but is required to synchronize tax
   * categories.
   *
   * @param keys set of tax category keys to fetch matching taxCategories by.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a {@link java.util.Set} of all matching taxCategories.
   */
  @Nonnull
  CompletionStage<Set<TaxCategory>> fetchMatchingTaxCategoriesByKeys(
      @Nonnull final Set<String> keys);

  /**
   * Given a tax category key, this method fetches a tax category that matches given key in the CTP
   * project defined in a potentially injected {@link com.commercetools.api.client.ProjectApiRoot}.
   * If there is no matching tax category an empty {@link java.util.Optional} will be returned in
   * the returned future. A mapping of the key to the id of the fetched category is persisted in an
   * in-memory map.
   *
   * @param key the key of the tax category to fetch.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&gt; in which
   *     the result of it's completion contains an {@link java.util.Optional} that contains the
   *     matching {@link TaxCategory} if exists, otherwise empty.
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
   * resource's id and key are cached and the method returns a {@link
   * java.util.concurrent.CompletionStage} in which the result of it's completion contains an
   * instance {@link java.util.Optional} of the resource which was created.
   *
   * @param taxCategoryDraft the resource draft to create a resource based off of.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<TaxCategory>> createTaxCategory(
      @Nonnull final TaxCategoryDraft taxCategoryDraft);

  /**
   * Given a {@link TaxCategory} and a {@link java.util.List}&lt;{@link
   * com.commercetools.api.models.tax_category.TaxCategoryUpdateAction}&lt;{@link
   * TaxCategory}&gt;&gt;, this method issues an update request with these update actions on this
   * {@link TaxCategory} in the CTP project defined in a potentially injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. This method returns {@link
   * java.util.concurrent.CompletionStage}&lt;{@link TaxCategory}&gt; in which the result of it's
   * completion contains an instance of the {@link TaxCategory} which was updated in the CTP
   * project.
   *
   * @param taxCategory the {@link TaxCategory} to update.
   * @param updateActions the update actions to update the {@link TaxCategory} with.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link TaxCategory}&gt; containing as a
   *     result of it's completion an instance of the {@link TaxCategory} which was updated in the
   *     CTP project or a {@link java.util.concurrent.CompletionException}.
   */
  @Nonnull
  CompletionStage<TaxCategory> updateTaxCategory(
      @Nonnull final TaxCategory taxCategory,
      @Nonnull final List<TaxCategoryUpdateAction> updateActions);
}
