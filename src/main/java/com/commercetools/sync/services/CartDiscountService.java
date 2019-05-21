package com.commercetools.sync.services;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.SphereException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface CartDiscountService {
    /**
     * If not already done once before, it fetches all the cart discount keys from the CTP project defined in an
     * injected {@link io.sphere.sdk.client.SphereClient} and stores a mapping for every cart discount to id
     * in {@link Map} and returns this cached map.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     * cart discount keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds();

    /**
     * Given a {@link Set} of CartDiscount keys, this method fetches a set of all the CartDiscounts, matching this given
     * set of keys in the CTP project, defined in an injected {@link SphereClient}. A
     * mapping of the key to the id of the fetched CartDiscount is persisted in an in-memory map.
     *
     * @param keys set of CartDiscounts keys to fetch matching CartDiscount by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of its completion contains a {@link Set}
     *          of all matching CartDiscounts.
     */
    @Nonnull
    CompletionStage<Set<CartDiscount>> fetchMatchingCartDiscountsByKeys(@Nonnull final Set<String> keys);


    /**
     * Given a cart discount key, this method fetches a cart discount that matches this given key in the CTP project
     * defined in a potentially injected {@link SphereClient}. If there is no matching cart discount an empty
     * {@link Optional} will be returned in the returned future. A mapping of the key to the id of the fetched cart
     * discount is persisted in an in-memory map.
     *
     * @param key the key of the product to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     * {@link Optional} that contains the matching {@link CartDiscount} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable final String key);

    /**
     * Given a resource draft of type {@link CartDiscountDraft}, this method attempts to create a resource
     * {@link CartDiscount} based on it in the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty option and the error callback will be triggered in those cases:
     * <ul>
     * <li>the draft has a blank key</li>
     * <li>the create request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets created successfully on CTP, then the created resource's id and
     * key are cached and the method returns a {@link CompletionStage} in which the result of it's completion
     * contains an instance {@link Optional} of the resource which was created.
     *
     * @param cartDiscountDraft the resource draft to create a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     * empty optional.
     */
    @Nonnull
    CompletionStage<Optional<CartDiscount>> createCartDiscount(@Nonnull final CartDiscountDraft cartDiscountDraft);

    /**
     * Given a {@link CartDiscount} and a {@link List}&lt;{@link UpdateAction}&lt;{@link CartDiscount}&gt;&gt;, this
     * method issues an update request with these update actions on this {@link CartDiscount} in the CTP project
     * defined in a potentially injected {@link SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link CartDiscount}&gt; in which the result of it's completion contains an instance
     * of the {@link CartDiscount} which was updated in the CTP project.
     *
     * @param cartDiscount  the {@link CartDiscount} to update.
     * @param updateActions the update actions to update the {@link CartDiscount} with.
     * @return {@link CompletionStage}&lt;{@link CartDiscount}&gt; containing as a result of it's completion an
     * instance of the {@link CartDiscount} which was updated in the CTP project or a
     * {@link SphereException}.
     */
    @Nonnull
    CompletionStage<CartDiscount> updateCartDiscount(@Nonnull final CartDiscount cartDiscount,
                                                     @Nonnull final List<UpdateAction<CartDiscount>> updateActions);
}
