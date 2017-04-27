package com.commercetools.sync.commons.helpers;

/**
 * Class for creation {@link BaseSyncStatistics}. Newly created instance of {@link BaseSyncStatisticsCreator} produces
 * stats object with zero values. It shares methods for stats incrementation and for time measurement.
 *
 * Mutable operation are not synchronized, class is designed to be processed by single thread.
 */
public class BaseSyncStatisticsCreator {

    private int updated = 0;
    private int created = 0;
    private int failed = 0;
    private int processed = 0;
    private long startTime = 0L;
    private long endTime = 0L;

    public BaseSyncStatisticsCreator() { }

    /**
     * Stores the current time in the {@code startTime} instance variable, that will be used to calculation
     * of processing time. To get proper values in instance created by {@link BaseSyncStatisticsCreator#create()} please use
     * this function in conjunction with {@link BaseSyncStatisticsCreator#stopTimer()}:
     * <pre>
     *     instance.startTimer();
     *
     *     // perform sync process
     *
     *     instance.endTimer();
     * </pre>
     */
    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Stores the current time in the {@code endTime} instance variable, that will be used to calculation
     * of processing time. To get proper values in instance created by {@link BaseSyncStatisticsCreator#create()} please use
     * this function in conjunction with {@link BaseSyncStatisticsCreator#startTimer()}:
     * <pre>
     *     instance.startTimer();
     *
     *     // perform sync process
     *
     *     instance.stopTimer();
     * </pre>
     */
    public void stopTimer() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Increments the total number of resource updated.
     */
    public void incrementUpdated() {
        this.updated++;
    }

    /**
     * Increments the total number of resource created.
     */
    public void incrementCreated() {
        this.created++;
    }

    /**
     * Increments the total number of resources processed/synced.
     */
    public void incrementProcessed() {
        this.processed++;
    }

    /**
     * Increments the total number of resources that failed to sync.
     */
    public void incrementFailed() {
        this.failed++;
    }

    /**
     *
     * @return new instance of {@link BaseSyncStatistics} fulfilled with data from {@code this}
     * {@link BaseSyncStatisticsCreator}. The processing time is taken by the subtracting the time, when the
     * {@link BaseSyncStatisticsCreator#startTimer()} method of this instance was called, from the the time, when the
     * {@link BaseSyncStatisticsCreator#stopTimer()} method of this instance was called. Then substraction results in
     * negative number, the zero is taken instead.
     */
    public BaseSyncStatistics create() {
        long processingTime = endTime - startTime;
        processingTime = processingTime >= 0L ? processingTime : 0L;
        return new BaseSyncStatistics(processed, created, updated, failed, processingTime);
    }
}
