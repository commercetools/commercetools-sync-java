package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadriConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CategorySyncOptions extends BaseSyncOptions<Category, CategoryDraft> {

    CategorySyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadriConsumer<SyncException, Optional<Category>, Optional<CategoryDraft>,
            Optional<List<UpdateAction<Category>>>> updateActionErrorCallBack,
        @Nullable final TriConsumer<SyncException, Optional<Category>, Optional<CategoryDraft>>
            updateActionWarningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<Category>>, CategoryDraft, Category,
            List<UpdateAction<Category>>> beforeUpdateCallback,
        @Nullable final Function<CategoryDraft, CategoryDraft> beforeCreateCallback) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback);
    }
}
