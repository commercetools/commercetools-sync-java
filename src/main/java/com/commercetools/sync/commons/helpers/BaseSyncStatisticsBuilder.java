package com.commercetools.sync.commons.helpers;

import javax.annotation.Nonnull;

public abstract class BaseSyncStatisticsBuilder<T extends BaseSyncStatisticsBuilder<T, S>,
    S extends BaseSyncStatistics> {

    protected int created = 0;
    protected int updated = 0;
    protected int upToDate = 0;
    protected int failed = 0;
    protected long processingTimeInMillis = 0;

    /**
     * Increments the total number of resource created.
     *
     * @return {@code this} instance of {@link T}
     */
    public T incrementCreated() {
        this.created++;
        return getThis();
    }

    /**
     * Increments the total number of resource updated.
     *
     * @return {@code this} instance of {@link T}
     */
    public T incrementUpdated() {
        this.updated++;
        return getThis();
    }

    /**
     * Increments the total number of resources that was up to date.
     *
     * @return {@code this} instance of {@link T}
     */
    public T incrementUpToDate() {
        this.upToDate++;
        return getThis();
    }

    /**
     * Increments the total number of resources that failed to sync.
     *
     * @return {@code this} instance of {@link T}
     */
    public T incrementFailed() {
        this.failed++;
        return getThis();
    }

    /**
     * Sets processing time in millis.
     *
     * @param processingTimeInMillis time of sync process in milliseconds.
     * @return {@code this} instance of {@link T}
     */
    public T setProcessingTimeInMillis(final long processingTimeInMillis) {
        this.processingTimeInMillis = processingTimeInMillis;
        return getThis();
    }

    /**
     * Adds all given statistics values to values held in {@code this} builder instance.
     *
     * @param statisticsToAdd statistics object that contains values that should be added
     * @return {@code this} instance of {@link T}
     */
    public T addAllStatistics(@Nonnull final S statisticsToAdd) {
        this.created += statisticsToAdd.getCreated();
        this.updated += statisticsToAdd.getUpdated();
        this.upToDate += statisticsToAdd.getUpToDate();
        this.failed += statisticsToAdd.getFailed();
        this.processingTimeInMillis += statisticsToAdd.getProcessingTimeInMillis();
        return getThis();
    }

    /**
     * Creates new instance of {@code S} which extends {@link BaseSyncStatistics} enriched with all attributes provided
     * to {@code this} builder.
     *
     * @return new instance of S which extends {@link BaseSyncStatistics}
     */
    public abstract S build();

    /**
     * Returns {@code this} instance of {@code T}, which extends {@link BaseSyncStatisticsBuilder}. The purpose of this
     * method is to make sure that {@code this} is an instance of a class which extends
     * {@link BaseSyncStatisticsBuilder} in order to be used in the generic methods of the class. Otherwise, without
     * this method, the methods above would need to cast {@code this to T} which could lead to a runtime error of the
     * class was extended in a wrong way.
     *
     * @return an instance of the class that overrides this method.
     */
    protected abstract T getThis();
}
