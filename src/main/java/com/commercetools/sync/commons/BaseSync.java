package com.commercetools.sync.commons;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletionStage;


public abstract class BaseSync<T, U extends BaseSyncStatistics, V extends BaseSyncOptions> {
    protected final U statistics;
    protected final V syncOptions;

    protected BaseSync(@Nonnull final U statistics, @Nonnull final V syncOptions) {
        this.statistics = statistics;
        this.syncOptions = syncOptions;
    }

    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in
     * this list with it's corresponding old resource in a given CTP project, and in turn it either issues update
     * actions on the existing resource if it exists or create it if it doesn't.
     *
     * @param resourceDrafts the list of new resources as drafts.
     * @return an instance of {@link CompletionStage}&lt;{@link U}&gt; which contains as a result an instance of
     *      {@link U} which is a subclass of {@link BaseSyncStatistics} representing the {@code statistics} instance
     *      attribute of {@code this} {@link BaseSync}.
     */
    protected abstract CompletionStage<U> process(@Nonnull final List<T> resourceDrafts);


    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in
     * this list with it's corresponding old resource in a given CTP project, and in turn it either issues update
     * actions on the existing resource if it exists or create it if it doesn't.
     *
     * <p>The time before and after the actual sync process starts is recorded in the {@link BaseSyncStatistics}
     * container so that the total processing time is computed in the statistics.
     *
     * @param resourceDrafts the list of new resources as drafts.
     * @return an instance of {@link CompletionStage}&lt;{@link U}&gt; which contains as a result an instance of
     *      {@link U} which is a subclass of {@link BaseSyncStatistics} representing the {@code statistics} instance
     *      attribute of {@code this} {@link BaseSync}.
     */
    public CompletionStage<U> sync(@Nonnull final List<T> resourceDrafts) {
        statistics.startTimer();
        return process(resourceDrafts).thenApply(resultingStatistics -> {
            resultingStatistics.calculateProcessingTime();
            return resultingStatistics;
        });
    }


    /**
     * Returns an instance of type U which is a subclass of {@link BaseSyncStatistics} containing all the stats of the
     * sync process; which includes a report message, the total number of update, created, failed, processed resources
     * and the processing time of the sync in different time units and in a human readable format.
     *
     * @return a statistics object for the sync process.
     */
    @Nonnull
    public U getStatistics() {
        return statistics;
    }

    /**
     * Given a list of resource (e.g. categories, products, etc..  batches represented by a
     * {@link List}&lt;{@link List}&gt; of resources, this method recursively calls
     * {@link #processBatch(List)} on each batch, then removes it, until there are no more batches, in other words,
     * all batches have been synced.
     *
     * @param batches the batches of resources to sync.
     * @param result  in the first call of this recursive method, this result is normally a completed future, it
     *                used from within the method to recursively sync each batch once the previous batch has
     *                finished syncing.
     * @return an instance of {@link CompletionStage}&lt;{@link U}&gt; which contains as a result an instance of
     *      {@link BaseSyncStatistics} representing the {@code statistics} of the sync process executed on the
     *      given list of batches.
     */
    protected abstract CompletionStage<U> syncBatches(@Nonnull final List<List<T>> batches,
                                                      @Nonnull final CompletionStage<U> result);

    protected abstract CompletionStage<U> processBatch(@Nonnull final List<T> batch);
}
