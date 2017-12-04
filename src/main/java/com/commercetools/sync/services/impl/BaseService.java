package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.commands.DraftBasedCreateCommand;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.commands.UpdateCommand;
import io.sphere.sdk.models.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
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

    private static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";
    private static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;

    BaseService(@Nonnull final BaseSyncOptions<U, V> syncOptions) {
        this.syncOptions = syncOptions;
    }

    /**
     * Applies the BeforeCreateCallback function on a supplied draft, then attempts to create it on CTP if it's not
     * empty, then applies the supplied handler on the response from CTP. If the draft was empty after applying the
     * callback an empty optional is returned as the resulting future.
     *
     * @param resourceDraft         draft to apply callback on and then create on CTP.
     * @param draftKey              draft key.
     * @param createCommandFunction the create command query to create the resource on CTP.
     * @return a future containing an optional which might contain the resource if successfully created or empty
     *         otherwise.
     */
    @Nonnull
    CompletionStage<Optional<U>> applyCallbackAndCreate(
        @Nonnull final V resourceDraft,
        @Nullable final String draftKey,
        @Nonnull final Function<V, DraftBasedCreateCommand<U, V>> createCommandFunction) {
        if (isBlank(draftKey)) {
            syncOptions.applyErrorCallback(format(CREATE_FAILED, draftKey, "Draft key is blank!"));
            return CompletableFuture.completedFuture(Optional.empty());
        } else {
            final BiFunction<U, Throwable, Optional<U>> responseHandler =
                (createdResource, sphereException) ->
                    handleResourceCreation(draftKey, createdResource, sphereException);
            return syncOptions.applyBeforeCreateCallBack(resourceDraft)
                              .map(mappedDraft ->
                                  syncOptions.getCtpClient()
                                             .execute(createCommandFunction.apply(mappedDraft))
                                             .handle(responseHandler)
                              )
                              .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));
        }
    }

    @Nonnull
    private Optional<U> handleResourceCreation(@Nonnull final String draftKey,
                                               @Nullable final U createdResource,
                                               @Nullable final Throwable sphereException) {
        if (createdResource != null) {
            keyToIdCache.put(draftKey, createdResource.getId());
            return Optional.of(createdResource);
        } else {
            syncOptions.applyErrorCallback(format(CREATE_FAILED, draftKey, sphereException), sphereException);
            return Optional.empty();
        }
    }

    /**
     * Executes update request(s) on the {@code resource} with all the {@code updateActions} using the
     * {@code updateCommandFunction} while taking care of the CTP constraint of 500 update actions per request by
     * batching update actions into requests of 500 actions each.
     *
     * @param resource              The resource to update.
     * @param updateCommandFunction a {@link BiFunction} used to compute the update command required to update the
     *                              resource.
     * @param updateActions         the update actions to execute on the resource.
     * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an instance of
     *         the resource {@link U} after all the update actions have been executed.
     */
    @Nonnull
    CompletionStage<U> updateResource(
        @Nonnull final U resource,
        @Nonnull final BiFunction<U, List<? extends UpdateAction<U>>, UpdateCommand<U>> updateCommandFunction,
        @Nonnull final List<UpdateAction<U>> updateActions) {
        final List<List<UpdateAction<U>>> actionBatches = batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);
        return updateBatches(CompletableFuture.completedFuture(resource), updateCommandFunction, actionBatches);
    }

    /**
     * Given a list of update actions batches represented by a {@link List}&lt;{@link List}&gt; of {@link UpdateAction},
     * this method recursively executes the update command, computed by {@code updateCommandFunction}, on each batch,
     * then removes the batch, until there are no more batches.
     *
     * @param result                in the first call of this recursive method, this result is normally a completed
     *                              future containing the resource to update, it is then used withing each recursive
     *                              batch execution to have the latest resource (with version) once the previous batch
     *                              has finished execution.
     * @param updateCommandFunction a {@link BiFunction} used to compute the update command required to update the
     *                              resource.
     * @param batches               the batches of update actions to execute.
     * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an instance of
     *         the resource {@link U} after all the update actions in all batches have been executed.
     */
    @Nonnull
    private CompletionStage<U> updateBatches(
        @Nonnull final CompletionStage<U> result,
        @Nonnull final BiFunction<U, List<? extends UpdateAction<U>>, UpdateCommand<U>> updateCommandFunction,
        @Nonnull final List<List<UpdateAction<U>>> batches) {
        if (batches.isEmpty()) {
            return result;
        }
        final List<UpdateAction<U>> nextBatch = batches.remove(0);
        final CompletionStage<U> nextBatchUpdateFuture = result.thenCompose(updatedProduct ->
            syncOptions.getCtpClient().execute(updateCommandFunction.apply(updatedProduct, nextBatch))
        );
        return updateBatches(nextBatchUpdateFuture, updateCommandFunction, batches);
    }
}
