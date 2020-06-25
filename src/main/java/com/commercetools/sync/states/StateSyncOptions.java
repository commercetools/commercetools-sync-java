package com.commercetools.sync.states;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StateSyncOptions extends BaseSyncOptions<State, StateDraft> {

    StateSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final BiConsumer<String, Throwable> errorCallBack,
        @Nullable final Consumer<String> warningCallBack,
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
