package com.commercetools.sync.commons.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Statistics of synchronisation process. Class is immutable.
 */
public class BaseSyncStatistics {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSyncStatistics.class);

    private int updated;
    private int created;
    private int failed;
    private int processed;
    private long processingTimeInMillis;

    protected BaseSyncStatistics(@Nonnull BaseSyncStatistics other) {
        this.processed = other.getProcessed();
        this.created = other.getCreated();
        this.updated = other.getUpdated();
        this.failed = other.getFailed();
        this.processingTimeInMillis = other.getProcessingTimeInMillis();
    }

    protected BaseSyncStatistics(int processed, int created, int updated, int failed, long processingTimeInMillis) {
        this.processed = processed;
        this.created = created;
        this.updated = updated;
        this.failed = failed;
        this.processingTimeInMillis = processingTimeInMillis;
    }

    /**
     * Returns new {@link BaseSyncStatistics} instance that contains statistics values summed up from
     * {@code statistics1} and {@code statistics2}
     *
     * @param statistics1 first element
     * @param statistics2 second element
     * @return new {@link BaseSyncStatistics} instance that contains summed up statistics
     */
    public static BaseSyncStatistics merge(@Nonnull final BaseSyncStatistics statistics1,
                                           @Nonnull final BaseSyncStatistics statistics2) {
        return new BaseSyncStatistics(statistics1.getProcessed() + statistics2.getProcessed(),
                statistics1.getCreated() + statistics2.getCreated(),
                statistics1.getUpdated() + statistics2.getUpdated(),
                statistics1.getFailed() + statistics2.getFailed(),
                statistics1.getProcessingTimeInMillis() + statistics2.getProcessingTimeInMillis());
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
     * Gets the total number of resources created.
     *
     * @return total number of resources created.
     */
    public int getCreated() {
        return created;
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
     * Gets the total number of resources that failed to sync.
     *
     * @return total number of resources that failed to sync.
     */
    public int getFailed() {
        return failed;
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
     * Gets the human readable processing time in the following format @{code "0d, 0h, 0m, 2s, 545ms"}.
     *
     * @return the human readable processing time in the following format @{code "0d, 0h, 0m, 2s, 545ms"}
     */
    public String getHumanReadableProcessingTime() {

        final long processingTimeInDays = TimeUnit.MILLISECONDS.toDays(processingTimeInMillis);
        final long processingTimeInHours = TimeUnit.MILLISECONDS.toHours(processingTimeInMillis);
        final long processingTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(processingTimeInMillis);
        final long processingTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(processingTimeInMillis);

        final long completeDaysInHours = TimeUnit.DAYS.toHours(processingTimeInDays);
        final long completeHoursInMinutes = TimeUnit.HOURS.toMinutes(processingTimeInHours);
        final long completeMinutesInSeconds = TimeUnit.MINUTES.toSeconds(processingTimeInMinutes);
        final long completeSecondsInMillis = TimeUnit.SECONDS.toMillis(processingTimeInSeconds);

        final long remainingHours = processingTimeInHours - completeDaysInHours;
        final long remainingMinutes = processingTimeInMinutes - completeHoursInMinutes;
        final long remainingSeconds = processingTimeInSeconds - completeMinutesInSeconds;
        final long remainingMillis = processingTimeInMillis - completeSecondsInMillis;

        return format("%dd, %dh, %dm, %ds, %dms",
                processingTimeInDays,
                remainingHours,
                remainingMinutes,
                remainingSeconds,
                remainingMillis
        );
    }

    /**
     * Builds a summary of the sync statistics instance that looks like the following example:
     * <p>
     * "Summary: 2 resources were processed in total (0 created, 0 updated and 0 resources failed to sync)."
     * </p>
     *
     * @return a summary message of the sync statistics instance.
     */
    public String getReportMessage(){
        return format("Summary: %s resources were processed in total " +
                        "(%s created, %s updated and %s resources failed to sync).",
                this.processed, this.created, this.updated, this.failed);
    }

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
