package com.commercetools.sync.commons.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class BaseSyncStatistics {
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

    public BaseStatistics() {
        this.startTime = System.currentTimeMillis();
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public void incrementUpdated() {
        this.updated++;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public void incrementCreated() {
        this.created++;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public void incrementProcessed() {
        this.processed++;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public void incrementFailed() {
        this.failed++;
    }

    public void calculateProcessingTime() {
        setProcessingTimeInAllUnits();
        setHumanReadableProcessingTime();
    }

    private void setProcessingTimeInAllUnits() {
        processingTimeInMillis = System.currentTimeMillis() - this.startTime;
        processingTimeInDays = TimeUnit.MILLISECONDS.toDays(processingTimeInMillis);
        processingTimeInHours = TimeUnit.MILLISECONDS.toHours(processingTimeInMillis);
        processingTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(processingTimeInHours);
        processingTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(processingTimeInMillis);
    }

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

    public String getHumanReadableProcessingTime() {
        return humanReadableProcessingTime;
    }

    public long getProcessingTimeInDays() {
        return processingTimeInDays;
    }

    public long getProcessingTimeInHours() {
        return processingTimeInHours;
    }

    public long getProcessingTimeInMinutes() {
        return processingTimeInMinutes;
    }

    public long getProcessingTimeInSeconds() {
        return processingTimeInSeconds;
    }

    public long getProcessingTimeInMillis() {
        return processingTimeInMillis;
    }

    public String getReportMessage() {
        return reportMessage;
    }

    public static String getStatisticsAsJSONString(@Nonnull final BaseStatistics statistics) {
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
