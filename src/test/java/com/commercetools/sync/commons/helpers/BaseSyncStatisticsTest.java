package com.commercetools.sync.commons.helpers;

import io.netty.util.internal.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.commercetools.sync.commons.helpers.BaseSyncStatistics.getStatisticsAsJSONString;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseSyncStatisticsTest {
    private static BaseSyncStatistics BASE_SYNC_STATISTICS;

    @Before
    public void setup() {
        BASE_SYNC_STATISTICS = new BaseSyncStatistics();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(BASE_SYNC_STATISTICS.getUpdated()).isEqualTo(0);
    }

    @Test
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        BASE_SYNC_STATISTICS.incrementUpdated();
        assertThat(BASE_SYNC_STATISTICS.getUpdated()).isEqualTo(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(BASE_SYNC_STATISTICS.getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        BASE_SYNC_STATISTICS.incrementCreated();
        assertThat(BASE_SYNC_STATISTICS.getCreated()).isEqualTo(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(BASE_SYNC_STATISTICS.getProcessed()).isEqualTo(0);
    }

    @Test
    public void incrementProcessed_ShouldIncrementProcessedValue() {
        BASE_SYNC_STATISTICS.incrementProcessed();
        assertThat(BASE_SYNC_STATISTICS.getProcessed()).isEqualTo(1);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(BASE_SYNC_STATISTICS.getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        BASE_SYNC_STATISTICS.incrementFailed();
        assertThat(BASE_SYNC_STATISTICS.getFailed()).isEqualTo(1);
    }

    @Test
    public void calculateProcessingTime_ShouldSetProcessingTimeInAllUnitsAndHumanReadableString() throws InterruptedException {
        assertThat(BASE_SYNC_STATISTICS.getProcessingTimeInMillis()).isEqualTo(0);
        assertThat(BASE_SYNC_STATISTICS.getHumanReadableProcessingTime()).isEqualTo(StringUtil.EMPTY_STRING);

        final int waitingTimeInMillis = 100;
        Thread.sleep(waitingTimeInMillis);
        BASE_SYNC_STATISTICS.calculateProcessingTime();

        assertThat(BASE_SYNC_STATISTICS.getProcessingTimeInDays()).isGreaterThanOrEqualTo(0);
        assertThat(BASE_SYNC_STATISTICS.getProcessingTimeInHours()).isGreaterThanOrEqualTo(0);
        assertThat(BASE_SYNC_STATISTICS.getProcessingTimeInMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(BASE_SYNC_STATISTICS.getProcessingTimeInSeconds()).isGreaterThanOrEqualTo(waitingTimeInMillis / 1000);
        assertThat(BASE_SYNC_STATISTICS.getProcessingTimeInMillis()).isGreaterThanOrEqualTo(waitingTimeInMillis);

        final long remainingMillis = BASE_SYNC_STATISTICS.getProcessingTimeInMillis() -
                TimeUnit.SECONDS.toMillis(BASE_SYNC_STATISTICS.getProcessingTimeInSeconds());
        assertThat(BASE_SYNC_STATISTICS.getHumanReadableProcessingTime()).contains(format(", %dms", remainingMillis));
    }

    @Test
    public void getStatisticsAsJSONString_WithoutCalculatingProcessingTime_ShouldGetCorrectJSONString() {
        BASE_SYNC_STATISTICS.incrementCreated();
        BASE_SYNC_STATISTICS.incrementProcessed();

        BASE_SYNC_STATISTICS.incrementFailed();
        BASE_SYNC_STATISTICS.incrementProcessed();

        BASE_SYNC_STATISTICS.incrementUpdated();
        BASE_SYNC_STATISTICS.incrementProcessed();

        final String statisticsAsJsonString = getStatisticsAsJSONString(BASE_SYNC_STATISTICS);
        assertThat(statisticsAsJsonString)
                .isEqualTo("{\"reportMessage\":\"\",\"" +
                        "updated\":1,\"" +
                        "created\":1,\"" +
                        "failed\":1,\"" +
                        "processed\":3,\"" +
                        "processingTimeInDays\":" + BASE_SYNC_STATISTICS.getProcessingTimeInDays() + ",\"" +
                        "processingTimeInHours\":" + BASE_SYNC_STATISTICS.getProcessingTimeInHours() + ",\"" +
                        "processingTimeInMinutes\":" + BASE_SYNC_STATISTICS.getProcessingTimeInMinutes() + ",\"" +
                        "processingTimeInSeconds\":" + BASE_SYNC_STATISTICS.getProcessingTimeInSeconds() + ",\"" +
                        "processingTimeInMillis\":" + BASE_SYNC_STATISTICS.getProcessingTimeInMillis() + ",\"" +
                        "humanReadableProcessingTime\":\"" + BASE_SYNC_STATISTICS.getHumanReadableProcessingTime() + "\"}");
    }

    @Test
    public void getStatisticsAsJSONString_WithCalculatingProcessingTime_ShouldGetCorrectJSONString() {
        BASE_SYNC_STATISTICS.incrementCreated();
        BASE_SYNC_STATISTICS.incrementProcessed();

        BASE_SYNC_STATISTICS.incrementFailed();
        BASE_SYNC_STATISTICS.incrementProcessed();

        BASE_SYNC_STATISTICS.incrementUpdated();
        BASE_SYNC_STATISTICS.incrementProcessed();

        BASE_SYNC_STATISTICS.calculateProcessingTime();

        final String statisticsAsJsonString = getStatisticsAsJSONString(BASE_SYNC_STATISTICS);
        assertThat(statisticsAsJsonString)
                .isEqualTo("{\"reportMessage\":\"\",\"" +
                        "updated\":1,\"" +
                        "created\":1,\"" +
                        "failed\":1,\"" +
                        "processed\":3,\"" +
                        "processingTimeInDays\":" + BASE_SYNC_STATISTICS.getProcessingTimeInDays() + ",\"" +
                        "processingTimeInHours\":" + BASE_SYNC_STATISTICS.getProcessingTimeInHours() + ",\"" +
                        "processingTimeInMinutes\":" + BASE_SYNC_STATISTICS.getProcessingTimeInMinutes() + ",\"" +
                        "processingTimeInSeconds\":" + BASE_SYNC_STATISTICS.getProcessingTimeInSeconds() + ",\"" +
                        "processingTimeInMillis\":" + BASE_SYNC_STATISTICS.getProcessingTimeInMillis() + ",\"" +
                        "humanReadableProcessingTime\":\"" + BASE_SYNC_STATISTICS.getHumanReadableProcessingTime() + "\"}");
    }


}
