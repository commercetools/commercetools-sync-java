package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategorySyncStatisticsBuilder;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static com.commercetools.sync.commons.helpers.BaseSyncStatistics.getStatisticsAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseSyncStatisticsTest {
    private static final long ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS = 75 * 60 * 1000 + 20 * 1000L;
    private BaseSyncStatisticsBuilder baseSyncStatisticsBuilder;

    @Before
    public void setup() {
        baseSyncStatisticsBuilder = new CategorySyncStatisticsBuilder();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(baseSyncStatisticsBuilder.build().getUpdated()).isEqualTo(0);
    }

    @Test
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        baseSyncStatisticsBuilder.incrementUpdated();
        assertThat(baseSyncStatisticsBuilder.build().getUpdated()).isEqualTo(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(baseSyncStatisticsBuilder.build().getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        baseSyncStatisticsBuilder.incrementCreated();
        assertThat(baseSyncStatisticsBuilder.build().getCreated()).isEqualTo(1);
    }

    @Test
    public void getUpToDate_WithNoUpToDate_ShouldReturnZero() {
        assertThat(baseSyncStatisticsBuilder.build().getUpToDate()).isEqualTo(0);
    }

    @Test
    public void incrementUpToDate_ShouldIncrementUpToDateValue() {
        baseSyncStatisticsBuilder.incrementUpToDate();
        assertThat(baseSyncStatisticsBuilder.build().getUpToDate()).isEqualTo(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(baseSyncStatisticsBuilder.build().getProcessed()).isEqualTo(0);
    }

    @Test
    public void getProcessed_WithOtherStatsIncremented_ShouldReturnSumOfOtherValues() {
        baseSyncStatisticsBuilder.incrementCreated();
        baseSyncStatisticsBuilder.incrementUpdated();
        baseSyncStatisticsBuilder.incrementUpToDate();
        baseSyncStatisticsBuilder.incrementFailed();
        assertThat(baseSyncStatisticsBuilder.build().getProcessed()).isEqualTo(4);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(baseSyncStatisticsBuilder.build().getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        baseSyncStatisticsBuilder.incrementFailed();
        assertThat(baseSyncStatisticsBuilder.build().getFailed()).isEqualTo(1);
    }

    @Test
    public void getProcesingTimeInMillis_WithNoProcessingTime_ShouldReturnZero() {
        assertThat(baseSyncStatisticsBuilder.build().getProcessingTimeInMillis()).isEqualTo(0L);
    }

    @Test
    public void setProcesingTimeInMillis_ShouldSetProcessingTimeValue() {
        baseSyncStatisticsBuilder.setProcessingTimeInMillis(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);
        assertThat(baseSyncStatisticsBuilder.build().getProcessingTimeInMillis())
            .isEqualTo(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);
    }

    @Test
    public void getFormattedProcessingTime_ShouldReturnFormattedString() {
        final DateFormat dateFormat = new SimpleDateFormat("H'h, 'm'm, 's's, 'SSS'ms'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        baseSyncStatisticsBuilder.setProcessingTimeInMillis(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);
        assertThat(baseSyncStatisticsBuilder.build().getFormattedProcessingTime(dateFormat))
            .isEqualTo("1h, 15m, 20s, 000ms");
    }

    @Test
    public void getStatisticsAsJsonString_ShouldGetCorrectJsonString() {
        baseSyncStatisticsBuilder.incrementCreated();
        baseSyncStatisticsBuilder.incrementFailed();
        baseSyncStatisticsBuilder.incrementUpdated();
        baseSyncStatisticsBuilder.incrementUpToDate();
        baseSyncStatisticsBuilder.setProcessingTimeInMillis(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);

        final String statisticsAsJsonString = getStatisticsAsJsonString(baseSyncStatisticsBuilder.build());
        assertThat(statisticsAsJsonString)
            .isEqualTo("{\"updated\":1,\""
                + "created\":1,\""
                + "failed\":1,\""
                + "upToDate\":1,\""
                + "processed\":4,\""
                + "processingTimeInMillis\":" + ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS + ",\""
                + "reportMessage\":\"Summary: 4 categories were processed in total (1 created, 1 updated, 1 were "
                + "up to date and 1 failed to sync).\"}");
    }
}
