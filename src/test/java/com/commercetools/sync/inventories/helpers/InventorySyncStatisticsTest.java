package com.commercetools.sync.inventories.helpers;


import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InventorySyncStatisticsTest {
    private static final long ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS = 75 * 60 * 1000 + 20 * 1000L;

    private InventorySyncStatisticsBuilder inventorySyncStatisticsBuilder;

    @Before
    public void setup() {
        inventorySyncStatisticsBuilder = new InventorySyncStatisticsBuilder();
    }

    @Test
    public void getUpdated_WithNoUpdated_ShouldReturnZero() {
        assertThat(inventorySyncStatisticsBuilder.build().getUpdated()).isEqualTo(0);
    }

    @Test
    public void incrementUpdated_ShouldIncrementUpdatedValue() {
        inventorySyncStatisticsBuilder.incrementUpdated();
        assertThat(inventorySyncStatisticsBuilder.build().getUpdated()).isEqualTo(1);
    }

    @Test
    public void getCreated_WithNoCreated_ShouldReturnZero() {
        assertThat(inventorySyncStatisticsBuilder.build().getCreated()).isEqualTo(0);
    }

    @Test
    public void incrementCreated_ShouldIncrementCreatedValue() {
        inventorySyncStatisticsBuilder.incrementCreated();
        assertThat(inventorySyncStatisticsBuilder.build().getCreated()).isEqualTo(1);
    }

    @Test
    public void getUpToDate_WithNoUpToDate_ShouldReturnZero() {
        assertThat(inventorySyncStatisticsBuilder.build().getUpToDate()).isEqualTo(0);
    }

    @Test
    public void incrementUpToDate_ShouldIncrementUpToDateValue() {
        inventorySyncStatisticsBuilder.incrementUpToDate();
        assertThat(inventorySyncStatisticsBuilder.build().getUpToDate()).isEqualTo(1);
    }

    @Test
    public void getProcessed_WithNoProcessed_ShouldReturnZero() {
        assertThat(inventorySyncStatisticsBuilder.build().getProcessed()).isEqualTo(0);
    }

    @Test
    public void getProcessed_WithOtherStatsIncremented_ShouldReturnSumOfOtherValues() {
        inventorySyncStatisticsBuilder.incrementCreated();
        inventorySyncStatisticsBuilder.incrementUpdated();
        inventorySyncStatisticsBuilder.incrementUpToDate();
        inventorySyncStatisticsBuilder.incrementFailed();
        assertThat(inventorySyncStatisticsBuilder.build().getProcessed()).isEqualTo(4);
    }

    @Test
    public void getFailed_WithNoFailed_ShouldReturnZero() {
        assertThat(inventorySyncStatisticsBuilder.build().getFailed()).isEqualTo(0);
    }

    @Test
    public void incrementFailed_ShouldIncrementFailedValue() {
        inventorySyncStatisticsBuilder.incrementFailed();
        assertThat(inventorySyncStatisticsBuilder.build().getFailed()).isEqualTo(1);
    }

    @Test
    public void getProcesingTimeInMillis_WithNoProcessingTime_ShouldReturnZero() {
        assertThat(inventorySyncStatisticsBuilder.build().getProcessingTimeInMillis()).isEqualTo(0L);
    }

    @Test
    public void setProcesingTimeInMillis_ShouldSetProcessingTimeValue() {
        inventorySyncStatisticsBuilder.setProcessingTimeInMillis(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);
        assertThat(inventorySyncStatisticsBuilder.build().getProcessingTimeInMillis())
            .isEqualTo(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);
    }

    @Test
    public void getFormattedProcessingTime_ShouldReturnFormattedString() {
        inventorySyncStatisticsBuilder.setProcessingTimeInMillis(ONE_HOUR_FIFTEEN_MINUTES_AND_TWENTY_SECONDS_IN_MILLIS);
        assertThat(inventorySyncStatisticsBuilder.build().getFormattedProcessingTime("d'd, 'H'h, 'm'm, 's's, 'S'ms'"))
            .isEqualTo("0d, 1h, 15m, 20s, 000ms");
    }
}
