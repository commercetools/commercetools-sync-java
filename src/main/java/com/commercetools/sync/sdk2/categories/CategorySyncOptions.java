package com.commercetools.sync.sdk2.categories;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryUpdateAction;
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

public final class CategorySyncOptions
    extends BaseSyncOptions<Category, CategoryDraft, CategoryUpdateAction> {

  CategorySyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  com.commercetools.sync.sdk2.commons.exceptions.SyncException,
                  Optional<CategoryDraft>,
                  Optional<Category>,
                  List<CategoryUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<CategoryUpdateAction>, CategoryDraft, Category, List<CategoryUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<CategoryDraft, CategoryDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
