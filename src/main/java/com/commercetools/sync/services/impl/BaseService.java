package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.commands.DraftBasedCreateCommand;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @param <U> Resource (e.g. {@link io.sphere.sdk.products.Product}, {@link io.sphere.sdk.categories.Category}, etc..
 * @param <V> Resource Draft (e.g. {@link io.sphere.sdk.products.ProductDraft},
 *            {@link io.sphere.sdk.categories.CategoryDraft}, etc..
 */
class BaseService<U extends Resource<U>, V> {
    final BaseSyncOptions<U, V> syncOptions;
    boolean isCached = false;
    final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();


    BaseService(@Nonnull final BaseSyncOptions<U, V> syncOptions) {
        this.syncOptions = syncOptions;
    }

    /**
     * Applies the BeforeCreateCallback function on a supplied draft, then attempts to create it on CTP if it's not
     * empty, then applies the supplied handler on the response from CTP. If the draft was empty after applying the
     * callback an empty optional is returned as the resulting future.
     *
     * @param resourceDraft         draft to apply callback on and then create on CTP.
     * @param createCommandFunction the create command query to create the resource on CTP.
     * @param responseHandler       defines how to handle the response from CTP.
     * @return a future containing an optional which might contain the resource if successfully created or empty
     *         otherwise.
     */
    @Nonnull
    CompletionStage<Optional<U>> applyCallbackAndCreate(
        @Nonnull final V resourceDraft,
        @Nonnull final Function<V, DraftBasedCreateCommand<U, V>> createCommandFunction,
        @Nonnull final TriFunction<V, U, Throwable, Optional<U>> responseHandler) {
        return syncOptions.applyBeforeCreateCallBack(resourceDraft)
                          .map(mappedDraft -> syncOptions.getCtpClient()
                                                         .execute(createCommandFunction.apply(mappedDraft))
                                                         .handle((createdResource, sphereException) ->
                                                             responseHandler
                                                                 .apply(mappedDraft, createdResource, sphereException)))
                          .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));

    }
}
