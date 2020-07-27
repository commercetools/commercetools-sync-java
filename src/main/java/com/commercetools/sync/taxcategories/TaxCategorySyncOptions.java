package com.commercetools.sync.taxcategories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TaxCategorySyncOptions extends BaseSyncOptions<TaxCategory, TaxCategoryDraft> {

    TaxCategorySyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final BiConsumer<String, Throwable> errorCallBack,
        @Nullable final Consumer<String> warningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<TaxCategory>>, TaxCategoryDraft, TaxCategory,
            List<UpdateAction<TaxCategory>>> beforeUpdateCallback,
        @Nullable final Function<TaxCategoryDraft, TaxCategoryDraft> beforeCreateCallback) {
        super(
            ctpClient,
            errorCallBack,
            warningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback);
    }

}
