package com.commercetools.sync.inventory;

import com.commercetools.sync.commons.helpers.CtpClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InventorySyncOptionsTest {

    @Test
    public void build_WithOnlyRequiredFieldsSet_ShouldReturnProperOptionsInstance() {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(mock(CtpClient.class)).build();
        assertThat(options).isNotNull();
        assertThat(options.getBatchSize()).isEqualTo(30);
        assertThat(options.isEnsureChannels()).isFalse();
        assertThat(options.getParallelProcessing()).isEqualTo(1);
    }

    @Test
    public void build_WithAllFieldsSet_ShouldReturnProperOptionsInstance() {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(mock(CtpClient.class))
                .setBatchSize(10)
                .ensureChannels(true)
                .setParallelProcessing(10)
                .build();
        assertThat(options).isNotNull();
        assertThat(options.getBatchSize()).isEqualTo(10);
        assertThat(options.isEnsureChannels()).isTrue();
        assertThat(options.getParallelProcessing()).isEqualTo(10);
    }

    @Test
    public void setParallelProcessing_WithZeroOrNegativeArgument_ShouldNotSetParallelProcessing() {
        final InventorySyncOptionsBuilder builder = InventorySyncOptionsBuilder.of(mock(CtpClient.class));
        final InventorySyncOptions optionsWithZero = builder.setParallelProcessing(0).build();
        final InventorySyncOptions optionsWithNegative = builder.setParallelProcessing(-1).build();
        assertThat(optionsWithZero.getParallelProcessing()).isEqualTo(1);
        assertThat(optionsWithNegative.getParallelProcessing()).isEqualTo(1);
    }

    @Test
    public void setBatchSize_WithZeroOrNegativePassed_ShouldNotSetBatchSize() {
        final InventorySyncOptionsBuilder builder = InventorySyncOptionsBuilder.of(mock(CtpClient.class));
        final InventorySyncOptions optionsWithZero = builder.setBatchSize(0).build();
        final InventorySyncOptions optionsWithNegative = builder.setBatchSize(-1).build();
        assertThat(optionsWithZero.getBatchSize()).isEqualTo(30);
        assertThat(optionsWithNegative.getBatchSize()).isEqualTo(30);
    }
}
