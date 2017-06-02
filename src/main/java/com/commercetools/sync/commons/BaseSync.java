package com.commercetools.sync.commons;

import com.commercetools.sync.commons.helpers.BaseSyncStatistics;
import com.commercetools.sync.commons.helpers.BaseSyncStatisticsBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletionStage;


public abstract class BaseSync<T, S extends BaseSyncStatistics, U extends BaseSyncStatisticsBuilder<U, S>,
    V extends BaseSyncOptions> {
    private final U totalStatisticsBuilder;
    private Object totalStatisticsLock;
    protected final V syncOptions;

    protected BaseSync(@Nonnull final U totalStatisticsBuilder, @Nonnull final V syncOptions) {
        this.totalStatisticsBuilder = totalStatisticsBuilder;
        this.syncOptions = syncOptions;
        this.totalStatisticsLock = new Object();
    }

    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in
     * this list with it's corresponding old resource in a given CTP project, and in turn it either issues update
     * actions on the existing resource if it exists or create it if it doesn't.
     *
     * <p>This method should return a subclass of {@link BaseSyncStatisticsBuilder} wrapped in a
     * {@link CompletionStage}. The {@code process} method is responsible for creating an instance of statistics builder
     * and for updating its counters with relevant values. However it is a responsibility of the caller for calculating
     * the total processing time and for setting that time on the returned instance. You can see how it is used by
     * {@link BaseSync#sync(List)}.
     *
     * @param resourceDrafts the list of new resources as drafts.
     * @return an instance of {@link CompletionStage&lt;S&gt;} which contains as a result an instance of {@link U} which
     *      is a subclass of {@link BaseSyncStatisticsBuilder} representing the statistics of a single sync performed by
     *      this method.
     */
    protected abstract CompletionStage<U> process(@Nonnull final List<T> resourceDrafts);


    /**
     * Given a list of resource (e.g. categories, products, etc..) drafts. This method compares each new resource in
     * this list with it's corresponding old resource in a given CTP project, and in turn it either issues update
     * actions on the existing resource if it exists or create it if it doesn't.
     *
     * @param resourceDrafts the list of new resources as drafts.
     * @return an instance of {@link CompletionStage&lt;S&gt;} which contains as a result an instance of {@link S} which
     *      is a subclass of {@link BaseSyncStatistics} representing the {@code statistics} of this sync process.
     */
    public CompletionStage<S> sync(@Nonnull final List<T> resourceDrafts) {
        final long startTime = System.currentTimeMillis();
        return process(resourceDrafts).thenApply(resultingStatisticsBuilder -> {
            resultingStatisticsBuilder.setProcessingTimeInMillis(System.currentTimeMillis() - startTime);
            final S resultingStatistics = resultingStatisticsBuilder.build();
            updateTotalStatistics(resultingStatistics);
            return resultingStatistics;
        });
    }


    /**
     * Returns an instance of type S which is a subclass of {@link BaseSyncStatistics} containing the total stats of
     * all sync processes performed by {@code this} sync instance.
     *
     * @return a statistics object for all sync processes performed by {@code this} sync instance.
     */
    @Nonnull
    public S getStatistics() {
        synchronized (totalStatisticsLock) {
            return totalStatisticsBuilder.build();
        }
    }

    /**
     * Join given statistics to total stats of all sync processes performed by {@code this} sync instance.
     *
     * @param statisticsToJoin statistics of sync that should be joined to total stats
     */
    private void updateTotalStatistics(final S statisticsToJoin) {
        synchronized (totalStatisticsLock) {
            totalStatisticsBuilder.addAllStatistics(statisticsToJoin);
        }
    }
}
