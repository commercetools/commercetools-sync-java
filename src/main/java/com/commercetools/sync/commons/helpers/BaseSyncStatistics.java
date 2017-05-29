package com.commercetools.sync.commons.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class BaseSyncStatistics {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSyncStatistics.class);

    protected static final String REPORT_MESSAGE_TEMPLATE = "Summary: %d %s were processed in total "
        + "(%d created, %d updated, %d up to date and %d failed to sync).";

    private int updated;
    private int created;
    private int failed;
    private int upToDate;
    private int processed;
    private long processingTimeInMillis;

    protected BaseSyncStatistics(final long processingTimeInMillis, final int created, final int updated,
                                 final int upToDate, final int failed) {
        this.processingTimeInMillis = processingTimeInMillis;
        this.processed = created + updated + upToDate + failed;
        this.created = created;
        this.updated = updated;
        this.upToDate = upToDate;
        this.failed = failed;
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
     * Gets the total number of resources that were already up to date.
     *
     * @return total number of resources that were already up to date.
     */
    public int getUpToDate() {
        return upToDate;
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
     * Returns processing time formatted by {@code dataFormat}.
     *
     * @param dateFormat date formatter
     * @return formatted time taken to process
     */
    public String getFormattedProcessingTime(@Nonnull final DateFormat dateFormat) {
        return dateFormat.format(new Date(processingTimeInMillis));
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
     * @return a JSON representation of the given {@code statistics} as a String.
     */
    public static String getStatisticsAsJsonString(@Nonnull final BaseSyncStatistics statistics) {
        String result = null;
        final ObjectMapper mapper = new ObjectMapper();
        try {
            result = mapper.writeValueAsString(statistics);
        } catch (JsonProcessingException processingException) {
            LOGGER.error("Failed to build JSON String of summary.", processingException);
        }
        return result;
    }
}
