package com.commercetools.sync.sdk2.inventories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import org.junit.jupiter.api.Test;

class InventorySyncOptionsTest {

  @Test
  void build_WithOnlyRequiredFieldsSet_ShouldReturnProperOptionsInstance() {
    final InventorySyncOptions options =
        InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();
    assertThat(options).isNotNull();
    assertThat(options.getBatchSize()).isEqualTo(InventorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    assertThat(options.shouldEnsureChannels())
        .isEqualTo(InventorySyncOptionsBuilder.ENSURE_CHANNELS_DEFAULT);
  }

  @Test
  void build_WithAllFieldsSet_ShouldReturnProperOptionsInstance() {
    final InventorySyncOptions options =
        InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .batchSize(10)
            .ensureChannels(true)
            .build();
    assertThat(options).isNotNull();
    assertThat(options.getBatchSize()).isEqualTo(10);
    assertThat(options.shouldEnsureChannels()).isTrue();
  }

  @Test
  void setBatchSize_WithZeroOrNegativePassed_ShouldNotSetBatchSize() {
    final InventorySyncOptionsBuilder builder =
        InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class));
    final InventorySyncOptions optionsWithZero = builder.batchSize(0).build();
    final InventorySyncOptions optionsWithNegative = builder.batchSize(-1).build();
    assertThat(optionsWithZero.getBatchSize())
        .isEqualTo(InventorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    assertThat(optionsWithNegative.getBatchSize())
        .isEqualTo(InventorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }
}
