package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadriConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ProductTypeSyncOptions extends BaseSyncOptions<ProductType, ProductTypeDraft> {
    ProductTypeSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadriConsumer<SyncException, ProductType, ProductTypeDraft,
            Optional<List<UpdateAction<ProductType>>>> updateActionErrorCallBack,
        @Nullable final TriConsumer<SyncException, ProductType, ProductTypeDraft> updateActionWarningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<ProductType>>, ProductTypeDraft, ProductType,
                List<UpdateAction<ProductType>>> beforeUpdateCallback,
        @Nullable final Function<ProductTypeDraft, ProductTypeDraft> beforeCreateCallback
    ) {

        super(
            ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback
        );
    }

}
