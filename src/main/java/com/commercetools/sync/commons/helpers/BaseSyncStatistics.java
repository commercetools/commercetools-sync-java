package com.commercetools.sync.commons.helpers;

import io.netty.util.internal.StringUtil;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class BaseSyncStatistics {
    protected String reportMessage;
    private int updated;
    private int created;
    private int failed;
    private int processed;
    private long latestBatchStartTime;
    private long latestBatchProcessingTimeInDays;
    private long latestBatchProcessingTimeInHours;
    private long latestBatchProcessingTimeInMinutes;
    private long latestBatchProcessingTimeInSeconds;
    private long latestBatchProcessingTimeInMillis;
    private String latestBatchHumanReadableProcessingTime;


    public BaseSyncStatistics() {
        reportMessage = StringUtil.EMPTY_STRING;
        latestBatchHumanReadableProcessingTime = StringUtil.EMPTY_STRING;
    }

    /**
     * Stores the current time of instantiation in the {@code latestBatchStartTime} instance variable that will be used
     * later when {@link BaseSyncStatistics#calculateProcessingTime()} is called to calculate the total time of
     * processing.
     */
    public void startTimer() {
        latestBatchStartTime = System.currentTimeMillis();
    }

    /**
     * Gets the total number of resources that were updated.
     *
     * @return total number of resources that were updated.
     */
    public int getUpdated() {
        return updated;
    }

    /**
     * Increments the total number of resource that were updated.
     */
    public void incrementUpdated() {
        this.updated++;
    }

    /**
     * Increments the total number of resources that were updated by the supplied times.
     */
    public void incrementUpdated(final int times) {
        this.updated += times;
    }

    /**
     * Gets the total number of resources that were created.
     *
     * @return total number of resources that were created.
     */
    public int getCreated() {
        return created;
    }

    /**
     * Increments the total number of resource that were created.
     */
    public void incrementCreated() {
        this.created++;
    }

    /**
     * Increments the total number of resources that were created by the supplied times.
     */
    public void incrementCreated(final int times) {
        this.created += times;
    }

    /**
     * Gets the total number of resources that were processed/synced.
     *
     * @return total number of resources that were processed/synced.
     */
    public int getProcessed() {
        return processed;
    }

    /**
     * Increments the total number of resources that were processed/synced.
     */
    public void incrementProcessed() {
        this.processed++;
    }

    /**
     * Increments the total number of resources that were processed/synced by the supplied times.
     */
    public void incrementProcessed(final int times) {
        this.processed += times;
    }

    /**
     * Gets the total number of resources that failed to sync.
     *
     * @return total number of resources that failed to sync.
     */
    public int getFailed() {
        return failed;
    }

    /**
     * Increments the total number of resources that failed to sync.
     */
    public void incrementFailed() {
        this.failed++;
    }

    /**
     * Increments the total number of resources that failed to sync by the supplied times.
     */
    public void incrementFailed(final int times) {
        this.failed += times;
    }

    /**
     * Calculates the processing time taken by the subtracting the time, when the
     * {@link BaseSyncStatistics#startTimer()} method of this instance was called, from the current time in
     * Milliseconds. It also sets the processing time in all the units {@code latestBatchProcessingTimeInDays},
     * {@code latestBatchProcessingTimeInHours}, {@code latestBatchProcessingTimeInMinutes},
     * {@code latestBatchProcessingTimeInSeconds} and
     * {@code latestBatchProcessingTimeInMillis}. It also builds a human readable processing time, as string, in the
     * following format @{code "0d, 0h, 0m, 2s, 545ms"} and stores it in the publicly exposed
     * variable {@code latestBatchHumanReadableProcessingTime}.
     */
    public void calculateProcessingTime() {
        setProcessingTimeInAllUnits();
        setHumanReadableProcessingTime();
    }

    /**
     * Calculates the processing time taken by the subtracting the time when this {@link BaseSyncStatistics} instance
     * was instantiated from the current time in Milliseconds. It sets the processing time in all the units
     * {@code latestBatchProcessingTimeInDays}, {@code latestBatchProcessingTimeInHours},
     * {@code latestBatchProcessingTimeInMinutes},
     * {@code latestBatchProcessingTimeInSeconds} and {@code latestBatchProcessingTimeInMillis}.
     */
    private void setProcessingTimeInAllUnits() {
        latestBatchProcessingTimeInMillis = System.currentTimeMillis() - this.latestBatchStartTime;
        latestBatchProcessingTimeInDays = TimeUnit.MILLISECONDS.toDays(latestBatchProcessingTimeInMillis);
        latestBatchProcessingTimeInHours = TimeUnit.MILLISECONDS.toHours(latestBatchProcessingTimeInMillis);
        latestBatchProcessingTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(latestBatchProcessingTimeInHours);
        latestBatchProcessingTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(latestBatchProcessingTimeInMillis);
    }

    /**
     * Builds a human readable processing time, as string, in the following format @{code "0d, 0h, 0m, 2s, 545ms"}
     * and stores it in the publicly exposed variable {@code latestBatchHumanReadableProcessingTime}.
     */
    private void setHumanReadableProcessingTime() {
        final long completeDaysInHours = TimeUnit.DAYS.toHours(latestBatchProcessingTimeInDays);
        final long completeHoursInMinutes = TimeUnit.HOURS.toMinutes(latestBatchProcessingTimeInHours);
        final long completeMinutesInSeconds = TimeUnit.MINUTES.toSeconds(latestBatchProcessingTimeInMinutes);
        final long completeSecondsInMillis = TimeUnit.SECONDS.toMillis(latestBatchProcessingTimeInSeconds);

        final long remainingHours = latestBatchProcessingTimeInHours - completeDaysInHours;
        final long remainingMinutes = latestBatchProcessingTimeInMinutes - completeHoursInMinutes;
        final long remainingSeconds = latestBatchProcessingTimeInSeconds - completeMinutesInSeconds;
        final long remainingMillis = latestBatchProcessingTimeInMillis - completeSecondsInMillis;

        latestBatchHumanReadableProcessingTime = format("%dd, %dh, %dm, %ds, %dms",
            latestBatchProcessingTimeInDays,
          remainingHours,
          remainingMinutes,
          remainingSeconds,
          remainingMillis
        );
    }

    /**
     * Gets the human readable processing time in the following format @{code "0d, 0h, 0m, 2s, 545ms"}.
     *
     * @return the human readable processing time in the following format @{code "0d, 0h, 0m, 2s, 545ms"}
     */
    public String getLatestBatchHumanReadableProcessingTime() {
        return latestBatchHumanReadableProcessingTime;
    }

    /**
     * Gets the number of days it took to process.
     *
     * @return number of days taken to process.
     */
    public long getLatestBatchProcessingTimeInDays() {
        return latestBatchProcessingTimeInDays;
    }

    /**
     * Gets the number of hours it took to process.
     *
     * @return number of hours taken to process.
     */
    public long getLatestBatchProcessingTimeInHours() {
        return latestBatchProcessingTimeInHours;
    }

    /**
     * Gets the number of minutes it took to process.
     *
     * @return number of minutes taken to process.
     */
    public long getLatestBatchProcessingTimeInMinutes() {
        return latestBatchProcessingTimeInMinutes;
    }

    /**
     * Gets the number of seconds it took to process.
     *
     * @return number of seconds taken to process.
     */
    public long getLatestBatchProcessingTimeInSeconds() {
        return latestBatchProcessingTimeInSeconds;
    }

    /**
     * Gets the number of milliseconds it took to process.
     *
     * @return number of milliseconds taken to process.
     */
    public long getLatestBatchProcessingTimeInMillis() {
        return latestBatchProcessingTimeInMillis;
    }

    /**
     * Gets a summary message of the statistics report.
     *
     * @return a summary message of the statistics report.
     */
    public String getReportMessage() {
        return reportMessage;
    }
}
