package com.commercetools.sync.states;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class StateSyncOptions extends BaseSyncOptions<State, StateDraft> {

    StateSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadConsumer<SyncException, Optional<StateDraft>, Optional<State>,
            List<UpdateAction<State>>> errorCallBack,
        @Nullable final TriConsumer<SyncException, Optional<StateDraft>, Optional<State>>
            warningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<State>>, StateDraft, State,
            List<UpdateAction<State>>> beforeUpdateCallback,
        @Nullable final Function<StateDraft, StateDraft> beforeCreateCallback) {
        super(
            ctpClient,
            errorCallBack,
            warningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback);
    }

}
