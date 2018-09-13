package com.commercetools.sync.types;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TypeSyncOptions extends BaseSyncOptions<Type, TypeDraft> {

    TypeSyncOptions(
        @Nonnull final SphereClient ctpClient,
        @Nullable final BiConsumer<String, Throwable> updateActionErrorCallBack,
        @Nullable final Consumer<String> updateActionWarningCallBack,
        final int batchSize,
        final boolean allowUuid,
        @Nullable final TriFunction<List<UpdateAction<Type>>, TypeDraft,
                Type, List<UpdateAction<Type>>> beforeUpdateCallback,
        @Nullable final Function<TypeDraft, TypeDraft> beforeCreateCallback
    ) {

        super(
            ctpClient,
            updateActionErrorCallBack,
            updateActionWarningCallBack,
            batchSize,
            allowUuid,
            beforeUpdateCallback,
            beforeCreateCallback
        );
    }

}
