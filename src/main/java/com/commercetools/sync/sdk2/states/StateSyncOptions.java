package com.commercetools.sync.sdk2.states;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateUpdateAction;
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

public final class StateSyncOptions extends BaseSyncOptions<State, StateDraft, StateUpdateAction> {

  StateSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException, Optional<StateDraft>, Optional<State>, List<StateUpdateAction>>
              errorCallBack,
      @Nullable
          final TriConsumer<SyncException, Optional<StateDraft>, Optional<State>> warningCallBack,
      final int batchSize,
      @Nullable
          final TriFunction<List<StateUpdateAction>, StateDraft, State, List<StateUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<StateDraft, StateDraft> beforeCreateCallback,
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
