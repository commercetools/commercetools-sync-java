package com.commercetools.sync.taxcategories;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

class TaxCategorySyncOptionsBuilderTest {

    private static SphereClient CTP_CLIENT = mock(SphereClient.class);
    private TaxCategorySyncOptionsBuilder taxCategorySyncOptionsBuilder = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT);

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
            () -> assertThat(taxCategorySyncOptions.getBeforeUpdateCallback()).isNull(),
            () -> assertThat(taxCategorySyncOptions.getBeforeCreateCallback()).isNull(),
            () -> assertThat(taxCategorySyncOptions.getErrorCallBack()).isNull(),
            () -> assertThat(taxCategorySyncOptions.getWarningCallBack()).isNull(),
            () -> assertThat(taxCategorySyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT),
            () -> assertThat(taxCategorySyncOptions.getBatchSize())
                .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT)
        );
    }

    @Test
    void build_WithBeforeUpdateCallback_ShouldSetBeforeUpdateCallback() {
        final TriFunction<List<UpdateAction<TaxCategory>>, TaxCategoryDraft, TaxCategory,
            List<UpdateAction<TaxCategory>>> beforeUpdateCallback = (updateActions, newTaxCategory, oldTaxCategory) ->
            emptyList();
        taxCategorySyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

        assertThat(taxCategorySyncOptions.getBeforeUpdateCallback())
            .isNotNull();
    }

    @Test
    void build_WithBeforeCreateCallback_ShouldSetBeforeCreateCallback() {
        taxCategorySyncOptionsBuilder.beforeCreateCallback((newTaxCategory) -> null);

        final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

        assertThat(taxCategorySyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    void build_WithErrorCallback_ShouldSetErrorCallback() {
        final BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        taxCategorySyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

        assertThat(taxCategorySyncOptions.getErrorCallBack()).isNotNull();
    }

    @Test
    void build_WithWarningCallback_ShouldSetWarningCallback() {
        final Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        taxCategorySyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final TaxCategorySyncOptions taxCategorySyncOptions = taxCategorySyncOptionsBuilder.build();

        assertThat(taxCategorySyncOptions.getWarningCallBack()).isNotNull();
    }


    @Test
    void build_WithBatchSize_ShouldSetBatchSize() {
        final TaxCategorySyncOptions taxCategorySyncOptions = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(10)
            .build();

        assertThat(taxCategorySyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    void build_WithInvalidBatchSize_ShouldBuildSyncOptions() {
        final TaxCategorySyncOptions taxCategorySyncOptionsWithZeroBatchSize = TaxCategorySyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(0)
            .build();

        assertThat(taxCategorySyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final TaxCategorySyncOptions taxCategorySyncOptionsWithNegativeBatchSize = TaxCategorySyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();

        assertThat(taxCategorySyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

}
