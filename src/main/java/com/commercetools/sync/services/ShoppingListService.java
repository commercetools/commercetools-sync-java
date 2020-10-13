package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface ShoppingListService {

    /**
     * Filters out the keys which are already cached and fetches only the not-cached shopping list keys from the CTP
     * project defined in an injected {@link SphereClient} and stores a mapping for every shopping list to id in
     * the cached map of keys -&gt; ids and returns this cached map.
     *
     * <p>Note: If all the supplied keys are already cached, the cached map is returned right away with no request to
     * CTP.
     *
     * @param keys the shopping list keys to fetch and cache the ids for.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     *          shopping list keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> keys);

    /**
     * Given a {@code key}, this method first checks if a cached map of shopping list keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all shopping list keys
     * to ids in the CTP project, by querying the CTP project for all shopping lists.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link ShoppingList} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link ShoppingList} id should be fetched from the CTP
     *            project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link ShoppingList} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedStateId(@Nullable final String key);

    /**
     * Given a {@link Set} of state keys, this method fetches a set of all the states, matching given set of
     * keys in the CTP project, defined in an injected {@link SphereClient}. A mapping of the key to the id
     * of the fetched states is persisted in an in-memory map.
     *
     * @param shoppingListKeys set of shopping list keys to fetch matching shopping lists by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *         of all matching shopping lists.
     */
    @Nonnull
    CompletionStage<Set<ShoppingList>> fetchMatchingShoppingListsByKeys(@Nonnull final Set<String> shoppingListKeys);

    /**
     * Given a shopping list key, this method fetches a shopping list that matches given key in the CTP project defined
     * in a potentially injected {@link SphereClient}. If there is no matching shopping list, an empty {@link Optional}
     * will be returned in the returned future. A mapping of the key to the id of the fetched shopping list is persisted
     * in an in -memory map.
     *
     * @param key the key of the shopping list to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@link ShoppingList} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<ShoppingList>> fetchShoppingList(@Nullable final String key);

    /**
     * Given a resource draft of type {@link ShoppingListDraft}, this method attempts to create a resource
     * {@link ShoppingList} based on it in the CTP project defined by the sync options.
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
     * @param shoppingListDraft the resource draft to create a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *         empty optional.
     */
    @Nonnull
    CompletionStage<Optional<ShoppingList>> createShoppingList(@Nonnull final ShoppingListDraft shoppingListDraft);

    /**
     * Given a {@link ShoppingList} and a {@link List}&lt;{@link UpdateAction}&lt;{@link ShoppingList}&gt;&gt;, this
     * method issues an update request with these update actions on this {@link ShoppingList} in the CTP project defined
     * in a potentially injected {@link SphereClient}. This method returns {
     * @link CompletionStage}&lt;{@link ShoppingList}&gt; in which the result of it's completion contains an instance of
     * the {@link ShoppingList} which was updated in the CTP project.
     *
     * @param shoppingList         the {@link ShoppingList} to update.
     * @param updateActions the update actions to update the {@link ShoppingList} with.
     * @return {@link CompletionStage}&lt;{@link ShoppingList}&gt; containing as a result of it's completion an instance
     *         of the {@link ShoppingList} which was updated in the CTP project or a
     *         {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<ShoppingList> updateShoppingList(@Nonnull final ShoppingList shoppingList,
                                       @Nonnull final List<UpdateAction<ShoppingList>> updateActions);

}
