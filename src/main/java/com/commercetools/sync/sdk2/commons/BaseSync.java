package com.commercetools.sync.sdk2.commons;

import com.commercetools.sync.sdk2.commons.helpers.BaseSyncStatistics;
import io.vrap.rmf.base.client.error.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

public abstract class BaseSync<
    DraftT, SyncStatisticsT extends BaseSyncStatistics, SyncOptionsT extends BaseSyncOptions> {
  protected final SyncStatisticsT statistics;
  protected final SyncOptionsT syncOptions;

  protected BaseSync(
      @Nonnull final SyncStatisticsT statistics, @Nonnull final SyncOptionsT syncOptions) {
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
  protected abstract CompletionStage<SyncStatisticsT> process(@Nonnull List<DraftT> resourceDrafts);

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
  public CompletionStage<SyncStatisticsT> sync(@Nonnull final List<DraftT> resourceDrafts) {
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
  public SyncStatisticsT getStatistics() {
    return statistics;
  }

  public SyncOptionsT getSyncOptions() {
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
  protected CompletionStage<SyncStatisticsT> syncBatches(
      @Nonnull final List<List<DraftT>> batches,
      @Nonnull final CompletionStage<SyncStatisticsT> result) {
    if (batches.isEmpty()) {
      return result;
    }
    final List<DraftT> firstBatch = batches.remove(0);
    return syncBatches(batches, result.thenCompose(subResult -> processBatch(firstBatch)));
  }

  protected abstract CompletionStage<SyncStatisticsT> processBatch(@Nonnull List<DraftT> batch);

  /**
   * This method checks if the supplied {@code exception} is an instance of {@link
   * ConcurrentModificationException}. If it is, then it executes the supplied {@code
   * onConcurrentModificationSupplier} {@link Supplier}. Otherwise, if it is not an instance of a
   * {@link ConcurrentModificationException} then it executes the other {@code
   * onOtherExceptionSupplier} {@link Supplier}. Regardless, which supplier is executed the results
   * of either is the result of this method.
   *
   * @param exception the commercetools exception to check if is {@link
   *     ConcurrentModificationException}.
   * @param onConcurrentModificationSupplier the supplier to execute if the {@code exception} is a
   *     {@link ConcurrentModificationException}.
   * @param onOtherExceptionSupplier the supplier to execute if the {@code exception} is not a
   *     {@link ConcurrentModificationException}.
   * @param <ResultT> the type of the result of the suppliers and this method.
   * @return the result of the executed supplier.
   */
  protected static <ResultT> ResultT executeSupplierIfConcurrentModificationException(
      @Nonnull final Throwable exception,
      @Nonnull final Supplier<ResultT> onConcurrentModificationSupplier,
      @Nonnull final Supplier<ResultT> onOtherExceptionSupplier) {

    final Throwable completionExceptionCause = exception.getCause();
    if (completionExceptionCause instanceof ConcurrentModificationException) {
      return onConcurrentModificationSupplier.get();
    }
    return onOtherExceptionSupplier.get();
  }
}
