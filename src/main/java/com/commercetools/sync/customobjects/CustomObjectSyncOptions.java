package com.commercetools.sync.customobjects;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.customobjects.models.NoopResourceUpdateAction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomObjectSyncOptions
    extends BaseSyncOptions<CustomObject, CustomObjectDraft, NoopResourceUpdateAction> {

  CustomObjectSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<CustomObjectDraft>,
                  Optional<CustomObject>,
                  List<NoopResourceUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<CustomObjectDraft>, Optional<CustomObject>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<NoopResourceUpdateAction>,
                  CustomObjectDraft,
                  CustomObject,
                  List<NoopResourceUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<CustomObjectDraft, CustomObjectDraft> beforeCreateCallback,
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
