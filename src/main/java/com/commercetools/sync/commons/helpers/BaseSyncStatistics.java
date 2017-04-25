package com.commercetools.sync.commons.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class BaseSyncStatistics {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSyncStatistics.class);

    private String reportMessage;
    private int updated;
    private int created;
    private int failed;
    private int processed;
    private long startTime;
    private long processingTimeInDays;
    private long processingTimeInHours;
    private long processingTimeInMinutes;
    private long processingTimeInSeconds;
    private long processingTimeInMillis;
    private String humanReadableProcessingTime;

    /**
     * Creates a new instance of {@link BaseSyncStatistics} which stores the current time of instantiation in the
     * {@code startTime} instance variable that will be used later when {@link BaseSyncStatistics#calculateProcessingTime()}
     * is called to calculate the total time of processing.
     */
    public BaseSyncStatistics() {
        startTime = System.currentTimeMillis();
        reportMessage = StringUtil.EMPTY_STRING;
        humanReadableProcessingTime = StringUtil.EMPTY_STRING;
    }

    /**
     * Gets the total number of resources updated.
     *
     * @return total number of resources updated.
     */
    public int getUpdated() {
        return updated;
    }

    /**
     * Increments the total number of resource updated.
     */
    public void incrementUpdated() {
        this.updated++;
    }

    /**
     * Gets the total number of resources created.
     *
     * @return total number of resources created.
     */
    public int getCreated() {
        return created;
    }

    /**
     * Increments the total number of resource created.
     */
    public void incrementCreated() {
        this.created++;
    }

    /**
     * Gets the total number of resources processed/synced.
     *
     * @return total number of resources processed/synced.
     */
    public int getProcessed() {
        return processed;
    }

    /**
     * Increments the total number of resources processed/synced.
     */
    public void incrementProcessed() {
        this.processed++;
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
     * Calculates the processing time taken by the subtracting the time when this {@link BaseSyncStatistics} instance
     * was instantiated from the current time in Milliseconds. It also sets the processing time in all the units
     * {@code processingTimeInDays}, {@code processingTimeInHours}, {@code processingTimeInMinutes},
     * {@code processingTimeInSeconds} and {@code processingTimeInMillis}. It also builds a human readable processing
     * time, as string, in the following format @{code "0d, 0h, 0m, 2s, 545ms"} and stores it in the publicly exposed
     * variable {@code humanReadableProcessingTime}.
     */
    public void calculateProcessingTime() {
        setProcessingTimeInAllUnits();
        setHumanReadableProcessingTime();
    }

    /**
     * Calculates the processing time taken by the subtracting the time when this {@link BaseSyncStatistics} instance
     * was instantiated from the current time in Milliseconds. It sets the processing time in all the units
     * {@code processingTimeInDays}, {@code processingTimeInHours}, {@code processingTimeInMinutes},
     * {@code processingTimeInSeconds} and {@code processingTimeInMillis}.
     */
    private void setProcessingTimeInAllUnits() {
        processingTimeInMillis = System.currentTimeMillis() - this.startTime;
        processingTimeInDays = TimeUnit.MILLISECONDS.toDays(processingTimeInMillis);
        processingTimeInHours = TimeUnit.MILLISECONDS.toHours(processingTimeInMillis);
        processingTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(processingTimeInHours);
        processingTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(processingTimeInMillis);
    }

    /**
     * Builds a human readable processing time, as string, in the following format @{code "0d, 0h, 0m, 2s, 545ms"}
     * and stores it in the publicly exposed variable {@code humanReadableProcessingTime}.
     */
    private void setHumanReadableProcessingTime() {
        final long completeDaysInHours = TimeUnit.DAYS.toHours(processingTimeInDays);
        final long completeHoursInMinutes = TimeUnit.HOURS.toMinutes(processingTimeInHours);
        final long completeMinutesInSeconds = TimeUnit.MINUTES.toSeconds(processingTimeInMinutes);
        final long completeSecondsInMillis = TimeUnit.SECONDS.toMillis(processingTimeInSeconds);

        final long remainingHours = processingTimeInHours - completeDaysInHours;
        final long remainingMinutes = processingTimeInMinutes - completeHoursInMinutes;
        final long remainingSeconds = processingTimeInSeconds - completeMinutesInSeconds;
        final long remainingMillis = processingTimeInMillis - completeSecondsInMillis;

        humanReadableProcessingTime = format("%dd, %dh, %dm, %ds, %dms",
                processingTimeInDays,
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
    public String getHumanReadableProcessingTime() {
        return humanReadableProcessingTime;
    }

    /**
     * Gets the number of days it took to process.
     *
     * @return number of days taken to process.
     */
    public long getProcessingTimeInDays() {
        return processingTimeInDays;
    }

    /**
     * Gets the number of hours it took to process.
     *
     * @return number of hours taken to process.
     */
    public long getProcessingTimeInHours() {
        return processingTimeInHours;
    }

    /**
     * Gets the number of minutes it took to process.
     *
     * @return number of minutes taken to process.
     */
    public long getProcessingTimeInMinutes() {
        return processingTimeInMinutes;
    }

    /**
     * Gets the number of seconds it took to process.
     *
     * @return number of seconds taken to process.
     */
    public long getProcessingTimeInSeconds() {
        return processingTimeInSeconds;
    }

    /**
     * Gets the number of milliseconds it took to process.
     *
     * @return number of milliseconds taken to process.
     */
    public long getProcessingTimeInMillis() {
        return processingTimeInMillis;
    }

    /**
     * Gets a summary message of the statistics report.
     *
     * @return a summary message of the statistics report.
     */
    public abstract String getReportMessage();

    /**
     * Builds a JSON String that represents the fields of the supplied instance of {@link BaseSyncStatistics}.
     * Note: The order of the fields in the built JSON String depends on the order of the instance variables in this
     * class.
     *
     * @param statistics the instance of {@link BaseSyncStatistics} from which to create a JSON String.
     * @return
     */
    public static String getStatisticsAsJSONString(@Nonnull final BaseSyncStatistics statistics) {
        String result = null;
        final ObjectMapper mapper = new ObjectMapper();
        try {
            result = mapper.writeValueAsString(statistics);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to build JSON String of summary.", e);
        }
        return result;
    }
}
