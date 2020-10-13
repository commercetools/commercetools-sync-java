package com.commercetools.sync.shoppinglists.helpers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListSyncStatisticsTest {
    private ShoppingListSyncStatistics shoppingListSyncStatistics;

    @BeforeEach
    void setup() {
        shoppingListSyncStatistics = new ShoppingListSyncStatistics();
    }

    @Test
    void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        shoppingListSyncStatistics.incrementCreated(1);
        shoppingListSyncStatistics.incrementFailed(2);
        shoppingListSyncStatistics.incrementUpdated(3);
        shoppingListSyncStatistics.incrementProcessed(6);

        assertThat(shoppingListSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 6 shopping lists were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
    }
}
