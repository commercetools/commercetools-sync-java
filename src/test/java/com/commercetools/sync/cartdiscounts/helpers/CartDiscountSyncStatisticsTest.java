package com.commercetools.sync.cartdiscounts.helpers;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class CartDiscountSyncStatisticsTest {
    private CartDiscountSyncStatistics cartDiscountSyncStatistics;

    @Before
    public void setup() {
        cartDiscountSyncStatistics = new CartDiscountSyncStatistics();
    }

    @Test
    public void getReportMessage_WithIncrementedStats_ShouldGetCorrectMessage() {
        cartDiscountSyncStatistics.incrementCreated(1);
        cartDiscountSyncStatistics.incrementFailed(2);
        cartDiscountSyncStatistics.incrementUpdated(3);
        cartDiscountSyncStatistics.incrementProcessed(6);

        assertThat(cartDiscountSyncStatistics.getReportMessage())
            .isEqualTo("Summary: 6 cart discounts were processed in total "
                + "(1 created, 3 updated and 2 failed to sync).");
    }
}
