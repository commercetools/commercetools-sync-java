package com.commercetools.sync.types;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadriConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TypeSyncOptions extends BaseSyncOptions<Type, TypeDraft> {

    TypeSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final QuadriConsumer<SyncException, Optional<Type>, Optional<TypeDraft>,
            Optional<List<UpdateAction<Type>>>> updateActionErrorCallBack,
        @Nullable final TriConsumer<SyncException, Optional<Type>, Optional<TypeDraft>>
            updateActionWarningCallBack,
        final int batchSize,
        @Nullable final TriFunction<List<UpdateAction<Type>>, TypeDraft,
                Type, List<UpdateAction<Type>>> beforeUpdateCallback,
        @Nullable final Function<TypeDraft, TypeDraft> beforeCreateCallback
    ) {
        super(
            ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            beforeUpdateCallback,
            beforeCreateCallback
        );
    }

}
