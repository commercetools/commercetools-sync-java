package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProductTypeSyncOptions extends BaseSyncOptions<ProductType, ProductTypeDraft> {
    ProductTypeSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final BiConsumer<String, Throwable> updateActionErrorCallBack,
        @Nullable final Consumer<String> updateActionWarningCallBack,
        final int batchSize,
        final boolean allowUuid,
        @Nullable final TriFunction<List<UpdateAction<ProductType>>, ProductTypeDraft, ProductType,
                List<UpdateAction<ProductType>>> beforeUpdateCallback,
        @Nullable final Function<ProductTypeDraft, ProductTypeDraft> beforeCreateCallback
    ) {

        super(
            ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            allowUuid,
            beforeUpdateCallback,
            beforeCreateCallback
        );
    }

}
