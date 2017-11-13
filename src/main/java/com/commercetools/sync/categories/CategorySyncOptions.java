package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CategorySyncOptions extends BaseSyncOptions<Category> {

    CategorySyncOptions(@Nonnull final SphereClient ctpClient,
                        @Nullable final BiConsumer<String, Throwable> updateActionErrorCallBack,
                        @Nullable final Consumer<String> updateActionWarningCallBack,
                        final int batchSize,
                        final boolean removeOtherLocales,
                        final boolean removeOtherSetEntries,
                        final boolean removeOtherCollectionEntries,
                        final boolean removeOtherProperties,
                        final boolean allowUuid,
                        @Nullable final Function<List<UpdateAction<Category>>,
                          List<UpdateAction<Category>>> beforeUpdateCallback) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid,
            beforeUpdateCallback);
    }
}
