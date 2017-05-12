package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategorySyncStatistics;
import io.netty.util.internal.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.commercetools.sync.commons.helpers.BaseSyncStatistics.getStatisticsAsJsonString;
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
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        baseSyncStatistics.incrementUpdated();
        assertThat(baseSyncStatistics.getUpdated()).isEqualTo(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        baseSyncStatistics.incrementCreated();
        assertThat(baseSyncStatistics.getCreated()).isEqualTo(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getProcessed()).isEqualTo(0);
    }

    @Test
    public void incrementProcessed_ShouldIncrementProcessedValue() {
        baseSyncStatistics.incrementProcessed();
        assertThat(baseSyncStatistics.getProcessed()).isEqualTo(1);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(baseSyncStatistics.getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        baseSyncStatistics.incrementFailed();
        assertThat(baseSyncStatistics.getFailed()).isEqualTo(1);
    }

    @Test
    public void calculateProcessingTime_ShouldSetProcessingTimeInAllUnitsAndHumanReadableString() throws
        InterruptedException {
        assertThat(baseSyncStatistics.getProcessingTimeInMillis()).isEqualTo(0);
        assertThat(baseSyncStatistics.getHumanReadableProcessingTime()).isEqualTo(StringUtil.EMPTY_STRING);

        final int waitingTimeInMillis = 100;
        Thread.sleep(waitingTimeInMillis);
        baseSyncStatistics.calculateProcessingTime();

        assertThat(baseSyncStatistics.getProcessingTimeInDays()).isGreaterThanOrEqualTo(0);
        assertThat(baseSyncStatistics.getProcessingTimeInHours()).isGreaterThanOrEqualTo(0);
        assertThat(baseSyncStatistics.getProcessingTimeInMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(baseSyncStatistics.getProcessingTimeInSeconds()).isGreaterThanOrEqualTo(waitingTimeInMillis / 1000);
        assertThat(baseSyncStatistics.getProcessingTimeInMillis()).isGreaterThanOrEqualTo(waitingTimeInMillis);

        final long remainingMillis = baseSyncStatistics.getProcessingTimeInMillis()
            - TimeUnit.SECONDS.toMillis(baseSyncStatistics.getProcessingTimeInSeconds());
        assertThat(baseSyncStatistics.getHumanReadableProcessingTime()).contains(format(", %dms", remainingMillis));
    }

    @Test
    public void getStatisticsAsJsonString_WithoutCalculatingProcessingTime_ShouldGetCorrectJsonString() {
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
                + "processingTimeInDays\":" + baseSyncStatistics.getProcessingTimeInDays() + ",\""
                + "processingTimeInHours\":" + baseSyncStatistics.getProcessingTimeInHours() + ",\""
                + "processingTimeInMinutes\":" + baseSyncStatistics.getProcessingTimeInMinutes() + ",\""
                + "processingTimeInSeconds\":" + baseSyncStatistics.getProcessingTimeInSeconds() + ",\""
                + "processingTimeInMillis\":" + baseSyncStatistics.getProcessingTimeInMillis() + ",\""
                + "humanReadableProcessingTime\":\"" + baseSyncStatistics.getHumanReadableProcessingTime() + "\"}");
    }

    @Test
    public void getStatisticsAsJsonString_WithCalculatingProcessingTime_ShouldGetCorrectJsonString() {
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
                + "processingTimeInDays\":" + baseSyncStatistics.getProcessingTimeInDays() + ",\""
                + "processingTimeInHours\":" + baseSyncStatistics.getProcessingTimeInHours() + ",\""
                + "processingTimeInMinutes\":" + baseSyncStatistics.getProcessingTimeInMinutes() + ",\""
                + "processingTimeInSeconds\":" + baseSyncStatistics.getProcessingTimeInSeconds() + ",\""
                + "processingTimeInMillis\":" + baseSyncStatistics.getProcessingTimeInMillis() + ",\""
                + "humanReadableProcessingTime\":\"" + baseSyncStatistics.getHumanReadableProcessingTime() + "\"}");
    }
}
