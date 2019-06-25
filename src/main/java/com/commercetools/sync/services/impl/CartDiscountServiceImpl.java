package com.commercetools.sync.services.impl;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.CartDiscountService;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQueryBuilder;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CartDiscountServiceImpl extends BaseService<CartDiscountDraft, CartDiscount, CartDiscountSyncOptions>
    implements CartDiscountService {

    public CartDiscountServiceImpl(@Nonnull final CartDiscountSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Set<CartDiscount>> fetchMatchingCartDiscountsByKeys(@Nonnull final Set<String> keys) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        final CartDiscountQuery cartDiscountQuery = CartDiscountQueryBuilder
            .of()
            .plusPredicates(queryModel -> queryModel.key().isIn(keys))
            .build();


        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), cartDiscountQuery, identity())
                            .thenApply(cartDiscounts -> cartDiscounts
                                .stream()
                                .flatMap(List::stream)
                                .peek(cartDiscount -> keyToIdCache.put(cartDiscount.getKey(), cartDiscount.getId()))
                                .collect(toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final CartDiscountQuery cartDiscountQuery = CartDiscountQuery
            .of().plusPredicates(cartDiscountQueryModel -> cartDiscountQueryModel.key().is(key));

        return syncOptions
            .getCtpClient()
            .execute(cartDiscountQuery)
            .thenApply(cartDiscountPagedQueryResult ->
                cartDiscountPagedQueryResult
                    .head()
                    .map(cartDiscount -> {
                        keyToIdCache.put(cartDiscount.getKey(), cartDiscount.getId());
                        return cartDiscount;
                    }));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CartDiscount>> createCartDiscount(
        @Nonnull final CartDiscountDraft cartDiscountDraft) {

        return createResource(cartDiscountDraft, CartDiscountDraft::getKey, CartDiscountCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<CartDiscount> updateCartDiscount(
        @Nonnull final CartDiscount cartDiscount,
        @Nonnull final List<UpdateAction<CartDiscount>> updateActions) {

        return updateResource(cartDiscount, CartDiscountUpdateCommand::of, updateActions);
    }
}
