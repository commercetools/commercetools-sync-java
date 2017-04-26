package com.commercetools.sync.categories;


import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import com.commercetools.sync.commons.helpers.CtpClient;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class CategorySyncOptionsBuilder extends BaseSyncOptionsBuilder {
    private Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter;

    private CategorySyncOptionsBuilder(@Nonnull final CtpClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link CategorySyncOptionsBuilder} given a {@link CtpClient}, as a param, that contains
     * all the configuration of the CTP client.
     *
     * @param ctpClient wrapper that contains instance of the {@link io.sphere.sdk.client.SphereClientConfig} and
     *                  {@link io.sphere.sdk.client.BlockingSphereClient}
     * @return {@code this} instance of {@link CategorySyncOptionsBuilder}
     */
    public static CategorySyncOptionsBuilder of(@Nonnull final CtpClient ctpClient) {
        return new CategorySyncOptionsBuilder(ctpClient);
    }

    /**
     * Sets the updateActions filter function which can be applied on generated list of update actions to produce
     * a resultant list after the filter function has been applied.
     *
     * @param updateActionsFilter filter function which can be applied on generated list of update actions
     * @return {@code this} instance of {@link CategorySyncOptionsBuilder}
     */
    public CategorySyncOptionsBuilder setUpdateActionsFilter(@Nonnull final Function<List<UpdateAction<Category>>,
            List<UpdateAction<Category>>> updateActionsFilter) {
        this.updateActionsFilter = updateActionsFilter;
        return this;
    }

    /***
     * Creates new instance of {@link CategorySyncOptions} enriched with all attributes provided to {@code this} builder.
     *
     * @return new instance of {@link CategorySyncOptions}
     */
    @Override
    public CategorySyncOptions build() {
        return new CategorySyncOptions(this.ctpClient, this.errorCallBack, this.warningCallBack, this.removeOtherLocales,
                this.removeOtherSetEntries, this.removeOtherCollectionEntries, this.removeOtherProperties,
                this.updateActionsFilter);
    }
}
