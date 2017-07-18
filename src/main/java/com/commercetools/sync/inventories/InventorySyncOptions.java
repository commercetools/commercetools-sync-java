package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class InventorySyncOptions extends BaseSyncOptions {

    private final boolean ensureChannels;

    InventorySyncOptions(@Nonnull final SphereClient ctpClient,
                         final BiConsumer<String, Throwable> updateActionErrorCallBack,
                         final Consumer<String> updateActionWarningCallBack,
                         final int batchSize,
                         final boolean removeOtherLocales,
                         final boolean removeOtherSetEntries,
                         final boolean removeOtherCollectionEntries,
                         final boolean removeOtherProperties,
                         final boolean allowUuid,
                         boolean ensureChannels) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid);
        this.ensureChannels = ensureChannels;

    }

    /**
     * @return option that indicates whether sync process should create supply channel of given key when it doesn't
     *      exists in a target system yet.
     */
    public boolean shouldEnsureChannels() {
        return ensureChannels;
    }


}
