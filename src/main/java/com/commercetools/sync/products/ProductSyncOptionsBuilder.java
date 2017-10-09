package com.commercetools.sync.products;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public final class ProductSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<ProductSyncOptionsBuilder, ProductSyncOptions> {
    public static final int BATCH_SIZE_DEFAULT = 30;
    private boolean removeOtherVariants = true;
    private SyncFilter syncFilter;
    private Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsCallBack;
    static final boolean ENSURE_CHANNELS_DEFAULT = false;
    private boolean ensurePriceChannels = ENSURE_CHANNELS_DEFAULT;

    private ProductSyncOptionsBuilder(final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    public static ProductSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new ProductSyncOptionsBuilder(ctpClient).setBatchSize(BATCH_SIZE_DEFAULT);
    }

    /**
     * Sets the {@code removeOtherVariants} boolean flag which sync additional variants without deleting
     * existing ones. If set to true, which is the default value of the option, it deletes the
     * existing variants. If set to false, it doesn't delete the existing ones.
     *
     * @param removeOtherVariants new value to set to the boolean flag.
     * @return {@code this} instance of {@link ProductSyncOptionsBuilder}
     */
    @Nonnull
    public ProductSyncOptionsBuilder removeOtherVariants(final boolean removeOtherVariants) {
        this.removeOtherVariants = removeOtherVariants;
        return this;
    }

    /**
     * Set option that defines {@link SyncFilter} for the sync, which defines either a blacklist or a whitelist for
     * filtering certain update action groups.
     *
     * <p>The action groups can be a list of any of the values of the enum {@link ActionGroup}, namely:
     * <ul>
     * <li>ATTRIBUTES</li>
     * <li>PRICES</li>
     * <li>IMAGES</li>
     * <li>CATEGORIES</li>
     * <li>.. and others</li>
     * </ul>
     *
     * <p>The {@code filterType} defines whether the list is to be blacklisted ({@link UpdateFilterType#BLACKLIST}) or
     * whitelisted ({@link UpdateFilterType#WHITELIST}). A blacklist means that <b>everything but</b> these action
     * groups will be synced. A whitelist means that <b>only</b> these action groups will be synced.
     *
     *
     * @param syncFilter defines either a blacklist or a whitelist for filtering certain update action groups.
     *
     * @return {@code this} instance of {@link ProductSyncOptionsBuilder}
     */
    @Nonnull
    public ProductSyncOptionsBuilder setSyncFilter(@Nonnull final SyncFilter syncFilter) {
        this.syncFilter = syncFilter;
        return this;
    }

    /**
     * Sets the update actions filter callback which can be applied on generated list of update actions to produce
     * a resultant list after the filter function has been applied.
     *
     * @param updateActionsCallBack filter function which can be applied on generated list of update actions
     * @return {@code this} instance of {@link CategorySyncOptionsBuilder}
     */
    @Nonnull
    public ProductSyncOptionsBuilder setUpdateActionsFilterCallBack(@Nonnull final Function<List<UpdateAction<Product>>,
        List<UpdateAction<Product>>> updateActionsCallBack) {
        this.updateActionsCallBack = updateActionsCallBack;
        return this;
    }

    /**
     * Set option that indicates whether sync process should create price channel of given key when it doesn't exists
     * in a target project yet. If set to {@code true} sync process would try to create new price channel of given key,
     * otherwise sync process would log error and fail to process draft with given price channel key.
     *
     * <p>This property is {@link ProductSyncOptionsBuilder#ENSURE_CHANNELS_DEFAULT} by default.
     *
     * @param ensurePriceChannels boolean that indicates whether sync process should create price channel of given key
     *                            when it doesn't exists in a target project yet
     * @return {@code this} instance of {@link ProductSyncOptionsBuilder}
     */
    @Nonnull
    public ProductSyncOptionsBuilder ensurePriceChannels(final boolean ensurePriceChannels) {
        this.ensurePriceChannels = ensurePriceChannels;
        return this;
    }

    @Override
    @Nonnull
    public ProductSyncOptions build() {
        return new ProductSyncOptions(
            ctpClient,
            errorCallBack,
            warningCallBack,
            batchSize,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid,
            removeOtherVariants,
            syncFilter,
            updateActionsCallBack,
            ensurePriceChannels
        );
    }


    @Override
    protected ProductSyncOptionsBuilder getThis() {
        return this;
    }
}
