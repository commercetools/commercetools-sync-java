package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CategorySyncOptions extends BaseSyncOptions<Category, CategoryDraft> {

  CategorySyncOptions(
      @Nonnull final SphereClient ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<CategoryDraft>,
                  Optional<Category>,
                  List<UpdateAction<Category>>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<CategoryDraft>, Optional<Category>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<UpdateAction<Category>>,
                  CategoryDraft,
                  Category,
                  List<UpdateAction<Category>>>
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
