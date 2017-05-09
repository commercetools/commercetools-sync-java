package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.CtpClient;
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
    private boolean compareStaged = true; // to control whether to compare to the staged data or the published ones.
    private boolean publish = false; // to control whether to auto-publish or not.
    private boolean removeOtherVariants = true; // to control whether to remove other product variants or not.

    // defines which attributes
    private List<String> whiteList;
    private List<String> blackList;

    // optional filter which can be applied on generated list of update actions
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> filterActions;

    ProductSyncOptions(@Nonnull final CtpClient ctpClient,
                              @Nonnull final BiConsumer<String, Throwable> errorCallBack,
                              @Nonnull final Consumer<String> warningCallBack,
                              final boolean removeOtherLocales,
                              final boolean removeOtherSetEntries,
                              final boolean removeOtherCollectionEntries,
                              final boolean removeOtherProperties,
                              final boolean compareStaged,
                              final boolean publish,
                              final boolean removeOtherVariants,
                              @Nonnull final List<String> whiteList,
                              @Nonnull final List<String> blackList,
                              @Nonnull final Function<List<UpdateAction<Product>>,
                                List<UpdateAction<Product>>> filterActions) {
        super(ctpClient, errorCallBack, warningCallBack, removeOtherLocales, removeOtherSetEntries,
          removeOtherCollectionEntries, removeOtherProperties);
        this.compareStaged = compareStaged;
        this.publish = publish;
        this.removeOtherVariants = removeOtherVariants;
        this.whiteList = whiteList;
        this.blackList = blackList;
        this.filterActions = filterActions;
    }

}
