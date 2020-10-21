package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.ShoppingListCreateCommand;
import io.sphere.sdk.shoppinglists.commands.ShoppingListUpdateCommand;
import io.sphere.sdk.shoppinglists.expansion.ShoppingListExpansionModel;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQueryBuilder;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * Implementation of ShoppingListService interface.
 */
public final class ShoppingListServiceImpl extends BaseService<ShoppingListDraft, ShoppingList, ShoppingListSyncOptions,
    ShoppingListQuery, ShoppingListQueryModel, ShoppingListExpansionModel<ShoppingList>>
    implements ShoppingListService {

    public ShoppingListServiceImpl(@Nonnull final ShoppingListSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> shoppingListKeys) {

        return cacheKeysToIds(
            shoppingListKeys, ShoppingList::getKey, keysNotCached -> ShoppingListQueryBuilder
                .of()
                .plusPredicates(shoppingListQueryModel -> shoppingListQueryModel.key().isIn(keysNotCached))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Set<ShoppingList>> fetchMatchingShoppingListsByKeys(@Nonnull final Set<String> keys) {

        return fetchMatchingResources(keys, ShoppingList::getKey,
            () -> ShoppingListQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<ShoppingList>> fetchShoppingList(@Nullable final String key) {

        return fetchResource(key,
            () -> ShoppingListQueryBuilder.of().plusPredicates(queryModel -> queryModel.key().is(key)).build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<ShoppingList>> createShoppingList(
        @Nonnull final ShoppingListDraft shoppingListDraft) {

        return createResource(shoppingListDraft, ShoppingListDraft::getKey, ShoppingListCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<ShoppingList> updateShoppingList(
        @Nonnull final ShoppingList shoppingList,
        @Nonnull final List<UpdateAction<ShoppingList>> updateActions) {

        return updateResource(shoppingList, ShoppingListUpdateCommand::of, updateActions);
    }
}
