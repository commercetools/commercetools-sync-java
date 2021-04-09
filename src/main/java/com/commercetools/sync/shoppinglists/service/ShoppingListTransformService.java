package com.commercetools.sync.shoppinglists.service;

import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface ShoppingListTransformService {

  /**
   * Transforms ShoppingLists by resolving the references and map them to ShoppingListDrafts.
   *
   * <p>This method resolves(fetch key values for the reference id's) non null and unexpanded
   * references of the ShoppingList{@link ShoppingList} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the ShoppingList to ShoppingListDraft by performing reference resolution
   * considering idToKey value from the cache.
   *
   * @param shoppingLists the shoppingLists to resolve the references.
   * @return a new list which contains ShoppingListDrafts which have all their references resolved.
   */
  @Nonnull
  CompletableFuture<List<ShoppingListDraft>> toShoppingListDrafts(
      @Nonnull List<ShoppingList> shoppingLists);
}
