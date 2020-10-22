package com.commercetools.sync.shoppinglists;

import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;

import javax.annotation.Nonnull;

public final class ShoppingListSyncOptionsBuilder
    extends BaseSyncOptionsBuilder<ShoppingListSyncOptionsBuilder, ShoppingListSyncOptions, ShoppingList,
    ShoppingListDraft> {

    public static final int BATCH_SIZE_DEFAULT = 50;

    private ShoppingListSyncOptionsBuilder(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    /**
     * Creates a new instance of {@link ShoppingListSyncOptionsBuilder} given a {@link SphereClient} responsible for
     * interaction with the target CTP project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 50).
     *
     * @param ctpClient instance of the {@link SphereClient} responsible for interaction with the target CTP project.
     * @return new instance of {@link ShoppingListSyncOptionsBuilder}
     */
    public static ShoppingListSyncOptionsBuilder of(@Nonnull final SphereClient ctpClient) {
        return new ShoppingListSyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
    }

    /**
     * Creates new instance of {@link ShoppingListSyncOptions} enriched with all fields provided to {@code this} builder
     *
     * @return new instance of {@link ShoppingListSyncOptions}
     */
    @Override
    public ShoppingListSyncOptions build() {
        return new ShoppingListSyncOptions(
            ctpClient,
            errorCallback,
            warningCallback,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback
        );
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected ShoppingListSyncOptionsBuilder getThis() {
        return this;
    }
}
