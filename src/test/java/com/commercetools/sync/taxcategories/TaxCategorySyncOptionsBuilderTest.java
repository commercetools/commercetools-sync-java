package com.commercetools.sync.taxcategories;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryDraft;
import com.commercetools.api.models.tax_category.TaxCategoryUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TaxCategorySyncOptionsBuilderTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private final TaxCategorySyncOptionsBuilder taxCategorySyncOptionsBuilder =
      TaxCategorySyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateTaxCategorySyncOptionsBuilder() {
    final TaxCategorySyncOptionsBuilder builder = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT);

    assertThat(builder).isNotNull();
  }

  @Test
  void getThis_ShouldReturnBuilderInstance() {
    final TaxCategorySyncOptionsBuilder instance = taxCategorySyncOptionsBuilder.getThis();

    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(TaxCategorySyncOptionsBuilder.class);
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

    assertThat(taxCategorySyncOptions).isNotNull();
    assertAll(
        () -> Assertions.assertThat(taxCategorySyncOptions.getBeforeUpdateCallback()).isNull(),
        () -> Assertions.assertThat(taxCategorySyncOptions.getBeforeCreateCallback()).isNull(),
        () -> Assertions.assertThat(taxCategorySyncOptions.getErrorCallback()).isNull(),
        () -> Assertions.assertThat(taxCategorySyncOptions.getWarningCallback()).isNull(),
        () -> Assertions.assertThat(taxCategorySyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT),
        () ->
            Assertions.assertThat(taxCategorySyncOptions.getBatchSize())
                .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT),
        () -> Assertions.assertThat(taxCategorySyncOptions.getCacheSize()).isEqualTo(10_000));
  }

  @Test
  void build_WithBeforeUpdateCallback_ShouldSetBeforeUpdateCallback() {
    final TriFunction<
            List<TaxCategoryUpdateAction>,
            TaxCategoryDraft,
            TaxCategory,
            List<TaxCategoryUpdateAction>>
        beforeUpdateCallback = (updateActions, newTaxCategory, oldTaxCategory) -> emptyList();
    taxCategorySyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

    Assertions.assertThat(taxCategorySyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void build_WithBeforeCreateCallback_ShouldSetBeforeCreateCallback() {
    taxCategorySyncOptionsBuilder.beforeCreateCallback((newTaxCategory) -> null);

    final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

    Assertions.assertThat(taxCategorySyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void build_WithErrorCallback_ShouldSetErrorCallback() {
    final QuadConsumer<
            SyncException,
            Optional<TaxCategoryDraft>,
            Optional<TaxCategory>,
            List<TaxCategoryUpdateAction>>
        mockErrorCallBack = (exception, entry, draft, actions) -> {};
    taxCategorySyncOptionsBuilder.errorCallback(mockErrorCallBack);

    final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

    Assertions.assertThat(taxCategorySyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void build_WithWarningCallback_ShouldSetWarningCallback() {
    final TriConsumer<SyncException, Optional<TaxCategoryDraft>, Optional<TaxCategory>>
        mockWarningCallBack = (warningMessage, draft, entry) -> {};
    taxCategorySyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

    Assertions.assertThat(taxCategorySyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void build_WithBatchSize_ShouldSetBatchSize() {
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();

    Assertions.assertThat(taxCategorySyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void build_WithInvalidBatchSize_ShouldBuildSyncOptions() {
    final TaxCategorySyncOptions taxCategorySyncOptionsWithZeroBatchSize =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();

    Assertions.assertThat(taxCategorySyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final TaxCategorySyncOptions taxCategorySyncOptionsWithNegativeBatchSize =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();

    Assertions.assertThat(taxCategorySyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void build_WithCacheSize_ShouldSetCacheSize() {
    final TaxCategorySyncOptions taxCategorySyncOptions =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();

    Assertions.assertThat(taxCategorySyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void build_WithZeroOrNegativeCacheSize_ShouldBuildSyncOptions() {
    final TaxCategorySyncOptions taxCategorySyncOptionsWithZeroCacheSize =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();

    Assertions.assertThat(taxCategorySyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    final TaxCategorySyncOptions taxCategorySyncOptionsWithNegativeCacheSize =
        TaxCategorySyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();

    Assertions.assertThat(taxCategorySyncOptionsWithNegativeCacheSize.getCacheSize())
        .isEqualTo(10_000);
  }
}
