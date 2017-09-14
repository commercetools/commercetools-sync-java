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
 * TODO: removeOtherVariants, whiteList, blackList, auto publish, revert staged changes, update staged.
 */
public class ProductSyncOptions extends BaseSyncOptions {
    private final boolean removeOtherVariants; // whether to remove other product variants or not.

    // defines which attributes to calculate update actions for
    private final List<String> whiteList;
    private final List<String> blackList;

    // optional filter which can be applied on generated list of update actions
    private final Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsFilter;

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
                       final List<String> whiteList,
                       final List<String> blackList,
                       final Function<List<UpdateAction<Product>>,
                           List<UpdateAction<Product>>> updateActionsFilter) {
        super(ctpClient, errorCallBack, warningCallBack, batchSize, removeOtherLocales, removeOtherSetEntries,
            removeOtherCollectionEntries, removeOtherProperties, allowUuid);
        this.removeOtherVariants = removeOtherVariants;
        this.whiteList = whiteList;
        this.blackList = blackList;
        this.updateActionsFilter = updateActionsFilter;
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

    public Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> getUpdateActionsFilter() {
        return updateActionsFilter;
    }
}
