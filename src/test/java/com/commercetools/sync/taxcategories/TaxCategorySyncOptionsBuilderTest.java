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
    private TaxCategorySyncOptionsBuilder stateSyncOptionsBuilder = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    void of_WithClient_ShouldCreateTaxCategorySyncOptionsBuilder() {
        TaxCategorySyncOptionsBuilder builder = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT);

        assertThat(builder).as("Builder should be created").isNotNull();
    }

    @Test
    void getThis_ShouldReturnBuilderInstance() {
        TaxCategorySyncOptionsBuilder instance = stateSyncOptionsBuilder.getThis();

        assertThat(instance).as("Builder should not be null").isNotNull();
        assertThat(instance).as("Builder is instance of 'TaxCategorySyncOptionsBuilder'")
            .isInstanceOf(TaxCategorySyncOptionsBuilder.class);
    }

    @Test
    void build_WithClient_ShouldBuildSyncOptions() {
        TaxCategorySyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions).as("Created instance should not be null").isNotNull();
        assertAll(
            () -> assertThat(stateSyncOptions.getBeforeUpdateCallback())
                .as("'beforeUpdateCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getBeforeCreateCallback())
                .as("'beforeCreateCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getErrorCallBack())
                .as("'errorCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getWarningCallBack())
                .as("'warningCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getCtpClient())
                .as("'ctpClient' should be equal to prepared one").isEqualTo(CTP_CLIENT),
            () -> assertThat(stateSyncOptions.getBatchSize())
                .as("'batchSize' should be equal to default value")
                .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT)
        );
    }

    @Test
    void build_WithBeforeUpdateCallback_ShouldSetBeforeUpdateCallback() {
        TriFunction<List<UpdateAction<TaxCategory>>, TaxCategoryDraft, TaxCategory, List<UpdateAction<TaxCategory>>>
            beforeUpdateCallback = (updateActions, newTaxCategory, oldTaxCategory) -> emptyList();
        stateSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        TaxCategorySyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getBeforeUpdateCallback())
            .as("'beforeUpdateCallback' should not be null").isNotNull();
    }

    @Test
    void build_WithBeforeCreateCallback_ShouldSetBeforeCreateCallback() {
        stateSyncOptionsBuilder.beforeCreateCallback((newTaxCategory) -> null);

        TaxCategorySyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getBeforeCreateCallback())
            .as("'beforeCreateCallback' should be null").isNotNull();
    }

    @Test
    void build_WithErrorCallback_ShouldSetErrorCallback() {
        BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        stateSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        TaxCategorySyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getErrorCallBack()).as("'errorCallback' should be null").isNotNull();
    }

    @Test
    void build_WithWarningCallback_ShouldSetWarningCallback() {
        Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        stateSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        TaxCategorySyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getWarningCallBack()).as("'warningCallback' should be null").isNotNull();
    }


    @Test
    void build_WithBatchSize_ShouldSetBatchSize() {
        TaxCategorySyncOptions stateSyncOptions = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(10)
            .build();

        assertThat(stateSyncOptions.getBatchSize()).as("'batchSize' should be equal to prepared value")
            .isEqualTo(10);
    }

    @Test
    void build_WithInvalidBatchSize_ShouldBuildSyncOptions() {
        TaxCategorySyncOptions stateSyncOptionsWithZeroBatchSize = TaxCategorySyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(0)
            .build();

        assertThat(stateSyncOptionsWithZeroBatchSize.getBatchSize())
            .as("'batchSize' should be equal to default value")
            .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        TaxCategorySyncOptions stateSyncOptionsWithNegativeBatchSize = TaxCategorySyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();

        assertThat(stateSyncOptionsWithNegativeBatchSize.getBatchSize())
            .as("'batchSize' should be equal to default value")
            .isEqualTo(TaxCategorySyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

}
