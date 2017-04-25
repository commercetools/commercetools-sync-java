package com.commercetools.sync.inventory.helpers;

import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientConfigBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InventorySyncOptionsTest {

    private static final String CTP_KEY = "testKey";
    private static final String CTP_ID = "testId";
    private static final String CTP_SECRET = "testSecret";

    @Test
    public void inventorySyncOptionsBuilder_returnsProperOptionsInstance_havingOnlyRequiredFieldsSet() {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(CTP_KEY, CTP_ID, CTP_SECRET).build();
        assertThat(options).isNotNull();
        assertThat(options.getClientConfig().getClientId()).isEqualTo(CTP_ID);
        assertThat(options.getClientConfig().getProjectKey()).isEqualTo(CTP_KEY);
        assertThat(options.getClientConfig().getClientSecret()).isEqualTo(CTP_SECRET);
        assertThat(options.getBatchSize()).isEqualTo(30);
        assertThat(options.isEnsureChannels()).isFalse();
        assertThat(options.getParallelProcessing()).isEqualTo(1);
    }

    @Test
    public void inventorySyncOptionsBuilder_returnsProperOptionsInstance_havingOnlyRequiredFieldsSet2() {
        final SphereClientConfig clientConfig = SphereClientConfigBuilder.ofKeyIdSecret(CTP_KEY, CTP_ID, CTP_SECRET)
                .build();
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(clientConfig).build();
        assertThat(options).isNotNull();
        assertThat(options.getClientConfig().getClientId()).isEqualTo(CTP_ID);
        assertThat(options.getClientConfig().getProjectKey()).isEqualTo(CTP_KEY);
        assertThat(options.getClientConfig().getClientSecret()).isEqualTo(CTP_SECRET);
        assertThat(options.getBatchSize()).isEqualTo(30);
        assertThat(options.isEnsureChannels()).isFalse();
        assertThat(options.getParallelProcessing()).isEqualTo(1);
    }

    @Test
    public void inventorySyncOptionsBuilder_returnsProperOptionsInstance_havingAllFieldsSet() {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(CTP_KEY, CTP_ID, CTP_SECRET)
                .batchSize(10)
                .ensureChannels(true)
                .parallelProcessing(10)
                .build();
        assertThat(options).isNotNull();
        assertThat(options.getClientConfig().getClientId()).isEqualTo(CTP_ID);
        assertThat(options.getClientConfig().getProjectKey()).isEqualTo(CTP_KEY);
        assertThat(options.getClientConfig().getClientSecret()).isEqualTo(CTP_SECRET);
        assertThat(options.getBatchSize()).isEqualTo(10);
        assertThat(options.isEnsureChannels()).isTrue();
        assertThat(options.getParallelProcessing()).isEqualTo(10);
    }

    @Test
    public void inventorySyncOptionsBuilder_wontSetParallelProcessing_havingZeroOrNegativePassed() {
        final InventorySyncOptionsBuilder builder = InventorySyncOptionsBuilder.of(CTP_KEY, CTP_ID, CTP_SECRET);
        final InventorySyncOptions optionsWithZero = builder.parallelProcessing(0).build();
        final InventorySyncOptions optionsWithNegative = builder.parallelProcessing(-1).build();
        assertThat(optionsWithZero.getParallelProcessing()).isEqualTo(1);
        assertThat(optionsWithNegative.getParallelProcessing()).isEqualTo(1);
    }

    @Test
    public void inventorySyncOptionsBuilder_wontSetBatchSize_havingZeroOrNegativePassed() {
        final InventorySyncOptionsBuilder builder = InventorySyncOptionsBuilder.of(CTP_KEY, CTP_ID, CTP_SECRET);
        final InventorySyncOptions optionsWithZero = builder.batchSize(0).build();
        final InventorySyncOptions optionsWithNegative = builder.batchSize(-1).build();
        assertThat(optionsWithZero.getBatchSize()).isEqualTo(30);
        assertThat(optionsWithNegative.getBatchSize()).isEqualTo(30);
    }
}
