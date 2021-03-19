package com.commercetools.sync.types;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TypeSyncOptions extends BaseSyncOptions<Type, TypeDraft, Type> {

  TypeSyncOptions(
      @Nonnull final SphereClient ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException, Optional<TypeDraft>, Optional<Type>, List<UpdateAction<Type>>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<TypeDraft>, Optional<Type>> warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<List<UpdateAction<Type>>, TypeDraft, Type, List<UpdateAction<Type>>>
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
