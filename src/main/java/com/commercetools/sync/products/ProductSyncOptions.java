package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
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
    private final SyncFilter syncFilter;

    private List<UpdateFilter> whiteList;
    private List<UpdateFilter> blackList;

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
        setFilterListsFromSyncFilter();
        this.updateActionsCallBack = updateActionsCallBack;
        this.ensurePriceChannels = ensurePriceChannels;
    }

    private void setFilterListsFromSyncFilter() {
        whiteList = Collections.emptyList();
        blackList = Collections.emptyList();
        if (syncFilter != null) {
            final List<UpdateFilter> filters = syncFilter.getFilters();
            final UpdateFilterType filterType = syncFilter.getFilterType();
            if (filterType.equals(UpdateFilterType.BLACKLIST)) {
                blackList = new ArrayList<>(filters);
            } else {
                whiteList = new ArrayList<>(filters);
            }
        }
    }

    boolean shouldRemoveOtherVariants() {
        return removeOtherVariants;
    }

    /**
     * TODO
     * @return TODO..
     */
    public SyncFilter getSyncFilter() {
        return syncFilter;
    }

    public List<UpdateFilter> getWhiteList() {
        return whiteList;
    }

    public List<UpdateFilter> getBlackList() {
        return blackList;
    }

    /**
     * getUpdateActionsCallBack TODO.
     * @return TODO
     */
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
