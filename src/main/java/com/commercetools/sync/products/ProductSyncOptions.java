package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * TODO: removeOtherVariants, whiteList, blackList, auto publish, revert staged changes, update staged.
 */
public class ProductSyncOptions extends BaseSyncOptions<Product> {
    private final boolean removeOtherVariants; // whether to remove other product variants or not.
    private final SyncFilter syncFilter; // which attributes to calculate update actions to black list or white list
    private final boolean ensurePriceChannels;

    ProductSyncOptions(@Nonnull final SphereClient ctpClient,
                       @Nullable final BiConsumer<String, Throwable> errorCallBack,
                       @Nullable final Consumer<String> warningCallBack,
                       final int batchSize,
                       final boolean removeOtherLocales,
                       final boolean removeOtherSetEntries,
                       final boolean removeOtherCollectionEntries,
                       final boolean removeOtherProperties,
                       final boolean allowUuid,
                       final boolean removeOtherVariants,
                       @Nullable final SyncFilter syncFilter,
                       @Nullable final Function<List<UpdateAction<Product>>,
                           List<UpdateAction<Product>>> beforeUpdateCallback,
                       boolean ensurePriceChannels) {
        super(ctpClient, errorCallBack, warningCallBack, batchSize, removeOtherLocales, removeOtherSetEntries,
            removeOtherCollectionEntries, removeOtherProperties, allowUuid, beforeUpdateCallback);
        this.removeOtherVariants = removeOtherVariants;
        this.syncFilter = ofNullable(syncFilter).orElseGet(SyncFilter::of);
        this.ensurePriceChannels = ensurePriceChannels;
    }

    boolean shouldRemoveOtherVariants() {
        return removeOtherVariants;
    }

    /**
     * Returns the {@link SyncFilter} set to {@code this} {@link ProductSyncOptions}.
     * It represents either a blacklist or a whitelist for filtering certain update action groups.
     *
     * @return the {@link SyncFilter} set to {@code this} {@link ProductSyncOptions}.
     */
    @Nonnull
    public SyncFilter getSyncFilter() {
        return syncFilter;
    }

    /**
     * @return option that indicates whether sync process should create price channel of given key when it doesn't
     *      exists in a target project yet.
     */
    public boolean shouldEnsurePriceChannels() {
        return ensurePriceChannels;
    }
}
