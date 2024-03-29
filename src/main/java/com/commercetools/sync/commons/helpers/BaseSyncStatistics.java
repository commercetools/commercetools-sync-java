package com.commercetools.sync.commons.helpers;

import static java.lang.String.format;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

public abstract class BaseSyncStatistics<SyncStatisticsT extends BaseSyncStatistics> {
  private AtomicInteger updated;
  private AtomicInteger created;
  private AtomicInteger failed;
  private AtomicInteger processed;
  private long latestBatchStartTime;
  private long latestBatchProcessingTimeInDays;
  private long latestBatchProcessingTimeInHours;
  private long latestBatchProcessingTimeInMinutes;
  private long latestBatchProcessingTimeInSeconds;
  private long latestBatchProcessingTimeInMillis;
  private String latestBatchHumanReadableProcessingTime;

  /**
   * Creates a new {@link BaseSyncStatistics} with initial values {@code 0} of created, updated,
   * failed and processed counters, an empty reportMessage and
   * latestBatchHumanReadableProcessingTime.
   */
  public BaseSyncStatistics() {
    updated = new AtomicInteger();
    created = new AtomicInteger();
    failed = new AtomicInteger();
    processed = new AtomicInteger();
    latestBatchHumanReadableProcessingTime = "";
  }

  /**
   * Stores the current time of instantiation in the {@code latestBatchStartTime} instance variable
   * that will be used later when {@link BaseSyncStatistics#calculateProcessingTime()} is called to
   * calculate the total time of processing.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   */
  public void startTimer() {
    latestBatchStartTime = System.currentTimeMillis();
  }

  /**
   * Gets the total number of resources that were updated.
   *
   * @return total number of resources that were updated.
   */
  public AtomicInteger getUpdated() {
    return updated;
  }

  /** Increments the total number of resource that were updated. */
  public void incrementUpdated() {
    updated.incrementAndGet();
  }

  /**
   * Increments the total number of resources that were updated by the supplied times.
   *
   * @param times the total number of times to increment.
   */
  public void incrementUpdated(final int times) {
    updated.addAndGet(times);
  }

  /**
   * Gets the total number of resources that were created.
   *
   * @return total number of resources that were created.
   */
  public AtomicInteger getCreated() {
    return created;
  }

  /** Increments the total number of resource that were created. */
  public void incrementCreated() {
    created.incrementAndGet();
  }

  /**
   * Increments the total number of resources that were created by the supplied times.
   *
   * @param times the total number of times to increment.
   */
  public void incrementCreated(final int times) {
    created.addAndGet(times);
  }

  /**
   * Gets the total number of resources that were processed/synced.
   *
   * @return total number of resources that were processed/synced.
   */
  public AtomicInteger getProcessed() {
    return processed;
  }

  /** Increments the total number of resources that were processed/synced. */
  public void incrementProcessed() {
    processed.incrementAndGet();
  }

  /**
   * Increments the total number of resources that were processed/synced by the supplied times.
   *
   * @param times the total number of times to increment.
   */
  public void incrementProcessed(final int times) {
    processed.addAndGet(times);
  }

  /**
   * Gets the total number of resources that failed to sync.
   *
   * @return total number of resources that failed to sync.
   */
  public AtomicInteger getFailed() {
    return failed;
  }

  /** Increments the total number of resources that failed to sync. */
  public void incrementFailed() {
    failed.incrementAndGet();
  }

  /**
   * Increments the total number of resources that failed to sync by the supplied times.
   *
   * @param times the total number of times to increment.
   */
  public void incrementFailed(final int times) {
    failed.addAndGet(times);
  }

  /**
   * Calculates the processing time taken by the subtracting the time, when the {@link
   * BaseSyncStatistics#startTimer()} method of this instance was called, from the current time in
   * Milliseconds. It also sets the processing time in all the units {@code
   * latestBatchProcessingTimeInDays}, {@code latestBatchProcessingTimeInHours}, {@code
   * latestBatchProcessingTimeInMinutes}, {@code latestBatchProcessingTimeInSeconds} and {@code
   * latestBatchProcessingTimeInMillis}. It also builds a human readable processing time, as string,
   * in the following format @{code "0d, 0h, 0m, 2s, 545ms"} and stores it in the publicly exposed
   * variable {@code latestBatchHumanReadableProcessingTime}.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   */
  public void calculateProcessingTime() {
    setProcessingTimeInAllUnits();
    setHumanReadableProcessingTime();
  }

  /**
   * Calculates the processing time taken by the subtracting the time when this {@link
   * BaseSyncStatistics} instance was instantiated from the current time in Milliseconds. It sets
   * the processing time in all the units {@code latestBatchProcessingTimeInDays}, {@code
   * latestBatchProcessingTimeInHours}, {@code latestBatchProcessingTimeInMinutes}, {@code
   * latestBatchProcessingTimeInSeconds} and {@code latestBatchProcessingTimeInMillis}.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   */
  private void setProcessingTimeInAllUnits() {
    latestBatchProcessingTimeInMillis = System.currentTimeMillis() - this.latestBatchStartTime;
    latestBatchProcessingTimeInDays =
        TimeUnit.MILLISECONDS.toDays(latestBatchProcessingTimeInMillis);
    latestBatchProcessingTimeInHours =
        TimeUnit.MILLISECONDS.toHours(latestBatchProcessingTimeInMillis);
    latestBatchProcessingTimeInMinutes =
        TimeUnit.MILLISECONDS.toMinutes(latestBatchProcessingTimeInMillis);
    latestBatchProcessingTimeInSeconds =
        TimeUnit.MILLISECONDS.toSeconds(latestBatchProcessingTimeInMillis);
  }

  /**
   * Builds a human readable processing time, as string, in the following format @{code "0d, 0h, 0m,
   * 2s, 545ms"} and stores it in the publicly exposed variable {@code
   * latestBatchHumanReadableProcessingTime}.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   */
  private void setHumanReadableProcessingTime() {
    final long completeDaysInHours = TimeUnit.DAYS.toHours(latestBatchProcessingTimeInDays);
    final long completeHoursInMinutes = TimeUnit.HOURS.toMinutes(latestBatchProcessingTimeInHours);
    final long completeMinutesInSeconds =
        TimeUnit.MINUTES.toSeconds(latestBatchProcessingTimeInMinutes);
    final long completeSecondsInMillis =
        TimeUnit.SECONDS.toMillis(latestBatchProcessingTimeInSeconds);

    final long remainingHours = latestBatchProcessingTimeInHours - completeDaysInHours;
    final long remainingMinutes = latestBatchProcessingTimeInMinutes - completeHoursInMinutes;
    final long remainingSeconds = latestBatchProcessingTimeInSeconds - completeMinutesInSeconds;
    final long remainingMillis = latestBatchProcessingTimeInMillis - completeSecondsInMillis;

    latestBatchHumanReadableProcessingTime =
        format(
            "%dd, %dh, %dm, %ds, %dms",
            latestBatchProcessingTimeInDays,
            remainingHours,
            remainingMinutes,
            remainingSeconds,
            remainingMillis);
  }

  /**
   * Gets the human readable processing time in the following format @{code "0d, 0h, 0m, 2s,
   * 545ms"}.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return the human readable processing time in the following format @{code "0d, 0h, 0m, 2s,
   *     545ms"}
   */
  public String getLatestBatchHumanReadableProcessingTime() {
    return latestBatchHumanReadableProcessingTime;
  }

  /**
   * Gets the number of days it took to process.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return number of days taken to process.
   */
  public long getLatestBatchProcessingTimeInDays() {
    return latestBatchProcessingTimeInDays;
  }

  /**
   * Gets the number of hours it took to process.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return number of hours taken to process.
   */
  public long getLatestBatchProcessingTimeInHours() {
    return latestBatchProcessingTimeInHours;
  }

  /**
   * Gets the number of minutes it took to process.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return number of minutes taken to process.
   */
  public long getLatestBatchProcessingTimeInMinutes() {
    return latestBatchProcessingTimeInMinutes;
  }

  /**
   * Gets the number of seconds it took to process.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return number of seconds taken to process.
   */
  public long getLatestBatchProcessingTimeInSeconds() {
    return latestBatchProcessingTimeInSeconds;
  }

  /**
   * Gets the number of milliseconds it took to process.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return number of milliseconds taken to process.
   */
  public long getLatestBatchProcessingTimeInMillis() {
    return latestBatchProcessingTimeInMillis;
  }

  /**
   * Gets a summary message of the statistics report.
   *
   * <p>Note: This method isn't thread-safe and shouldn't be used in a concurrent context.
   *
   * @return a summary message of the statistics report.
   */
  public abstract String getReportMessage();

  /**
   * Gets the name of the implementation class extending this abstract class. This name is then
   * serialized into JSON and stored in the custom object of the last sync statistics. Later we
   * could use this name to deserialize the custom object into the correct implementation class.
   *
   * @return name of the implementation class extending this abstract class.
   */
  public String getSyncStatisticsClassName() {
    return getThis().getClass().getName();
  }

  /**
   * Returns {@code this} instance of {@code T}, which extends {@link
   * com.commercetools.sync.commons.helpers.BaseSyncStatistics}. The purpose of this method is to
   * make sure that {@code this} is an instance of a class which extends {@link
   * com.commercetools.sync.commons.helpers.BaseSyncStatistics} in order to be used in the generic
   * methods of the class. Otherwise, without this method, the methods above would need to cast
   * {@code this to T} which could lead to a runtime error of the class was extended in a wrong way.
   *
   * @return an instance of the class that overrides this method.
   */
  protected abstract SyncStatisticsT getThis();

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final BaseSyncStatistics<?> that = (BaseSyncStatistics<?>) o;
    return latestBatchStartTime == that.latestBatchStartTime
        && Objects.equals(getSyncStatisticsClassName(), that.getSyncStatisticsClassName())
        && getLatestBatchProcessingTimeInDays() == that.getLatestBatchProcessingTimeInDays()
        && getLatestBatchProcessingTimeInHours() == that.getLatestBatchProcessingTimeInHours()
        && getLatestBatchProcessingTimeInMinutes() == that.getLatestBatchProcessingTimeInMinutes()
        && getLatestBatchProcessingTimeInSeconds() == that.getLatestBatchProcessingTimeInSeconds()
        && getLatestBatchProcessingTimeInMillis() == that.getLatestBatchProcessingTimeInMillis()
        && getUpdated().get() == that.getUpdated().get()
        && getCreated().get() == that.getCreated().get()
        && getFailed().get() == that.getFailed().get()
        && getProcessed().get() == that.getProcessed().get()
        && Objects.equals(
            getLatestBatchHumanReadableProcessingTime(),
            that.getLatestBatchHumanReadableProcessingTime());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getSyncStatisticsClassName(),
        getUpdated().get(),
        getCreated().get(),
        getFailed().get(),
        getProcessed().get(),
        latestBatchStartTime,
        getLatestBatchProcessingTimeInDays(),
        getLatestBatchProcessingTimeInHours(),
        getLatestBatchProcessingTimeInMinutes(),
        getLatestBatchProcessingTimeInSeconds(),
        getLatestBatchProcessingTimeInMillis(),
        getLatestBatchHumanReadableProcessingTime());
  }

  /**
   * Builds a proper summary message of the statistics report of a given {@code resourceString} in
   * following format:
   *
   * <p>"Summary: 2 resources were processed in total (2 created, 2 updated and 0 failed to sync)."
   *
   * @param resourceString a string representing the resource
   * @return a summary message of the statistics report
   */
  protected String getDefaultReportMessageForResource(@Nonnull final String resourceString) {
    return format(
        "Summary: %s %s were processed in total (%s created, %s updated and %s failed to sync).",
        getProcessed(), resourceString, getCreated(), getUpdated(), getFailed());
  }
}
