package com.commercetools.sync.services;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface CategoryService {

    /**
     * If not already done once before, it fetches all the category keys from the CTP project defined in a potentially
     * injected {@link io.sphere.sdk.client.SphereClient} and stores a mapping for every category to id in {@link Map}
     * and returns this cached map.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     *         category keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds();

    /**
     * Given a {@link Set} of category keys, this method fetches a set of all the categories matching this given set of
     * keys in the CTP project defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}.
     *
     * @param categoryKeys set of category keys to fetch matching categories by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all matching categories.
     */
    @Nonnull
    CompletionStage<Set<Category>> fetchMatchingCategoriesByKeys(@Nonnull final Set<String> categoryKeys);

    /**
     * Given a category key, this method fetches a category that matches this given key in the CTP project defined in a
     * potentially injected {@link SphereClient}. If there is no matching category an empty {@link Optional} will be
     * returned in the returned future.
     *
     * @param key the key of the category to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@link Category} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<Category>> fetchCategory(@Nonnull final String key);

    /**
     * Given a {@link Set} of categoryDrafts, this method creates Categories corresponding to them in the CTP project
     * defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}.
     *
     * @param categoryDrafts set of categoryDrafts to create on the CTP project.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all created categories.
     */
    @Nonnull
    CompletionStage<Set<Category>> createCategories(@Nonnull final Set<CategoryDraft> categoryDrafts);

    /**
     * Given a {@code key}, this method first checks if cached map of category keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all categories' keys
     * to ids in the CTP project. After that, the method returns a
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
    CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key);

    /**
     * Given a {@link CategoryDraft}, this method creates a {@link Category} based on it in the CTP project defined in
     * a potentially injected {@link io.sphere.sdk.client.SphereClient}. The created category's id and key are also
     * cached. This method returns {@link CompletionStage}&lt;{@link Category}&gt; in which the result of it's
     * completion contains an instance of the {@link Category} which was created in the CTP project.
     *
     * @param categoryDraft the {@link CategoryDraft} to create a {@link Category} based off of.
     * @return {@link CompletionStage}&lt;{@link Category}&gt; containing as a result of it's completion an instance of
     *         the {@link Category} which was created in the CTP project or a
     *         {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Optional<Category>> createCategory(@Nonnull final CategoryDraft categoryDraft);

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
    CompletionStage<Category> updateCategory(@Nonnull final Category category,
                                             @Nonnull final List<UpdateAction<Category>> updateActions);
}
