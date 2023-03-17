package com.commercetools.sync.sdk2.taxcategories;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TaxCategorySyncOptions
    extends BaseSyncOptions<TaxCategory, TaxCategoryDraft, TaxCategoryUpdateAction> {

  TaxCategorySyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<TaxCategoryDraft>,
                  Optional<TaxCategory>,
                  List<TaxCategoryUpdateAction>>
              errorCallBack,
      @Nullable
          final TriConsumer<SyncException, Optional<TaxCategoryDraft>, Optional<TaxCategory>>
              warningCallBack,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<TaxCategoryUpdateAction>,
                  TaxCategoryDraft,
                  TaxCategory,
                  List<TaxCategoryUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<TaxCategoryDraft, TaxCategoryDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallBack,
        warningCallBack,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
