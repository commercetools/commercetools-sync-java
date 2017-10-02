package com.commercetools.sync.products;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductSyncOptionsBuilderTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private ProductSyncOptionsBuilder productSyncOptionsBuilder = ProductSyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    public void of_WithClient_ShouldCreateCategorySyncOptionsBuilder() {
        final ProductSyncOptionsBuilder builder = ProductSyncOptionsBuilder.of(CTP_CLIENT);
        assertThat(builder).isNotNull();
    }

    @Test
    public void build_WithClient_ShouldBuildCategorySyncOptions() {
        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherCollectionEntries()).isTrue();
        assertThat(productSyncOptions.shouldRemoveOtherProperties()).isTrue();
        assertThat(productSyncOptions.shouldRemoveOtherSetEntries()).isTrue();
        assertThat(productSyncOptions.shouldRemoveOtherLocales()).isTrue();
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isFalse();
        assertThat(productSyncOptions.shouldRemoveOtherVariants()).isTrue();
        assertThat(productSyncOptions.getBlackList()).isEmpty();
        assertThat(productSyncOptions.getWhiteList()).isEmpty();
        assertThat(productSyncOptions.getUpdateActionsCallBack()).isNull();
        assertThat(productSyncOptions.getErrorCallBack()).isNull();
        assertThat(productSyncOptions.getWarningCallBack()).isNull();
        assertThat(productSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(productSyncOptions.getBatchSize()).isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    public void removeOtherVariants_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.removeOtherVariants(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherVariants()).isFalse();
    }

    @Test
    public void whiteList_WithNonEmptyList_ShouldSetWhiteList() {
        productSyncOptionsBuilder.whiteList(Collections.singletonList(UpdateFilter.NAME));

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getWhiteList()).hasSize(1);
        assertThat(productSyncOptions.getWhiteList().get(0)).isEqualTo(UpdateFilter.NAME);
    }

    @Test
    public void blackList_WithNonEmptyList_ShouldSetBlackList() {
        productSyncOptionsBuilder.blackList(Collections.singletonList(UpdateFilter.NAME));

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getBlackList()).hasSize(1);
        assertThat(productSyncOptions.getBlackList().get(0)).isEqualTo(UpdateFilter.NAME);
    }

    @Test
    public void setUpdateActionsFilter_WithFilter_ShouldSetFilter() {
        final Function<List<UpdateAction<Product>>,
            List<UpdateAction<Product>>> clearListFilter = (updateActions -> Collections.emptyList());
        productSyncOptionsBuilder.setUpdateActionsFilter(clearListFilter);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getUpdateActionsFilter()).isNotNull();
    }

    @Test
    public void setRemoveOtherCollectionEntries_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.setRemoveOtherCollectionEntries(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherCollectionEntries()).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherCollectionEntries()).isFalse();
    }

    @Test
    public void setRemoveOtherLocales_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.setRemoveOtherLocales(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherLocales()).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherLocales()).isFalse();
    }

    @Test
    public void setRemoveOtherProperties_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.setRemoveOtherProperties(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherProperties()).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherProperties()).isFalse();
    }

    @Test
    public void setAllowUuid_WithTrue_ShouldSetFlag() {
        productSyncOptionsBuilder.setAllowUuidKeys(true);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isNotNull();
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isTrue();
    }

    @Test
    public void setErrorCallBack_WithCallBack_ShouldSetCallBack() {
        final BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        productSyncOptionsBuilder.setErrorCallBack(mockErrorCallBack);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getErrorCallBack()).isNotNull();
    }

    @Test
    public void setWarningCallBack_WithCallBack_ShouldSetCallBack() {
        final Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        productSyncOptionsBuilder.setWarningCallBack(mockWarningCallBack);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getWarningCallBack()).isNotNull();
    }

    @Test
    public void setRemoveOtherSetEntries_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.setRemoveOtherSetEntries(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherSetEntries()).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherSetEntries()).isFalse();
    }

    @Test
    public void getThis_ShouldReturnCorrectInstance() {
        final ProductSyncOptionsBuilder instance = productSyncOptionsBuilder.getThis();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(ProductSyncOptionsBuilder.class);
        assertThat(instance).isEqualTo(productSyncOptionsBuilder);
    }

    @Test
    public void productSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
            .of(CTP_CLIENT)
            .setAllowUuidKeys(true)
            .setRemoveOtherLocales(false)
            .setBatchSize(30)
            .setUpdateActionsFilterCallBack(updateActions -> Collections.emptyList())
            .build();
        assertThat(productSyncOptions).isNotNull();
    }

    @Test
    public void setBatchSize_WithPositiveValue_ShouldSetBatchSize() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .setBatchSize(10)
                                                                               .build();
        assertThat(productSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    public void setBatchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final ProductSyncOptions productSyncOptionsWithZeroBatchSize = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                                .setBatchSize(0)
                                                                                                .build();
        assertThat(productSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final ProductSyncOptions productSyncOptionsWithNegativeBatchSize = ProductSyncOptionsBuilder
            .of(CTP_CLIENT)
            .setBatchSize(-100)
            .build();
        assertThat(productSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }
}
