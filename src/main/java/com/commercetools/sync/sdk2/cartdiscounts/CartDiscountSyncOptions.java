package com.commercetools.sync.sdk2.cartdiscounts;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CartDiscountSyncOptions
    extends BaseSyncOptions<CartDiscount, CartDiscountDraft, CartDiscountUpdateAction> {

  CartDiscountSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<CartDiscountDraft>,
                  Optional<CartDiscount>,
                  List<CartDiscountUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<CartDiscountUpdateAction>,
                  CartDiscountDraft,
                  CartDiscount,
                  List<CartDiscountUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<CartDiscountDraft, CartDiscountDraft> beforeCreateCallback,
      final long cacheSize) {

    super(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
