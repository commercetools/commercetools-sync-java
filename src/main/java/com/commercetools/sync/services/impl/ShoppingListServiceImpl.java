package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyShoppingListsGet;
import com.commercetools.api.client.ByProjectKeyShoppingListsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyShoppingListsPost;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListPagedQueryResponse;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateBuilder;
import com.commercetools.api.predicates.query.shopping_list.ShoppingListQueryBuilderDsl;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Implementation of ShoppingListService interface. */
public final class ShoppingListServiceImpl
    extends BaseService<
        ShoppingListSyncOptions,
        ShoppingList,
        ShoppingListDraft,
        ByProjectKeyShoppingListsGet,
        ShoppingListPagedQueryResponse,
        ByProjectKeyShoppingListsKeyByKeyGet,
        ShoppingList,
        ShoppingListQueryBuilderDsl,
        ByProjectKeyShoppingListsPost>
    implements ShoppingListService {

  public ShoppingListServiceImpl(@Nonnull final ShoppingListSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> shoppingListKeys) {
    return super.cacheKeysToIdsUsingGraphQl(shoppingListKeys, GraphQlQueryResource.SHOPPING_LISTS);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<ShoppingList>> fetchMatchingShoppingListsByKeys(
      @Nonnull final Set<String> keys) {
    return fetchMatchingShoppingLists(keys);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ShoppingList>> fetchShoppingList(@Nullable final String key) {
    return super.fetchResource(
        key,
        syncOptions
            .getCtpClient()
            .shoppingLists()
            .withKey(key)
            .get()
            .addExpand("lineItems[*].variant"));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<ShoppingList>> createShoppingList(
      @Nonnull final ShoppingListDraft shoppingListDraft) {
    return super.createResource(
        shoppingListDraft,
        ShoppingListDraft::getKey,
        ShoppingList::getId,
        Function.identity(),
        () -> syncOptions.getCtpClient().shoppingLists().post(shoppingListDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<ShoppingList> updateShoppingList(
      @Nonnull final ShoppingList shoppingList,
      @Nonnull final List<ShoppingListUpdateAction> updateActions) {

    final List<List<ShoppingListUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<ShoppingList>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, shoppingList));

    for (final List<ShoppingListUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedShoppingList ->
                      syncOptions
                          .getCtpClient()
                          .shoppingLists()
                          .withId(updatedShoppingList.getId())
                          .post(
                              ShoppingListUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedShoppingList.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }

  private CompletionStage<Set<ShoppingList>> fetchMatchingShoppingLists(
      @Nonnull final Set<String> shoppingListKeys) {
    return super.fetchMatchingResources(
        shoppingListKeys,
        ShoppingList::getKey,
        (keysNotCached) -> {
          final ByProjectKeyShoppingListsGet query =
              syncOptions
                  .getCtpClient()
                  .shoppingLists()
                  .get()
                  .withWhere("key in :keys")
                  .withPredicateVar("keys", keysNotCached)
                  .addExpand("lineItems[*].variant");
          return query;
        });
  }
}
