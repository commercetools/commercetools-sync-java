package com.commercetools.sync.services;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface CategoryService {

    /**
     * If not already done once before, it fetches all the category keys from the CTP project defined in an
     * injected {@link io.sphere.sdk.client.SphereClient} and stores a mapping for every category to id in {@link Map}
     * and returns this cached map.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     *         category keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds();

    /**
     * Given a {@link Set} of category keys, this method fetches a set of all the categories, matching this given set of
     * keys in the CTP project, defined in an injected {@link io.sphere.sdk.client.SphereClient}. A mapping
     * of the key to the id of the fetched categories is persisted in an in-memory map.
     *
     * @param categoryKeys set of category keys to fetch matching categories by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all matching categories.
     */
    @Nonnull
    CompletionStage<Set<Category>> fetchMatchingCategoriesByKeys(@Nonnull Set<String> categoryKeys);

    /**
     * Given a category key, this method fetches a category that matches this given key in the CTP project defined in a
     * potentially injected {@link SphereClient}. If there is no matching category an empty {@link Optional} will be
     * returned in the returned future. A mapping of the key to the id of the fetched category is persisted in an in
     * -memory map.
     *
     * @param key the key of the category to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@link Category} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<Category>> fetchCategory(@Nullable String key);

    /**
     * Given a {@code key}, this method first checks if cached map of category keys -&gt; ids contains the key.
     * If it does, then an optional containing the mapped id is returned. If the cache doesn't contain the key; this
     * method attempts to fetch the id of the key from the CTP project, caches it and returns a
     * {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's completion
     * could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Category}
     * was found in the CTP project with this key.
     *
     * @param key the key by which a {@link Category} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link Category} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull String key);

    /**
     * Given a resource draft of type {@link CategoryDraft}, this method attempts to create a resource
     * {@link Category} based on it in the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty option and the error callback will be triggered in those cases:
     * <ul>
     *     <li>the draft has a blank key</li>
     *     <li>the create request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets created successfully on CTP, then the created resource's id and
     * key are cached and the method returns a {@link CompletionStage} in which the result of it's completion
     * contains an instance {@link Optional} of the resource which was created.
     *
     * @param categoryDraft the resource draft to create a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *         empty optional.
     */
    @Nonnull
    CompletionStage<Optional<Category>> createCategory(@Nonnull CategoryDraft categoryDraft);

    /**
     * Given a {@link Category} and a {@link List}&lt;{@link UpdateAction}&lt;{@link Category}&gt;&gt;, this method
     * issues an update request with these update actions on this {@link Category} in the CTP project defined in a
     * potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link Category}&gt; in which the result of it's completion contains an instance of
     * the {@link Category} which was updated in the CTP project.
     *
     * @param category      the {@link Category} to update.
     * @param updateActions the update actions to update the {@link Category} with.
     * @return {@link CompletionStage}&lt;{@link Category}&gt; containing as a result of it's completion an instance of
     *         the {@link Category} which was updated in the CTP project or a
     *         {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Category> updateCategory(@Nonnull Category category,
                                             @Nonnull List<UpdateAction<Category>> updateActions);
}