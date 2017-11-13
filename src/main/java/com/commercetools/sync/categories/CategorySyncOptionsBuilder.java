package com.commercetools.sync.categories;


import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;

import javax.annotation.Nonnull;

public final class CategorySyncOptionsBuilder extends BaseSyncOptionsBuilder<CategorySyncOptionsBuilder,
    CategorySyncOptions, Category> {
    public static final int BATCH_SIZE_DEFAULT = 50;

    private CategorySyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link CategorySyncOptionsBuilder} given a {@link SphereClient} responsible for
     * interaction with the target CTP project, with the dafult batch size ({@code BATCH_SIZE_DEFAULT} = 50).
     *
     * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the target CTP project.
     * @return new instance of {@link CategorySyncOptionsBuilder}
     */
    public static CategorySyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new CategorySyncOptionsBuilder(ctpClient)
            .setBatchSize(BATCH_SIZE_DEFAULT);
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
            this.batchSize,
            this.removeOtherLocales,
            this.removeOtherSetEntries,
            this.removeOtherCollectionEntries,
            this.removeOtherProperties,
            this.allowUuid,
            this.beforeUpdateCallback);
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
