package com.commercetools.sync.services.impl;

import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.services.CartDiscountService;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountCreateCommand;
import io.sphere.sdk.cartdiscounts.commands.CartDiscountUpdateCommand;
import io.sphere.sdk.cartdiscounts.expansion.CartDiscountExpansionModel;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQueryBuilder;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQueryModel;
import io.sphere.sdk.commands.UpdateAction;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CartDiscountServiceImpl
    extends BaseServiceWithKey<
        CartDiscountDraft,
        CartDiscount,
        CartDiscountSyncOptions,
        CartDiscountQuery,
        CartDiscountQueryModel,
        CartDiscountExpansionModel<CartDiscount>>
    implements CartDiscountService {

  public CartDiscountServiceImpl(@Nonnull final CartDiscountSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<CartDiscount>> fetchMatchingCartDiscountsByKeys(
      @Nonnull final Set<String> keys) {

    return fetchMatchingResources(
        keys,
        (keysNotCached) ->
            CartDiscountQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.key().isIn(keysNotCached))
                .build());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable final String key) {

    return fetchResource(
        key,
        () ->
            CartDiscountQuery.of()
                .plusPredicates(cartDiscountQueryModel -> cartDiscountQueryModel.key().is(key)));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CartDiscount>> createCartDiscount(
      @Nonnull final CartDiscountDraft cartDiscountDraft) {

    return createResource(cartDiscountDraft, CartDiscountCreateCommand::of);
  }

  @Nonnull
  @Override
  public CompletionStage<CartDiscount> updateCartDiscount(
      @Nonnull final CartDiscount cartDiscount,
      @Nonnull final List<UpdateAction<CartDiscount>> updateActions) {

    return updateResource(cartDiscount, CartDiscountUpdateCommand::of, updateActions);
  }
}
