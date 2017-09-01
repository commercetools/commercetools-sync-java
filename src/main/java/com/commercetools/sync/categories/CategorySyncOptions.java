package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CategorySyncOptions extends BaseSyncOptions {
    private final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter;

    CategorySyncOptions(@Nonnull final SphereClient ctpClient,
                        final BiConsumer<String, Throwable> updateActionErrorCallBack,
                        final Consumer<String> updateActionWarningCallBack,
                        final int batchSize,
                        final boolean removeOtherLocales,
                        final boolean removeOtherSetEntries,
                        final boolean removeOtherCollectionEntries,
                        final boolean removeOtherProperties,
                        final boolean allowUuid,
                        final Function<List<UpdateAction<Category>>,
                          List<UpdateAction<Category>>> updateActionsFilter) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            removeOtherLocales,
            removeOtherSetEntries,
            removeOtherCollectionEntries,
            removeOtherProperties,
            allowUuid);
        this.updateActionsFilter = updateActionsFilter;
    }

    /**
     * Returns the {@code updateActionsFilter} {@link Function}&lt;{@link List}&lt;{@link UpdateAction}&lt;
     * {@link Category}&gt;&gt;, {@link List}&lt;{@link UpdateAction}&lt;{@link Category}&gt;&gt;&gt; function set to
     * {@code this} {@link CategorySyncOptions}. It represents a filter function which can be applied on generated list
     * of update actions to produce a resultant list after the filter function has been applied.
     *
     * @return the {@code updateActionsFilter} {@link Function}&lt;{@link List}&lt;{@link UpdateAction}&lt;
     *         {@link Category}&gt;&gt;, {@link List}&lt;{@link UpdateAction}&lt;{@link Category}&gt;&gt;&gt; function
     *         set to {@code this} {@link CategorySyncOptions}.
     */
    public Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> getUpdateActionsFilter() {
        return updateActionsFilter;
    }
}
