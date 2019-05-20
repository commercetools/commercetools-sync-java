package com.commercetools.sync.services.impl;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.services.CartDiscountService;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommand;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class CartDiscountServiceImpl extends BaseService<CartDiscountDraft, CartDiscount, CartDiscountSyncOptions>
        implements CartDiscountService {
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    CartDiscountServiceImpl(@Nonnull CartDiscountSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds() {
        return null;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // @TODO change the below name comparison to Key equility
        final CartDiscountQuery cartDiscountQuery = CartDiscountQuery
                .of().plusPredicates(cartDiscountQueryModel ->
                        cartDiscountQueryModel.name().lang(Locale.getDefault()).is(key));
        return syncOptions
                .getCtpClient()
                .execute(cartDiscountQuery)
                .thenApply(cartDiscountPagedQueryResult ->
                        cartDiscountPagedQueryResult
                                .head()
                                .map(cartDiscount -> {
                                    // @TODO change the below name comparison to Key equility
                                    keyToIdCache.put(cartDiscount.getName().toString(), cartDiscount.getId());
                                    return cartDiscount;
                                }));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CartDiscount>> createCartDiscount(@Nonnull CartDiscountDraft cartDiscountDraft) {
        // @TODO remove below lambda, replace it with CartDiscountDraft::getKey
        return createResource(cartDiscountDraft, cd -> cd.getName().toString(), CartDiscountCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<CartDiscount> updateCartDiscount(@Nonnull CartDiscount cartDiscount,
                                                            @Nonnull List<UpdateAction<CartDiscount>> updateActions) {
        return updateResource(cartDiscount, CartDiscountUpdateCommand::of, updateActions);
    }
}
