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

/**
 * TODO: removeOtherVariants, whiteList, blackList, auto publish, revert staged changes, update staged.
 */
public class ProductSyncOptions extends BaseSyncOptions {
    private final boolean removeOtherVariants; // whether to remove other product variants or not.

    // defines which attributes to calculate update actions to black list or white list
    private final SyncFilter syncFilter;

    // optional callback function which can be applied on the generated list of update actions
    private final Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsCallBack;
    private final boolean ensurePriceChannels;

    ProductSyncOptions(@Nonnull final SphereClient ctpClient,
                       final BiConsumer<String, Throwable> errorCallBack,
                       final Consumer<String> warningCallBack,
                       final int batchSize,
                       final boolean removeOtherLocales,
                       final boolean removeOtherSetEntries,
                       final boolean removeOtherCollectionEntries,
                       final boolean removeOtherProperties,
                       final boolean allowUuid,
                       final boolean removeOtherVariants,
                       final SyncFilter syncFilter,
                       final Function<List<UpdateAction<Product>>,
                           List<UpdateAction<Product>>> updateActionsCallBack,
                       boolean ensurePriceChannels) {
        super(ctpClient, errorCallBack, warningCallBack, batchSize, removeOtherLocales, removeOtherSetEntries,
            removeOtherCollectionEntries, removeOtherProperties, allowUuid);
        this.removeOtherVariants = removeOtherVariants;
        this.syncFilter = syncFilter;
        this.updateActionsCallBack = updateActionsCallBack;
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
    @Nullable
    public SyncFilter getSyncFilter() {
        return syncFilter;
    }

    /**
     * Returns the {@code updateActionsCallBack} {@link Function}&lt;{@link List}&lt;{@link UpdateAction}&lt;
     * {@link Product}&gt;&gt;, {@link List}&lt;{@link UpdateAction}&lt;{@link Product}&gt;&gt;&gt; function set to
     * {@code this} {@link ProductSyncOptions}. It represents a filter function which can be applied on generated list
     * of update actions to produce a resultant list after the filter function has been applied.
     *
     * @return the {@code updateActionsFilter} {@link Function}&lt;{@link List}&lt;{@link UpdateAction}&lt;
     *         {@link Product}&gt;&gt;, {@link List}&lt;{@link UpdateAction}&lt;{@link Product}&gt;&gt;&gt; function
     *         set to {@code this} {@link ProductSyncOptions}.
     */
    @Nullable
    public Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> getUpdateActionsCallBack() {
        return updateActionsCallBack;
    }

    /**
     * @return option that indicates whether sync process should create price channel of given key when it doesn't
     *      exists in a target project yet.
     */
    public boolean shouldEnsurePriceChannels() {
        return ensurePriceChannels;
    }
}
