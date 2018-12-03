package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface ProductService {

    /**
     * Given a {@code key}, if it is blank (null/empty), a completed future with an empty optional is returned.
     * This method then checks if the cached map of product keys -&gt; ids contains the key. If it does, then an
     * optional containing the mapped id is returned. If the cache doesn't contain the key; this method attempts to
     * fetch the id of the key from the CTP project, caches it and returns a
     * {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's completion
     * could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Product}
     * was found in the CTP project with this key.
     *
     * @param key the key by which a {@link Product} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link Product} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> getIdFromCacheOrFetch(@Nullable final String key);

    /**
     * Filters out the keys which are already cached and fetches only the not-cached product keys from the CTP project
     * defined in a potentially injected {@link SphereClient} and stores a mapping for every product to id in
     * the cached map of keys -&gt; ids and returns this cached map.
     *
     * <p>Note: If all the supplied keys are already cached, the cached map is returned right away with no request to
     * CTP.
     *
     * @param productKeys the product keys to fetch and cache the ids for.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     *          product keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> productKeys);

    /**
     * Given a {@link Set} of product keys, this method fetches a set of all the products, matching this given set of
     * keys in the CTP project, defined in a potentially injected {@link SphereClient}. A mapping of the key to the id
     * of the fetched products is persisted in an in-memory map.
     *
     * @param productKeys set of product keys to fetch matching products by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all matching products.
     */
    @Nonnull
    CompletionStage<Set<Product>> fetchMatchingProductsByKeys(@Nonnull final Set<String> productKeys);


    /**
     * Given a product key, this method fetches a product that matches this given key in the CTP project defined in a
     * potentially injected {@link SphereClient}. If there is no matching product an empty {@link Optional} will be
     * returned in the returned future. A mapping of the key to the id of the fetched category is persisted in an in
     * -memory map.
     *
     * @param key the key of the product to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@link Product} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<Product>> fetchProduct(@Nullable final String key);

    /**
     * Given a resource draft of type {@link ProductDraft}, this method attempts to create a resource
     * {@link Product} based on it in the CTP project defined by the sync options.
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
     * @param productDraft the resource draft to create a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *         empty optional.
     */
    @Nonnull
    CompletionStage<Optional<Product>> createProduct(@Nonnull final ProductDraft productDraft);

    /**
     * Given a {@link Product} and a {@link List}&lt;{@link UpdateAction}&lt;{@link Product}&gt;&gt;, this method
     * issues an update request with these update actions on this {@link Product} in the CTP project defined in a
     * potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link Product}&gt; in which the result of it's completion contains an instance of
     * the {@link Product} which was updated in the CTP project.
     *
     * @param product       the {@link Product} to update.
     * @param updateActions the update actions to update the {@link Product} with.
     * @return {@link CompletionStage}&lt;{@link Product}&gt; containing as a result of it's completion an instance of
     *          the {@link Product} which was updated in the CTP project or a
     *          {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Product> updateProduct(@Nonnull final Product product,
                                           @Nonnull final List<UpdateAction<Product>> updateActions);
}