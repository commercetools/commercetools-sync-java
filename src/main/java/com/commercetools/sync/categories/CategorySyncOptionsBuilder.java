package com.commercetools.sync.categories;


import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public final class CategorySyncOptionsBuilder extends BaseSyncOptionsBuilder<CategorySyncOptionsBuilder,
    CategorySyncOptions> {
    private Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter;

    private CategorySyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link CategorySyncOptionsBuilder} given a {@link SphereClient} responsible for
     * interaction with the target CTP project.
     *
     * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the target CTP project.
     * @return new instance of {@link CategorySyncOptionsBuilder}
     */
    public static CategorySyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
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

    /**
     * Creates new instance of {@link CategorySyncOptions} enriched with all attributes provided to {@code this}
     * builder.
     *
     * @return new instance of {@link CategorySyncOptions}
     */
    @Override
    public CategorySyncOptions build() {
        return new CategorySyncOptions(
            this.ctpClient,
            this.errorCallBack,
            this.warningCallBack,
            this.removeOtherLocales,
            this.removeOtherSetEntries,
            this.removeOtherCollectionEntries,
            this.removeOtherProperties,
            this.allowUuid,
            this.updateActionsFilter);
    }

    /**
     * Returns an instance of this class to be used in the super class generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected CategorySyncOptionsBuilder getThis() {
        return this;
    }
}
