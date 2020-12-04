package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.services.ShoppingListService;
import com.commercetools.sync.shoppinglists.ShoppingListSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.commands.ShoppingListCreateCommand;
import io.sphere.sdk.shoppinglists.commands.ShoppingListUpdateCommand;
import io.sphere.sdk.shoppinglists.expansion.ShoppingListExpansionModel;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQuery;
import io.sphere.sdk.shoppinglists.queries.ShoppingListQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
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

        return cacheKeysToIdsUsingGraphQl(
            shoppingListKeys, resource -> Collections.singletonMap(resource.getKey(), resource.getId()),
            keysNotCached -> new ResourceKeyIdGraphQlRequest(keysNotCached, GraphQlQueryResources.SHOPPING_LISTS));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<ShoppingList>> fetchMatchingShoppingListsByKeys(@Nonnull final Set<String> keys) {

        return fetchMatchingResources(keys, ShoppingList::getKey,
            () -> ShoppingListQuery
                .of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keys))
                .plusExpansionPaths(ExpansionPath.of("lineItems[*].variant")));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<ShoppingList>> fetchShoppingList(@Nullable final String key) {

        return fetchResource(key,
            () -> ShoppingListQuery.of()
                                   .plusPredicates(queryModel -> queryModel.key().is(key))
                                   .plusExpansionPaths(ExpansionPath.of("lineItems[*].variant")));
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
