package com.commercetools.sync.commons;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Versioned;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseSync<
    T, S extends Versioned, U extends BaseSyncStatistics, V extends BaseSyncOptions> {
  protected final U statistics;
  protected final V syncOptions;

  protected BaseSync(@Nonnull final U statistics, @Nonnull final V syncOptions) {
    this.statistics = statistics;
    this.syncOptions = syncOptions;
  }

  /**
   * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each
   * new resource in this list with it's corresponding old resource in a given CTP project, and in
   * turn it either issues update actions on the existing resource if it exists or create it if it
   * doesn't.
   *
   * @param resourceDrafts the list of new resources as drafts.
   * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an
   *     instance of {@code U} which is a subclass of {@link BaseSyncStatistics} representing the
   *     {@code statistics} instance attribute of {@code this} {@link BaseSync}.
   */
  protected abstract CompletionStage<U> process(@Nonnull List<T> resourceDrafts);

  /**
   * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each
   * new resource in this list with it's corresponding old resource in a given CTP project, and in
   * turn it either issues update actions on the existing resource if it exists or create it if it
   * doesn't.
   *
   * <p>The time before and after the actual sync process starts is recorded in the {@link
   * BaseSyncStatistics} container so that the total processing time is computed in the statistics.
   *
   * @param resourceDrafts the list of new resources as drafts.
   * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an
   *     instance of {@code U} which is a subclass of {@link BaseSyncStatistics} representing the
   *     {@code statistics} instance attribute of {@code this} {@link BaseSync}.
   */
  public CompletionStage<U> sync(@Nonnull final List<T> resourceDrafts) {
    statistics.startTimer();
    return process(resourceDrafts)
        .thenApply(
            resultingStatistics -> {
              resultingStatistics.calculateProcessingTime();
              return resultingStatistics;
            });
  }

  /**
   * Returns an instance of type U which is a subclass of {@link BaseSyncStatistics} containing all
   * the stats of the sync process; which includes a report message, the total number of update,
   * created, failed, processed resources and the processing time of the sync in different time
   * units and in a human readable format.
   *
   * @return a statistics object for the sync process.
   */
  @Nonnull
  public U getStatistics() {
    return statistics;
  }

  public V getSyncOptions() {
    return syncOptions;
  }

  /**
   * Given a list of resource (e.g. categories, products, etc.. batches represented by a {@link
   * List}&lt;{@link List}&gt; of resources, this method recursively calls {@link
   * #processBatch(List)} on each batch, then removes it, until there are no more batches, in other
   * words, all batches have been synced.
   *
   * @param batches the batches of resources to sync.
   * @param result in the first call of this recursive method, this result is normally a completed
   *     future, it used from within the method to recursively sync each batch once the previous
   *     batch has finished syncing.
   * @return an instance of {@link CompletionStage}&lt;{@code U}&gt; which contains as a result an
   *     instance of {@link BaseSyncStatistics} representing the {@code statistics} of the sync
   *     process executed on the given list of batches.
   */
  protected CompletionStage<U> syncBatches(
      @Nonnull final List<List<T>> batches, @Nonnull final CompletionStage<U> result) {
    if (batches.isEmpty()) {
      return result;
    }
    final List<T> firstBatch = batches.remove(0);
    return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
  }

  protected abstract CompletionStage<U> processBatch(@Nonnull List<T> batch);

  /**
   * This method checks if the supplied {@code sphereException} is an instance of {@link
   * ConcurrentModificationException}. If it is, then it executes the supplied {@code
   * onConcurrentModificationSupplier} {@link Supplier}. Otherwise, if it is not an instance of a
   * {@link ConcurrentModificationException} then it executes the other {@code
   * onOtherExceptionSupplier} {@link Supplier}. Regardless, which supplier is executed the results
   * of either is the result of this method.
   *
   * @param sphereException the sphere exception to check if is {@link
   *     ConcurrentModificationException}.
   * @param onConcurrentModificationSupplier the supplier to execute if the {@code sphereException}
   *     is a {@link ConcurrentModificationException}.
   * @param onOtherExceptionSupplier the supplier to execute if the {@code sphereException} is not a
   *     {@link ConcurrentModificationException}.
   * @param <S> the type of the result of the suppliers and this method.
   * @return the result of the executed supplier.
   */
  protected static <S> S executeSupplierIfConcurrentModificationException(
      @Nonnull final Throwable sphereException,
      @Nonnull final Supplier<S> onConcurrentModificationSupplier,
      @Nonnull final Supplier<S> onOtherExceptionSupplier) {
    final Throwable completionExceptionCause = sphereException.getCause();
    if (completionExceptionCause instanceof ConcurrentModificationException) {
      return onConcurrentModificationSupplier.get();
    }
    return onOtherExceptionSupplier.get();
  }

  /**
   * This method calls the optional error callback specified in the {@code syncOptions} and updates
   * the {@code statistics} instance by incrementing the total number of failed resources S to sync.
   *
   * @param errorMessage The error message describing the reason(s) of failure.
   * @param exception The exception that called caused the failure, if any.
   * @param oldResource the commercetools resource which could be updated.
   * @param newResourceDraft the commercetools resource draft where we get the new data.
   * @param updateActions the update actions to update the resource with.
   * @param failedTimes The number of times that the failed cart discount statistic counter is
   *     incremented.
   */
  @SuppressWarnings("unchecked")
  protected void handleError(
      @Nonnull final String errorMessage,
      @Nullable final Throwable exception,
      final S oldResource,
      final T newResourceDraft,
      final List<UpdateAction<S>> updateActions,
      final int failedTimes) {
    final SyncException syncException =
        exception != null
            ? new SyncException(errorMessage, exception)
            : new SyncException(errorMessage);
    syncOptions.applyErrorCallback(syncException, oldResource, newResourceDraft, updateActions);
    statistics.incrementFailed(failedTimes);
  }
}
