package com.commercetools.sync.cartdiscounts;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CartDiscountSyncOptions extends BaseSyncOptions<CartDiscount, CartDiscountDraft> {

    CartDiscountSyncOptions(
            @Nonnull final SphereClient ctpClient,
            @Nullable final QuadConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>,
                    List<UpdateAction<CartDiscount>>> errorCallback,
            @Nullable final TriConsumer<SyncException, Optional<CartDiscountDraft>, Optional<CartDiscount>>
                    warningCallback,
            final int batchSize,
            @Nullable final TriFunction<List<UpdateAction<CartDiscount>>, CartDiscountDraft,
                    CartDiscount, List<UpdateAction<CartDiscount>>> beforeUpdateCallback,
            @Nullable final Function<CartDiscountDraft, CartDiscountDraft> beforeCreateCallback) {

        super(ctpClient, errorCallback, warningCallback, batchSize, beforeUpdateCallback, beforeCreateCallback);
    }
}
