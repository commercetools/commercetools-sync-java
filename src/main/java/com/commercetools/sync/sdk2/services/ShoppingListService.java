package com.commercetools.sync.sdk2.services;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ShoppingListService {

  /**
   * Filters out the keys which are already cached and fetches only the not-cached shopping list
   * keys from the CTP project defined in an injected {@link
   * com.commercetools.api.client.ProjectApiRoot} and stores a mapping for every shopping list to id
   * in the cached map of keys -&gt; ids and returns this cached map.
   *
   * <p>Note: If all the supplied keys are already cached, the cached map is returned right away
   * with no request to CTP.
   *
   * @param shoppingListKeys the shopping list keys to fetch and cache the ids for.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a map of all shopping list keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull Set<String> shoppingListKeys);

  /**
   * Given a {@link java.util.Set} of shopping list keys, this method fetches a set of all the
   * shopping lists, matching given set of keys in the CTP project, defined in an injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. A mapping of the key to the id of the fetched
   * shopping lists is persisted in an in-memory map.
   *
   * @param keys set of shopping list keys to fetch matching shopping lists by.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Map}&gt; in which the
   *     result of it's completion contains a {@link java.util.Set} of all matching shopping lists.
   */
  @Nonnull
  CompletionStage<Set<ShoppingList>> fetchMatchingShoppingListsByKeys(
      @Nonnull final Set<String> keys);

  /**
   * Given a shopping list key, this method fetches a shopping list that matches given key in the
   * CTP project defined in a potentially injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. If there is no matching shopping list, an empty
   * {@link java.util.Optional} will be returned in the returned future. A mapping of the key to the
   * id of the fetched shopping list is persisted in an in-memory map.
   *
   * @param key the key of the shopping list to fetch.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link java.util.Optional}&gt; in which
   *     the result of its completion contains an {@link java.util.Optional} that contains the
   *     matching {@link ShoppingList} if exists, otherwise empty.
   */
  @Nonnull
  CompletionStage<Optional<ShoppingList>> fetchShoppingList(@Nullable final String key);

  /**
   * Given a resource draft of type {@link ShoppingListDraft}, this method attempts to create a
   * resource {@link ShoppingList} based on it in the CTP project defined by the sync options.
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
   * @param shoppingListDraft the resource draft to create a resource based off of.
   * @return a {@link java.util.concurrent.CompletionStage} containing an optional with the created
   *     resource if successful otherwise an empty optional.
   */
  @Nonnull
  CompletionStage<Optional<ShoppingList>> createShoppingList(
      @Nonnull final ShoppingListDraft shoppingListDraft);

  /**
   * Given a {@link ShoppingList} and a {@link java.util.List}&lt;{@link
   * ShoppingListUpdateAction}&gt;, this method issues an update request with these update actions
   * on this {@link ShoppingList} in the CTP project defined in a potentially injected {@link
   * com.commercetools.api.client.ProjectApiRoot}. This method returns {@link
   * java.util.concurrent.CompletionStage}&lt; {@link ShoppingList}&gt; in which the result of it's
   * completion contains an instance of the {@link ShoppingList} which was updated in the CTP
   * project.
   *
   * @param shoppingList the {@link ShoppingList} to update.
   * @param updateActions the update actions to update the {@link ShoppingList} with.
   * @return {@link java.util.concurrent.CompletionStage}&lt;{@link ShoppingList}&gt; containing as
   *     a result of it's completion an instance of the {@link ShoppingList} which was updated in
   *     the CTP project or a {@link java.util.concurrent.CompletionException}.
   */
  @Nonnull
  CompletionStage<ShoppingList> updateShoppingList(
      @Nonnull final ShoppingList shoppingList,
      @Nonnull final List<ShoppingListUpdateAction> updateActions);
}
