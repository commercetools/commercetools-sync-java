package com.commercetools.sync.products;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.sync.products.ActionGroup.IMAGES;
import static com.commercetools.sync.products.SyncFilter.ofWhiteList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductSyncOptionsBuilderTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private ProductSyncOptionsBuilder productSyncOptionsBuilder = ProductSyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    public void of_WithClient_ShouldCreateProductSyncOptionsBuilder() {
        final ProductSyncOptionsBuilder builder = ProductSyncOptionsBuilder.of(CTP_CLIENT);
        assertThat(builder).isNotNull();
    }

    @Test
    public void build_WithClient_ShouldBuildProductSyncOptions() {
        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherCollectionEntries()).isTrue();
        assertThat(productSyncOptions.shouldRemoveOtherProperties()).isTrue();
        assertThat(productSyncOptions.shouldRemoveOtherSetEntries()).isTrue();
        assertThat(productSyncOptions.shouldRemoveOtherLocales()).isTrue();
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isFalse();
        assertThat(productSyncOptions.shouldRemoveOtherVariants()).isTrue();
        assertThat(productSyncOptions.getSyncFilter()).isNotNull();
        assertThat(productSyncOptions.getSyncFilter()).isSameAs(SyncFilter.of());
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNull();
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
    public void setSyncFilter_WithNoSyncFilter_ShouldSetDefaultFilter() {
        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getSyncFilter()).isNotNull();
        assertThat(productSyncOptions.getSyncFilter()).isSameAs(SyncFilter.of());
    }

    @Test
    public void setSyncFilter_WithSyncFilter_ShouldSetFilter() {
        productSyncOptionsBuilder.setSyncFilter(ofWhiteList(IMAGES));

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getSyncFilter()).isNotNull();
    }

    @Test
    public void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final Function<List<UpdateAction<Product>>,
                    List<UpdateAction<Product>>> clearListFilter = (updateActions -> Collections.emptyList());
        productSyncOptionsBuilder.beforeUpdateCallback(clearListFilter);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();
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
            .beforeUpdateCallback(updateActions -> Collections.emptyList())
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

    @Test
    public void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .build();
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNull();

        final List<UpdateAction<Product>> updateActions = Collections
            .singletonList(ChangeName.of(LocalizedString.ofEnglish("name")));
        final List<UpdateAction<Product>> filteredList = productSyncOptions.applyBeforeUpdateCallBack(updateActions);
        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    public void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .beforeUpdateCallback(list ->
                                                                                   Collections.emptyList())
                                                                               .build();
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<Product>> updateActions = Collections
            .singletonList(ChangeName.of(LocalizedString.ofEnglish("name")));
        final List<UpdateAction<Product>> filteredList = productSyncOptions.applyBeforeUpdateCallBack(updateActions);
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }
}
