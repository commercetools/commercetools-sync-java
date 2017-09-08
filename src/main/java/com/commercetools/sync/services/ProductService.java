package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface ProductService {
    /**
     * If not already done once before, it fetches all the product keys from the CTP project defined in a potentially
     * injected {@link SphereClient} and stores a mapping for every product to id in {@link Map}
     * and returns this cached map.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     *          product keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds();

    /**
     * Given a {@link Set} of product keys, this method fetches a set of all the products matching this given set of
     * keys in the CTP project defined in a potentially injected {@link SphereClient}.
     *
     * @param productKeys set of product keys to fetch matching products by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all matching products.
     */
    @Nonnull
    CompletionStage<Set<Product>> fetchMatchingProductsByKeys(@Nonnull final Set<String> productKeys);

    /**
     * Given a {@link Set} of productsDrafts, this method creates Products corresponding to them in the CTP project
     * defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}.
     *
     * @param productsDrafts set of productsDrafts to create on the CTP project.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all created products.
     */
    @Nonnull
    CompletionStage<Set<Product>> createProducts(@Nonnull final Set<ProductDraft> productsDrafts);
    
    /**
     * Given a {@link ProductDraft}, this method creates a {@link Product} based on it in the CTP project defined in
     * a potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link Product}&gt; in which the result of it's completion contains an instance of
     * the {@link Product} which was created in the CTP project.
     *
     * @param productDraft the {@link ProductDraft} to create a {@link Product} based off of.
     * @return {@link CompletionStage}&lt;{@link Product}&gt; containing as a result of it's completion an instance of
     *          the {@link Product} which was created in the CTP project or a
     *          {@link io.sphere.sdk.models.SphereException}.
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

    /**
     * Given a {@link Product}, this method issues an update request to publish this {@link Product} in the CTP project
     * defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link Product}&gt; in which the result of it's completion contains an instance of
     * the {@link Product} which was published in the CTP project.
     *
     * @param product the {@link Product} to publish.
     * @return {@link CompletionStage}&lt;{@link Product}&gt; containing as a result of it's completion an instance of
     *          the {@link Product} which was published in the CTP project or a
     *          {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Product> publishProduct(@Nonnull final Product product);

    /**
     * Given a {@link Product}, this method issues an update request to revert the staged changes of this
     * {@link Product} in the CTP project defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}.
     * This method returns {@link CompletionStage}&lt;{@link Product}&gt; in which the result of it's completion
     * contains an instance of the {@link Product} which had its staged changes reverted in the CTP project.
     *
     * @param product the {@link Product} to revert the staged changes for.
     * @return {@link CompletionStage}&lt;{@link Product}&gt; containing as a result of it's completion an instance of
     *          the {@link Product} which was published in the CTP project or a
     *          {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Product> revertProduct(@Nonnull final Product product);
}
