package com.commercetools.sync.inventories;

import io.sphere.sdk.client.SphereClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InventorySyncOptionsTest {

    @Test
    public void build_WithOnlyRequiredFieldsSet_ShouldReturnProperOptionsInstance() {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(mock(SphereClient.class)).build();
        assertThat(options).isNotNull();
        assertThat(options.getBatchSize()).isEqualTo(InventorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
        assertThat(options.shouldEnsureChannels()).isEqualTo(InventorySyncOptionsBuilder.ENSURE_CHANNELS_DEFAULT);
    }

    @Test
    public void build_WithAllFieldsSet_ShouldReturnProperOptionsInstance() {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                .setBatchSize(10)
                .ensureChannels(true)
                .build();
        assertThat(options).isNotNull();
        assertThat(options.getBatchSize()).isEqualTo(10);
        assertThat(options.shouldEnsureChannels()).isTrue();
    }

    @Test
    public void setBatchSize_WithZeroOrNegativePassed_ShouldNotSetBatchSize() {
        final InventorySyncOptionsBuilder builder = InventorySyncOptionsBuilder.of(mock(SphereClient.class));
        final InventorySyncOptions optionsWithZero = builder.setBatchSize(0).build();
        final InventorySyncOptions optionsWithNegative = builder.setBatchSize(-1).build();
        assertThat(optionsWithZero.getBatchSize()).isEqualTo(InventorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
        assertThat(optionsWithNegative.getBatchSize()).isEqualTo(InventorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }
}
