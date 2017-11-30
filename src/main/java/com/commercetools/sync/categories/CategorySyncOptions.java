package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CategorySyncOptions extends BaseSyncOptions<Category, CategoryDraft> {

    CategorySyncOptions(@Nonnull final SphereClient ctpClient,
                        @Nullable final BiConsumer<String, Throwable> updateActionErrorCallBack,
                        @Nullable final Consumer<String> updateActionWarningCallBack,
                        final int batchSize,
                        final boolean allowUuid,
                        @Nullable final TriFunction<List<UpdateAction<Category>>, CategoryDraft, Category,
                                                    List<UpdateAction<Category>>> beforeUpdateCallback,
                        @Nullable final Function<CategoryDraft, CategoryDraft> beforeCreateCallback) {
        super(ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            allowUuid,
            beforeUpdateCallback,
            beforeCreateCallback);
    }
}
