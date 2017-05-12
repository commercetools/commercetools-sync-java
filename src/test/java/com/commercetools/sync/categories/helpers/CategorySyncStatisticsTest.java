package com.commercetools.sync.categories.helpers;


import io.netty.util.internal.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.commercetools.sync.commons.helpers.BaseSyncStatistics.getStatisticsAsJsonString;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CategorySyncStatisticsTest {

    private CategorySyncStatistics categorySyncStatistics;

    @Before
    public void setup() {
        categorySyncStatistics = new CategorySyncStatistics();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getUpdated()).isEqualTo(0);
    }

    @Test
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        categorySyncStatistics.incrementUpdated();
        assertThat(categorySyncStatistics.getUpdated()).isEqualTo(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        categorySyncStatistics.incrementCreated();
        assertThat(categorySyncStatistics.getCreated()).isEqualTo(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getProcessed()).isEqualTo(0);
    }

    @Test
    public void incrementProcessed_ShouldIncrementProcessedValue() {
        categorySyncStatistics.incrementProcessed();
        assertThat(categorySyncStatistics.getProcessed()).isEqualTo(1);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(categorySyncStatistics.getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        categorySyncStatistics.incrementFailed();
        assertThat(categorySyncStatistics.getFailed()).isEqualTo(1);
    }

    @Test
    public void calculateProcessingTime_ShouldSetProcessingTimeInAllUnitsAndHumanReadableString() throws
        InterruptedException {
        assertThat(categorySyncStatistics.getProcessingTimeInMillis()).isEqualTo(0);
        assertThat(categorySyncStatistics.getHumanReadableProcessingTime()).isEqualTo(StringUtil.EMPTY_STRING);

        final int waitingTimeInMillis = 100;
        Thread.sleep(waitingTimeInMillis);
        categorySyncStatistics.calculateProcessingTime();

        assertThat(categorySyncStatistics.getProcessingTimeInDays()).isGreaterThanOrEqualTo(0);
        assertThat(categorySyncStatistics.getProcessingTimeInHours()).isGreaterThanOrEqualTo(0);
        assertThat(categorySyncStatistics.getProcessingTimeInMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(categorySyncStatistics.getProcessingTimeInSeconds()).isGreaterThanOrEqualTo(waitingTimeInMillis
            / 1000);
        assertThat(categorySyncStatistics.getProcessingTimeInMillis()).isGreaterThanOrEqualTo(waitingTimeInMillis);

        final long remainingMillis = categorySyncStatistics.getProcessingTimeInMillis()
            - TimeUnit.SECONDS.toMillis(categorySyncStatistics.getProcessingTimeInSeconds());
        assertThat(categorySyncStatistics.getHumanReadableProcessingTime()).contains(format(", %dms", remainingMillis));
    }

    @Test
    public void getStatisticsAsJsonString_WithoutCalculatingProcessingTime_ShouldGetCorrectJsonString() {
        categorySyncStatistics.incrementCreated();
        categorySyncStatistics.incrementProcessed();

        categorySyncStatistics.incrementFailed();
        categorySyncStatistics.incrementProcessed();

        categorySyncStatistics.incrementUpdated();
        categorySyncStatistics.incrementProcessed();

        final String statisticsAsJsonString = getStatisticsAsJsonString(categorySyncStatistics);
        assertThat(statisticsAsJsonString)
                .isEqualTo("{\"reportMessage\":\"Summary: 3 categories were processed in total (1 created, 1 updated "
                  + "and 1 categories failed to sync).\",\""
                  + "updated\":1,\""
                  + "created\":1,\""
                  + "failed\":1,\""
                  + "processed\":3,\""
                  + "processingTimeInDays\":" + categorySyncStatistics.getProcessingTimeInDays() + ",\""
                  + "processingTimeInHours\":" + categorySyncStatistics.getProcessingTimeInHours() + ",\""
                  + "processingTimeInMinutes\":" + categorySyncStatistics.getProcessingTimeInMinutes() + ",\""
                  + "processingTimeInSeconds\":" + categorySyncStatistics.getProcessingTimeInSeconds() + ",\""
                  + "processingTimeInMillis\":" + categorySyncStatistics.getProcessingTimeInMillis() + ",\""
                  + "humanReadableProcessingTime\":\"" + categorySyncStatistics.getHumanReadableProcessingTime()
                  + "\"}");
    }

    @Test
    public void getStatisticsAsJsonString_WithCalculatingProcessingTime_ShouldGetCorrectJsonString() {
        categorySyncStatistics.incrementCreated();
        categorySyncStatistics.incrementProcessed();

        categorySyncStatistics.incrementFailed();
        categorySyncStatistics.incrementProcessed();

        categorySyncStatistics.incrementUpdated();
        categorySyncStatistics.incrementProcessed();

        categorySyncStatistics.calculateProcessingTime();

        final String statisticsAsJsonString = getStatisticsAsJsonString(categorySyncStatistics);
        assertThat(statisticsAsJsonString)
            .isEqualTo("{\"reportMessage\":\"Summary: 3 categories were processed in total (1 created, 1 updated "
                + "and 1 categories failed to sync).\",\""
                + "updated\":1,\""
                + "created\":1,\""
                + "failed\":1,\""
                + "processed\":3,\""
                + "processingTimeInDays\":" + categorySyncStatistics.getProcessingTimeInDays() + ",\""
                + "processingTimeInHours\":" + categorySyncStatistics.getProcessingTimeInHours() + ",\""
                + "processingTimeInMinutes\":" + categorySyncStatistics.getProcessingTimeInMinutes() + ",\""
                + "processingTimeInSeconds\":" + categorySyncStatistics.getProcessingTimeInSeconds() + ",\""
                + "processingTimeInMillis\":" + categorySyncStatistics.getProcessingTimeInMillis() + ",\""
                + "humanReadableProcessingTime\":\"" + categorySyncStatistics.getHumanReadableProcessingTime() + "\"}");
    }
}
