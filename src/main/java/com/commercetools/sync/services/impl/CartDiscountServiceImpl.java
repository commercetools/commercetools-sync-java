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
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQueryModel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.queries.QueryPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class CartDiscountServiceImpl extends BaseService<CartDiscountDraft, CartDiscount, CartDiscountSyncOptions>
    implements CartDiscountService {

    public CartDiscountServiceImpl(@Nonnull final CartDiscountSyncOptions syncOptions) {
        super(syncOptions);
    }

    private String getKey(@Nonnull final CartDiscount cartDiscount) {
        //todo: SUPPORT-4443 need to be merged from name to key.
        return cartDiscount.getName().get(Locale.ENGLISH);
    }

    private String getKey(@Nonnull final CartDiscountDraft cartDiscountDraft) {
        //todo: SUPPORT-4443 need to be merged from name to key.
        return cartDiscountDraft.getName().get(Locale.ENGLISH);
    }

    @Nonnull
    private QueryPredicate<CartDiscount> getKeyQuery(@Nonnull final CartDiscountQueryModel cartDiscountQueryModel,
                                                     @Nonnull final String key) {
        //todo: SUPPORT-4443 need to be merged from name to key.
        return cartDiscountQueryModel.name().lang(Locale.ENGLISH).is(key);
    }

    @Nonnull
    private QueryPredicate<CartDiscount> getKeysQuery(@Nonnull final CartDiscountQueryModel cartDiscountQueryModel,
                                                      @Nonnull final Set<String> keys) {
        //todo: SUPPORT-4443 need to be merged from name to key.
        return cartDiscountQueryModel.name().lang(Locale.ENGLISH).isIn(keys);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCartDiscountId(@Nonnull final String key) {
        if (keyToIdCache.containsKey(key)) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return fetchAndCache(key);
    }

    @Nonnull
    private CompletionStage<Optional<String>> fetchAndCache(@Nonnull final String key) {

        final Consumer<List<CartDiscount>> cartDiscountPageConsumer = cartDiscountPage ->
                cartDiscountPage.forEach(cartDiscount -> keyToIdCache.put(getKey(cartDiscount), cartDiscount.getId()));

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), CartDiscountQuery.of(), cartDiscountPageConsumer)
                .thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<CartDiscount>> fetchMatchingCartDiscountsByKeys(@Nonnull final Set<String> keys) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        final CartDiscountQuery cartDiscountQuery = CartDiscountQueryBuilder
            .of()
            .plusPredicates(queryModel -> getKeysQuery(queryModel, keys))
            .build();


        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), cartDiscountQuery, identity())
                            .thenApply(cartDiscounts -> cartDiscounts
                                .stream()
                                .flatMap(List::stream)
                                .peek(cartDiscount -> keyToIdCache.put(getKey(cartDiscount), cartDiscount.getId()))
                                .collect(toSet()));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final CartDiscountQuery cartDiscountQuery = CartDiscountQuery
            .of().plusPredicates(cartDiscountQueryModel -> getKeyQuery(cartDiscountQueryModel, key));

        return syncOptions
            .getCtpClient()
            .execute(cartDiscountQuery)
            .thenApply(cartDiscountPagedQueryResult ->
                cartDiscountPagedQueryResult
                    .head()
                    .map(cartDiscount -> {
                        keyToIdCache.put(getKey(cartDiscount), cartDiscount.getId());
                        return cartDiscount;
                    }));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CartDiscount>> createCartDiscount(
        @Nonnull final CartDiscountDraft cartDiscountDraft) {

        return createResource(cartDiscountDraft, this::getKey, CartDiscountCreateCommand::of);
    }

    @Nonnull
    @Override
    public CompletionStage<CartDiscount> updateCartDiscount(
        @Nonnull final CartDiscount cartDiscount,
        @Nonnull final List<UpdateAction<CartDiscount>> updateActions) {

        return updateResource(cartDiscount, CartDiscountUpdateCommand::of, updateActions);
    }
}
