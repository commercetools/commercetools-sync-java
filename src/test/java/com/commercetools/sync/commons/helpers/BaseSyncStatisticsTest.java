package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.util.internal.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.commercetools.sync.commons.MockUtils.getStatisticsAsJsonString;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseSyncStatisticsTest {
    private BaseSyncStatistics baseSyncStatistics;

    @Before
    public void setup() {
        baseSyncStatistics = new CategorySyncStatistics();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getUpdated()).isEqualTo(0);
    }

    @Test
    public void incrementUpdated_WithNoSpecifiedTimes_ShouldIncrementUpdatedValue() {
        baseSyncStatistics.incrementUpdated();
        assertThat(baseSyncStatistics.getUpdated()).isEqualTo(1);
    }

    @Test
    public void incrementUpdated_WithSpecifiedTimes_ShouldIncrementUpdatedValue() {
        baseSyncStatistics.incrementUpdated(5);
        assertThat(baseSyncStatistics.getUpdated()).isEqualTo(5);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_WithNoSpecifiedTimes_ShouldIncrementCreatedValue() {
        baseSyncStatistics.incrementCreated();
        assertThat(baseSyncStatistics.getCreated()).isEqualTo(1);
    }

    @Test
    public void incrementCreated_WithSpecifiedTimes_ShouldIncrementCreatedValue() {
        baseSyncStatistics.incrementCreated(2);
        assertThat(baseSyncStatistics.getCreated()).isEqualTo(2);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getProcessed()).isEqualTo(0);
    }

    @Test
    public void incrementProcessed_WithNoSpecifiedTimes_ShouldIncrementProcessedValue() {
        baseSyncStatistics.incrementProcessed();
        assertThat(baseSyncStatistics.getProcessed()).isEqualTo(1);
    }

    @Test
    public void incrementProcessed_WithSpecifiedTimes_ShouldIncrementProcessedValue() {
        baseSyncStatistics.incrementProcessed(2);
        assertThat(baseSyncStatistics.getProcessed()).isEqualTo(2);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_WithNoSpecifiedTimes_ShouldIncrementFailedValue() {
        baseSyncStatistics.incrementFailed();
        assertThat(baseSyncStatistics.getFailed()).isEqualTo(1);
    }

    @Test
    public void incrementFailed_WithSpecifiedTimes_ShouldIncrementFailedValue() {
        baseSyncStatistics.incrementFailed(3);
        assertThat(baseSyncStatistics.getFailed()).isEqualTo(3);
    }

    @Test
    public void calculateProcessingTime_ShouldSetProcessingTimeInAllUnitsAndHumanReadableString() throws
        InterruptedException {
        assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInMillis()).isEqualTo(0);
        assertThat(baseSyncStatistics.getLatestBatchHumanReadableProcessingTime()).isEqualTo(StringUtil.EMPTY_STRING);

        final int waitingTimeInMillis = 100;
        Thread.sleep(waitingTimeInMillis);
        baseSyncStatistics.calculateProcessingTime();

        assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInDays()).isGreaterThanOrEqualTo(0);
        assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInHours()).isGreaterThanOrEqualTo(0);
        assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInSeconds())
            .isGreaterThanOrEqualTo(waitingTimeInMillis / 1000);
        assertThat(baseSyncStatistics.getLatestBatchProcessingTimeInMillis())
            .isGreaterThanOrEqualTo(waitingTimeInMillis);

        final long remainingMillis = baseSyncStatistics.getLatestBatchProcessingTimeInMillis()
            - TimeUnit.SECONDS.toMillis(baseSyncStatistics.getLatestBatchProcessingTimeInSeconds());
        assertThat(baseSyncStatistics.getLatestBatchHumanReadableProcessingTime())
            .contains(format(", %dms", remainingMillis));
    }

    @Test
    public void getStatisticsAsJsonString_WithoutCalculatingProcessingTime_ShouldGetCorrectJsonString()
        throws JsonProcessingException {
        baseSyncStatistics.incrementCreated();
        baseSyncStatistics.incrementProcessed();

        baseSyncStatistics.incrementFailed();
        baseSyncStatistics.incrementProcessed();

        baseSyncStatistics.incrementUpdated();
        baseSyncStatistics.incrementProcessed();

        final String statisticsAsJsonString = getStatisticsAsJsonString(baseSyncStatistics);
        assertThat(statisticsAsJsonString)
            .isEqualTo("{\"reportMessage\":\"Summary: 3 categories were processed in total (1 created, 1 updated and"
                + " 1 categories failed to sync).\",\""
                + "updated\":1,\""
                + "created\":1,\""
                + "failed\":1,\""
                + "processed\":3,\""
                + "latestBatchProcessingTimeInDays\":" + baseSyncStatistics.getLatestBatchProcessingTimeInDays() + ",\""
                + "latestBatchProcessingTimeInHours\":" + baseSyncStatistics.getLatestBatchProcessingTimeInHours()
                + ",\""
                + "latestBatchProcessingTimeInMinutes\":" + baseSyncStatistics.getLatestBatchProcessingTimeInMinutes()
                + ",\""
                + "latestBatchProcessingTimeInSeconds\":" + baseSyncStatistics.getLatestBatchProcessingTimeInSeconds()
                + ",\""
                + "latestBatchProcessingTimeInMillis\":" + baseSyncStatistics.getLatestBatchProcessingTimeInMillis()
                + ",\""
                + "latestBatchHumanReadableProcessingTime\":\"" + baseSyncStatistics
                .getLatestBatchHumanReadableProcessingTime() + "\"}");
    }

    @Test
    public void getStatisticsAsJsonString_WithCalculatingProcessingTime_ShouldGetCorrectJsonString()
        throws JsonProcessingException {
        baseSyncStatistics.incrementCreated();
        baseSyncStatistics.incrementProcessed();

        baseSyncStatistics.incrementFailed();
        baseSyncStatistics.incrementProcessed();

        baseSyncStatistics.incrementUpdated();
        baseSyncStatistics.incrementProcessed();

        baseSyncStatistics.calculateProcessingTime();

        final String statisticsAsJsonString = getStatisticsAsJsonString(baseSyncStatistics);
        assertThat(statisticsAsJsonString)
            .isEqualTo("{\"reportMessage\":\"Summary: 3 categories were processed in total (1 created, 1 updated and"
                + " 1 categories failed to sync).\",\""
                + "updated\":1,\""
                + "created\":1,\""
                + "failed\":1,\""
                + "processed\":3,\""
                + "latestBatchProcessingTimeInDays\":" + baseSyncStatistics.getLatestBatchProcessingTimeInDays() + ",\""
                + "latestBatchProcessingTimeInHours\":" + baseSyncStatistics.getLatestBatchProcessingTimeInHours()
                + ",\""
                + "latestBatchProcessingTimeInMinutes\":" + baseSyncStatistics.getLatestBatchProcessingTimeInMinutes()
                + ",\""
                + "latestBatchProcessingTimeInSeconds\":" + baseSyncStatistics.getLatestBatchProcessingTimeInSeconds()
                + ",\""
                + "latestBatchProcessingTimeInMillis\":" + baseSyncStatistics.getLatestBatchProcessingTimeInMillis()
                + ",\""
                + "latestBatchHumanReadableProcessingTime\":\"" + baseSyncStatistics
                .getLatestBatchHumanReadableProcessingTime() + "\"}");
    }
}
