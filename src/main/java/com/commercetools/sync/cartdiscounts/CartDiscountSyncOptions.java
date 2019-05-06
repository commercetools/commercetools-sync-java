package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CartDiscountSyncOptions extends BaseSyncOptions<CartDiscount, CartDiscountDraft> {

    CartDiscountSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final BiConsumer<String, Throwable> errorCallBack,
        @Nullable final Consumer<String> warningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<CartDiscount>>, CartDiscountDraft,
            CartDiscount, List<UpdateAction<CartDiscount>>> beforeUpdateCallback,
        @Nullable final Function<CartDiscountDraft, CartDiscountDraft> beforeCreateCallback) {

        super(ctpClient, errorCallBack, warningCallBack, batchSize, beforeUpdateCallback, beforeCreateCallback);
    }
}
