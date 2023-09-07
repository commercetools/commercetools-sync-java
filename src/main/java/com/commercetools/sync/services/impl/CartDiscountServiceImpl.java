package com.commercetools.sync.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

import com.commercetools.api.client.ByProjectKeyCartDiscountsGet;
import com.commercetools.api.client.ByProjectKeyCartDiscountsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyCartDiscountsPost;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountPagedQueryResponse;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateBuilder;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptions;
import com.commercetools.sync.services.CartDiscountService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CartDiscountServiceImpl
    extends BaseServiceWithKey<
        CartDiscountSyncOptions,
        CartDiscount,
        CartDiscountDraft,
        ByProjectKeyCartDiscountsGet,
        CartDiscountPagedQueryResponse,
        ByProjectKeyCartDiscountsKeyByKeyGet,
        CartDiscount,
        ByProjectKeyCartDiscountsPost>
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
            syncOptions
                .getCtpClient()
                .cartDiscounts()
                .get()
                .withWhere("key in :keys")
                .withPredicateVar("keys", keysNotCached));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CartDiscount>> fetchCartDiscount(@Nullable final String key) {
    final ByProjectKeyCartDiscountsKeyByKeyGet byProjectKeyCartDiscountsKeyByKeyGet =
        syncOptions.getCtpClient().cartDiscounts().withKey(key).get();
    return super.fetchResource(key, byProjectKeyCartDiscountsKeyByKeyGet);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<CartDiscount>> createCartDiscount(
      @Nonnull final CartDiscountDraft cartDiscountDraft) {
    return super.createResource(
        cartDiscountDraft,
        CartDiscountDraft::getKey,
        CartDiscount::getId,
        Function.identity(),
        () -> syncOptions.getCtpClient().cartDiscounts().post(cartDiscountDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<CartDiscount> updateCartDiscount(
      @Nonnull final CartDiscount cartDiscount,
      @Nonnull final List<CartDiscountUpdateAction> updateActions) {

    final List<List<CartDiscountUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<CartDiscount>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, cartDiscount));

    for (final List<CartDiscountUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedCartDiscount ->
                      syncOptions
                          .getCtpClient()
                          .cartDiscounts()
                          .withId(updatedCartDiscount.getId())
                          .post(
                              CartDiscountUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedCartDiscount.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
