package com.commercetools.sync.producttypes;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.updateactions.ChangeName;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductTypeSyncOptionsBuilderTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private ProductTypeSyncOptionsBuilder productTypeSyncOptionsBuilder = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    public void of_WithClient_ShouldCreateProductTypeSyncOptionsBuilder() {
        final ProductTypeSyncOptionsBuilder builder = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT);
        assertThat(builder).isNotNull();
    }

    @Test
    public void build_WithClient_ShouldBuildProductSyncOptions() {
        final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
        assertThat(productTypeSyncOptions).isNotNull();
        assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNull();
        assertThat(productTypeSyncOptions.getErrorCallBack()).isNull();
        assertThat(productTypeSyncOptions.getWarningCallBack()).isNull();
        assertThat(productTypeSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
        assertThat(productTypeSyncOptions.getBatchSize()).isEqualTo(ProductTypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

    @Test
    public void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final TriFunction<List<UpdateAction<ProductType>>,
                ProductTypeDraft, ProductType, List<UpdateAction<ProductType>>>
            beforeUpdateCallback = (updateActions, newProductType, oldProductType) -> Collections.emptyList();
        productTypeSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
        assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    public void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
        productTypeSyncOptionsBuilder.beforeCreateCallback((newProductType) -> null);

        final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
        assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    public void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        productTypeSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
        assertThat(productTypeSyncOptions.getErrorCallBack()).isNotNull();
    }

    @Test
    public void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        productTypeSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
        assertThat(productTypeSyncOptions.getWarningCallBack()).isNotNull();
    }

    @Test
    public void getThis_ShouldReturnCorrectInstance() {
        final ProductTypeSyncOptionsBuilder instance = productTypeSyncOptionsBuilder.getThis();
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(ProductTypeSyncOptionsBuilder.class);
        assertThat(instance).isEqualTo(productTypeSyncOptionsBuilder);
    }

    @Test
    public void productTypeSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newProduct) -> null)
            .beforeUpdateCallback((updateActions, newProductType, oldProductType) -> Collections.emptyList())
            .build();
        assertThat(productTypeSyncOptions).isNotNull();
    }

    @Test
    public void batchSize_WithPositiveValue_ShouldSetBatchSize() {
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .batchSize(10)
                                                                               .build();
        assertThat(productTypeSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    public void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final ProductTypeSyncOptions productTypeSyncOptionsWithZeroBatchSize =
                ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                    .batchSize(0)
                    .build();

        assertThat(productTypeSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(ProductTypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final ProductTypeSyncOptions productTypeSyncOptionsWithNegativeBatchSize = ProductTypeSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();

        assertThat(productTypeSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(ProductTypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }


    @Test
    public void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                .build();
        assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNull();

        final List<UpdateAction<ProductType>> updateActions = singletonList(ChangeName.of("name"));
        final List<UpdateAction<ProductType>> filteredList = productTypeSyncOptions
                .applyBeforeUpdateCallBack(updateActions, mock(ProductTypeDraft.class), mock(ProductType.class));
        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    public void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
        final TriFunction<List<UpdateAction<ProductType>>,
                ProductTypeDraft, ProductType, List<UpdateAction<ProductType>>>
                beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeUpdateCallback(
                        beforeUpdateCallback)
                .build();
        assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<ProductType>> updateActions = singletonList(ChangeName.of("name"));
        final List<UpdateAction<ProductType>> filteredList =
                productTypeSyncOptions.applyBeforeUpdateCallBack(updateActions,
                        mock(ProductTypeDraft.class), mock(ProductType.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    @Test
    public void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final TriFunction<List<UpdateAction<ProductType>>,
                ProductTypeDraft, ProductType, List<UpdateAction<ProductType>>>
                beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> Collections.emptyList();
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeUpdateCallback(
                        beforeUpdateCallback)
                .build();
        assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<ProductType>> updateActions = singletonList(ChangeName.of("name"));
        final List<UpdateAction<ProductType>> filteredList =
                productTypeSyncOptions.applyBeforeUpdateCallBack(updateActions,
                        mock(ProductTypeDraft.class), mock(ProductType.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    @Test
    public void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
        final Function<ProductTypeDraft, ProductTypeDraft> draftFunction = productTypeDraft ->
                ProductTypeDraftBuilder.of(productTypeDraft).key(productTypeDraft.getKey() + "_filteredKey").build();

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeCreateCallback(draftFunction)
                .build();
        assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNotNull();

        final ProductTypeDraft resourceDraft = mock(ProductTypeDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");


        final Optional<ProductTypeDraft> filteredDraft =
                productTypeSyncOptions.applyBeforeCreateCallBack(resourceDraft);
        
        assertThat(filteredDraft).hasValueSatisfying(productTypeDraft ->
            assertThat(productTypeDraft.getKey()).isEqualTo("myKey_filteredKey"));

    }

    @Test
    public void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNull();

        final ProductTypeDraft resourceDraft = mock(ProductTypeDraft.class);
        final Optional<ProductTypeDraft> filteredDraft =
                productTypeSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }

    @Test
    public void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
        final Function<ProductTypeDraft, ProductTypeDraft> draftFunction = productDraft -> null;
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeCreateCallback(draftFunction)
                .build();
        assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNotNull();

        final ProductTypeDraft resourceDraft = mock(ProductTypeDraft.class);
        final Optional<ProductTypeDraft> filteredDraft =
                productTypeSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertThat(filteredDraft).isEmpty();
    }



}
