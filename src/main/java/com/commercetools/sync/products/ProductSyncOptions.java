package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This Class is a WIP and should be reimplemented when the Product Sync module is implemented.
 * It is just provided as a skeleton/reminder for the options it should include in the future.
 */
public class ProductSyncOptions extends BaseSyncOptions {
    private final boolean updateStaged; // whether to compare and update the staged or current projection data.
    private final boolean publish; // whether to auto-publish or not.
    private final boolean revertStagedChanges; // whether to revert potential staged changes before synchronization.
    private final boolean removeOtherVariants; // whether to remove other product variants or not.

    // defines which attributes
    private final List<String> whiteList;
    private final List<String> blackList;

    // optional filter which can be applied on generated list of update actions
    private final Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> actionsFilter;

    ProductSyncOptions(@Nonnull final SphereClient ctpClient,
                       @Nonnull final BiConsumer<String, Throwable> errorCallBack,
                       @Nonnull final Consumer<String> warningCallBack,
                       final int batchSize,
                       final boolean removeOtherLocales,
                       final boolean removeOtherSetEntries,
                       final boolean removeOtherCollectionEntries,
                       final boolean removeOtherProperties,
                       final boolean allowUuid,
                       final boolean updateStaged,
                       final boolean publish,
                       final boolean revertStagedChanges,
                       final boolean removeOtherVariants,
                       @Nonnull final List<String> whiteList,
                       @Nonnull final List<String> blackList,
                       @Nonnull final Function<List<UpdateAction<Product>>,
                           List<UpdateAction<Product>>> actionsFilter) {
        super(ctpClient, errorCallBack, warningCallBack, batchSize, removeOtherLocales, removeOtherSetEntries,
            removeOtherCollectionEntries, removeOtherProperties, allowUuid);
        this.updateStaged = updateStaged;
        this.publish = publish;
        this.revertStagedChanges = revertStagedChanges;
        this.removeOtherVariants = removeOtherVariants;
        this.whiteList = whiteList;
        this.blackList = blackList;
        this.actionsFilter = actionsFilter;
    }

    public boolean shouldUpdateStaged() {
        return updateStaged;
    }

    public boolean shouldPublish() {
        return publish;
    }

    public boolean shouldRevertStagedChanges() {
        return revertStagedChanges;
    }

    boolean shouldRemoveOtherVariants() {
        return removeOtherVariants;
    }

    List<String> getWhiteList() {
        return whiteList;
    }

    List<String> getBlackList() {
        return blackList;
    }

    public Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> getActionsFilter() {
        return actionsFilter;
    }
}
