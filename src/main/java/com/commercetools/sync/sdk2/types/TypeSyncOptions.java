package com.commercetools.sync.sdk2.types;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TypeSyncOptions extends BaseSyncOptions<Type, TypeDraft, TypeUpdateAction> {

  TypeSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException, Optional<TypeDraft>, Optional<Type>, List<TypeUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<TypeDraft>, Optional<Type>> warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<List<TypeUpdateAction>, TypeDraft, Type, List<TypeUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<TypeDraft, TypeDraft> beforeCreateCallback,
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
