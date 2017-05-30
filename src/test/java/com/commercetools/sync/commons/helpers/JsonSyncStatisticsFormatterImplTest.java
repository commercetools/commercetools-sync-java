package com.commercetools.sync.commons.helpers;

import com.commercetools.sync.categories.helpers.CategorySyncStatisticsBuilder;
import com.commercetools.sync.inventories.helpers.InventorySyncStatisticsBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonSyncStatisticsFormatterImplTest {

    private static final long PROCESSING_TIME_IN_MILLIS = 10000L;
    private static SyncStatisticsFormatter jsonFormatter;

    @BeforeClass
    public static void setup() {
        jsonFormatter = new JsonSyncStatisticsFormatterImpl();
    }


    @Test
    public void format_withInventoryStatistics_ShouldReturnsCorrectJsonString() {
        final InventorySyncStatisticsBuilder inventorySyncStatisticsBuilder = new InventorySyncStatisticsBuilder();
        inventorySyncStatisticsBuilder.incrementCreated();
        inventorySyncStatisticsBuilder.incrementFailed();
        inventorySyncStatisticsBuilder.incrementUpdated();
        inventorySyncStatisticsBuilder.incrementUpToDate();
        inventorySyncStatisticsBuilder.setProcessingTimeInMillis(PROCESSING_TIME_IN_MILLIS);

        final String statisticsAsJsonString = jsonFormatter.format(inventorySyncStatisticsBuilder.build());
        assertThat(statisticsAsJsonString)
            .isEqualTo("{\"updated\":1,\""
                + "created\":1,\""
                + "failed\":1,\""
                + "upToDate\":1,\""
                + "processed\":4,\""
                + "processingTimeInMillis\":" + PROCESSING_TIME_IN_MILLIS + "}");
    }

    @Test
    public void format_withCategoryStatistics_ShouldReturnsCorrectJsonString() {
        final CategorySyncStatisticsBuilder categorySyncStatisticsBuilder = new CategorySyncStatisticsBuilder();
        categorySyncStatisticsBuilder.incrementCreated();
        categorySyncStatisticsBuilder.incrementFailed();
        categorySyncStatisticsBuilder.incrementUpdated();
        categorySyncStatisticsBuilder.incrementUpToDate();
        categorySyncStatisticsBuilder.setProcessingTimeInMillis(PROCESSING_TIME_IN_MILLIS);

        final String statisticsAsJsonString = jsonFormatter.format(categorySyncStatisticsBuilder.build());
        assertThat(statisticsAsJsonString)
            .isEqualTo("{\"updated\":1,\""
                + "created\":1,\""
                + "failed\":1,\""
                + "upToDate\":1,\""
                + "processed\":4,\""
                + "processingTimeInMillis\":" + PROCESSING_TIME_IN_MILLIS + "}");
    }
}
