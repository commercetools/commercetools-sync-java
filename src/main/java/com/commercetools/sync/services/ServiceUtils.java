package com.commercetools.sync.services;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.commands.DraftBasedCreateCommand;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class ServiceUtils {

    /**
     * Applies the BeforeCreateCallback function on the supplied draft, then attempts to create it on CTP if it's not
     * empty, then applies the supplied handler on the response from CTP. If the draft was empty after applying the
     * callback an empty optional is returned as the resulting future.
     *
     * @param resourceDraft         draft to apply callback on and then create on CTP.
     * @param syncOptions           the sync options in which the CTP client is defined on and the callback.
     * @param createCommandFunction the create command query to create the resource on CTP.
     * @param responseHandler       defines how to handle the response from CTP.
     * @param <T>                   the type of the created resource.
     * @param <S>                   the type of the draft resource to create.
     * @return a future containing an optional which might contain the resource if successfully creater or empty
     *         otherwise.
     */
    public static <T, S> CompletionStage<Optional<T>> applyCallbackAndCreate(
            @Nonnull final S resourceDraft,
            @Nonnull final BaseSyncOptions<T, S> syncOptions,
            @Nonnull final Function<S, DraftBasedCreateCommand<T, S>> createCommandFunction,
            @Nonnull final TriFunction<S, T, Throwable, Optional<T>> responseHandler) {
        return syncOptions.applyBeforeCreateCallBack(resourceDraft)
                .map(mappedDraft -> syncOptions.getCtpClient().execute(createCommandFunction.apply(mappedDraft))
                        .handle((createdResource, sphereException) ->
                                responseHandler.apply(mappedDraft, createdResource, sphereException)))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));

    }

}
