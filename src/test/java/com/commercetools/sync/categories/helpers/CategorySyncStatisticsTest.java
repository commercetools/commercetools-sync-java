package com.commercetools.sync.categories.helpers;


import io.netty.util.internal.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.commercetools.sync.commons.helpers.BaseSyncStatistics.getStatisticsAsJSONString;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CategorySyncStatisticsTest {

    private static CategorySyncStatistics CATEGORY_SYNC_STATISTICS;

    @Before
    public void setup() {
        CATEGORY_SYNC_STATISTICS = new CategorySyncStatistics();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(CATEGORY_SYNC_STATISTICS.getUpdated()).isEqualTo(0);
    }

    @Test
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        CATEGORY_SYNC_STATISTICS.incrementUpdated();
        assertThat(CATEGORY_SYNC_STATISTICS.getUpdated()).isEqualTo(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(CATEGORY_SYNC_STATISTICS.getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        CATEGORY_SYNC_STATISTICS.incrementCreated();
        assertThat(CATEGORY_SYNC_STATISTICS.getCreated()).isEqualTo(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessed()).isEqualTo(0);
    }

    @Test
    public void incrementProcessed_ShouldIncrementProcessedValue() {
        CATEGORY_SYNC_STATISTICS.incrementProcessed();
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessed()).isEqualTo(1);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(CATEGORY_SYNC_STATISTICS.getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        CATEGORY_SYNC_STATISTICS.incrementFailed();
        assertThat(CATEGORY_SYNC_STATISTICS.getFailed()).isEqualTo(1);
    }

    @Test
    public void calculateProcessingTime_ShouldSetProcessingTimeInAllUnitsAndHumanReadableString() throws InterruptedException {
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessingTimeInMillis()).isEqualTo(0);
        assertThat(CATEGORY_SYNC_STATISTICS.getHumanReadableProcessingTime()).isEqualTo(StringUtil.EMPTY_STRING);

        final int waitingTimeInMillis = 100;
        Thread.sleep(waitingTimeInMillis);
        CATEGORY_SYNC_STATISTICS.calculateProcessingTime();

        assertThat(CATEGORY_SYNC_STATISTICS.getProcessingTimeInDays()).isGreaterThanOrEqualTo(0);
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessingTimeInHours()).isGreaterThanOrEqualTo(0);
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessingTimeInMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessingTimeInSeconds()).isGreaterThanOrEqualTo(waitingTimeInMillis / 1000);
        assertThat(CATEGORY_SYNC_STATISTICS.getProcessingTimeInMillis()).isGreaterThanOrEqualTo(waitingTimeInMillis);

        final long remainingMillis = CATEGORY_SYNC_STATISTICS.getProcessingTimeInMillis() -
                TimeUnit.SECONDS.toMillis(CATEGORY_SYNC_STATISTICS.getProcessingTimeInSeconds());
        assertThat(CATEGORY_SYNC_STATISTICS.getHumanReadableProcessingTime()).contains(format(", %dms", remainingMillis));
    }

    @Test
    public void getStatisticsAsJSONString_WithoutCalculatingProcessingTime_ShouldGetCorrectJSONString() {
        CATEGORY_SYNC_STATISTICS.incrementCreated();
        CATEGORY_SYNC_STATISTICS.incrementProcessed();

        CATEGORY_SYNC_STATISTICS.incrementFailed();
        CATEGORY_SYNC_STATISTICS.incrementProcessed();

        CATEGORY_SYNC_STATISTICS.incrementUpdated();
        CATEGORY_SYNC_STATISTICS.incrementProcessed();

        final String statisticsAsJsonString = getStatisticsAsJSONString(CATEGORY_SYNC_STATISTICS);
        assertThat(statisticsAsJsonString)
                .isEqualTo("{\"reportMessage\":\"Summary: 3 categories were processed in total (1 created, 1 updated and 1 categories failed to sync).\",\"" +
                        "updated\":1,\"" +
                        "created\":1,\"" +
                        "failed\":1,\"" +
                        "processed\":3,\"" +
                        "processingTimeInDays\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInDays() + ",\"" +
                        "processingTimeInHours\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInHours() + ",\"" +
                        "processingTimeInMinutes\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInMinutes() + ",\"" +
                        "processingTimeInSeconds\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInSeconds() + ",\"" +
                        "processingTimeInMillis\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInMillis() + ",\"" +
                        "humanReadableProcessingTime\":\"" + CATEGORY_SYNC_STATISTICS.getHumanReadableProcessingTime() + "\"}");
    }

    @Test
    public void getStatisticsAsJSONString_WithCalculatingProcessingTime_ShouldGetCorrectJSONString() {
        CATEGORY_SYNC_STATISTICS.incrementCreated();
        CATEGORY_SYNC_STATISTICS.incrementProcessed();

        CATEGORY_SYNC_STATISTICS.incrementFailed();
        CATEGORY_SYNC_STATISTICS.incrementProcessed();

        CATEGORY_SYNC_STATISTICS.incrementUpdated();
        CATEGORY_SYNC_STATISTICS.incrementProcessed();

        CATEGORY_SYNC_STATISTICS.calculateProcessingTime();

        final String statisticsAsJsonString = getStatisticsAsJSONString(CATEGORY_SYNC_STATISTICS);
        assertThat(statisticsAsJsonString)
                .isEqualTo("{\"reportMessage\":\"Summary: 3 categories were processed in total (1 created, 1 updated and 1 categories failed to sync).\",\"" +
                        "updated\":1,\"" +
                        "created\":1,\"" +
                        "failed\":1,\"" +
                        "processed\":3,\"" +
                        "processingTimeInDays\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInDays() + ",\"" +
                        "processingTimeInHours\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInHours() + ",\"" +
                        "processingTimeInMinutes\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInMinutes() + ",\"" +
                        "processingTimeInSeconds\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInSeconds() + ",\"" +
                        "processingTimeInMillis\":" + CATEGORY_SYNC_STATISTICS.getProcessingTimeInMillis() + ",\"" +
                        "humanReadableProcessingTime\":\"" + CATEGORY_SYNC_STATISTICS.getHumanReadableProcessingTime() + "\"}");
    }
}
