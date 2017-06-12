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
    private final boolean compareStaged; // to control whether to compare to the staged data or the published ones.
    private final boolean updateStaged; // to control whether to update staged or current projection
    private final boolean publish; // to control whether to auto-publish or not.
    private final boolean removeOtherVariants; // to control whether to remove other product variants or not.

    // defines which attributes
    private final List<String> whiteList;
    private final List<String> blackList;

    // optional filter which can be applied on generated list of update actions
    private final Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> actionsFilter;

    ProductSyncOptions(@Nonnull final SphereClient ctpClient,
                       @Nonnull final BiConsumer<String, Throwable> errorCallBack,
                       @Nonnull final Consumer<String> warningCallBack,
                       final boolean removeOtherLocales,
                       final boolean removeOtherSetEntries,
                       final boolean removeOtherCollectionEntries,
                       final boolean removeOtherProperties,
                       final boolean compareStaged,
                       final boolean updateStaged,
                       final boolean publish,
                       final boolean removeOtherVariants,
                       final List<String> whiteList,
                       final List<String> blackList,
                       final Function<List<UpdateAction<Product>>,
                           List<UpdateAction<Product>>> actionsFilter) {
        super(ctpClient, errorCallBack, warningCallBack, removeOtherLocales, removeOtherSetEntries,
            removeOtherCollectionEntries, removeOtherProperties);
        this.compareStaged = compareStaged;
        this.updateStaged = updateStaged;
        this.publish = publish;
        this.removeOtherVariants = removeOtherVariants;
        this.whiteList = whiteList;
        this.blackList = blackList;
        this.actionsFilter = actionsFilter;
    }

    public boolean shouldCompareStaged() {
        return compareStaged;
    }

    public boolean shouldUpdateStaged() {
        return updateStaged;
    }

    public boolean shouldPublish() {
        return publish;
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

    Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> getActionsFilter() {
        return actionsFilter;
    }
}
