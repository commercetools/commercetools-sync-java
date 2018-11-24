package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.commands.UpdateCommand;
import io.sphere.sdk.models.Resource;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;

/**
 * @param <U> Resource (e.g. {@link io.sphere.sdk.products.Product}, {@link io.sphere.sdk.categories.Category}, etc..
 * @param <S> Subclass of {@link BaseSyncOptions}
 */
class BaseService<U extends Resource<U>, S extends BaseSyncOptions> {

    final S syncOptions;
    boolean isCached = false;
    final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();

    private static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;

    BaseService(@Nonnull final S syncOptions) {
        this.syncOptions = syncOptions;
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
     * this method executes the update command, computed by {@code updateCommandFunction}, on each batch.
     *
     * @param result                in the first call of this method, this result is normally a completed
     *                              future containing the resource to update, it is then used within each iteration of
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
        CompletionStage<U> resultStage = result;
        for (final List<UpdateAction<U>> batch : batches) {
            resultStage = resultStage.thenCompose(updatedProduct ->
                syncOptions.getCtpClient().execute(updateCommandFunction.apply(updatedProduct, batch)));
        }
        return resultStage;
    }
}
