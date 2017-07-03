package com.commercetools.sync.services;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface CategoryService {
    /**
     * Given a {@code key}, this method first checks if cached map of category keys -> ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all categories' keys
     * to ids in the CTP project. After that, the method returns a
     * {@link CompletionStage&lt;Optional&lt;String&gt;&gt;} in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Category} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link Category} id should be fetched from the CTP project.
     * @return {@link CompletionStage&lt;Optional&lt;String&gt;&gt;} in which the result of it's completion could
     *      contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Category} was
     *      found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedCategoryId(@Nonnull final String key);

    /**
     * Given an {@code key}, this method tries to fetch a {@link Category} with this {@code key} from the
     * CTP project defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage&lt;Optional&lt;Category&gt;&gt;} in which the result of it's completion could contain an
     * {@link Optional} with the {@link Category} inside of it or an empty {@link Optional} if no {@link Category} was
     * found in the CTP project.
     *
     * @param key the key by which a {@link Category} should be fetched from the CTP project.
     * @return {@link CompletionStage&lt;Optional&lt;Category&gt;&gt;} containing as a result of it's completion an
     *      {@link Optional} with the {@link Category} inside of it or an empty {@link Optional} if no {@link Category}
     *      was found in the CTP project, or a {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Optional<Category>> fetchCategoryByKey(@Nonnull final String key);

    /**
     * Given a {@link CategoryDraft}, this method creates a {@link Category} based on it in the CTP project defined in
     * a potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage&lt;Category&gt;} in which the result of it's completion contains an instance of the
     * {@link Category} which was created in the CTP project.
     *
     * @param categoryDraft the {@link CategoryDraft} to create a {@link Category} based off of.
     * @return {@link CompletionStage&lt;Category&gt;}containing as a result of it's completion an instance of the
     *      {@link Category} which was created in the CTP project or a {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Category> createCategory(@Nonnull final CategoryDraft categoryDraft);

    /**
     * Given a {@link Category} and a {@link List&lt;UpdateAction&lt;Category&gt;&gt;}, this method issues an update
     * request with these update actions on this {@link Category} in the CTP project defined in a potentially injected
     * {@link io.sphere.sdk.client.SphereClient}. This method returns {@link CompletionStage&lt;Category&gt;} in which
     * the result of it's completion contains an instance of the {@link Category} which was updated in the CTP project.
     *
     * @param category      the {@link Category} to update.
     * @param updateActions the update actions to update the {@link Category} with.
     * @return {@link CompletionStage&lt;Category&gt;}containing as a result of it's completion an instance of the
     *      {@link Category} which was updated in the CTP project or a {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Category> updateCategory(@Nonnull final Category category,
                                             @Nonnull final List<UpdateAction<Category>> updateActions);

    /**
     * Invalidates the state of the cache so that on the next fetch a fresh copy is fetched and cached.
     */
    void invalidateCache();
}
