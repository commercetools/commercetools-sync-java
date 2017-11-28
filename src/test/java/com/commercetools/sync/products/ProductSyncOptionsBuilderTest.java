package com.commercetools.sync.products;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.commercetools.sync.products.ActionGroup.IMAGES;
import static com.commercetools.sync.products.SyncFilter.ofWhiteList;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isFalse();
        assertThat(productSyncOptions.shouldRemoveOtherVariants()).isTrue();
        assertThat(productSyncOptions.getSyncFilter()).isNotNull();
        assertThat(productSyncOptions.getSyncFilter()).isSameAs(SyncFilter.of());
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNull();
        assertThat(productSyncOptions.getBeforeCreateCallback()).isNull();
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
    public void syncFilter_WithNoSyncFilter_ShouldSetDefaultFilter() {
        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getSyncFilter()).isNotNull();
        assertThat(productSyncOptions.getSyncFilter()).isSameAs(SyncFilter.of());
    }

    @Test
    public void syncFilter_WithSyncFilter_ShouldSetFilter() {
        productSyncOptionsBuilder.syncFilter(ofWhiteList(IMAGES));

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getSyncFilter()).isNotNull();
    }

    @Test
    public void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            beforeUpdateCallback = (updateActions, newProduct, oldProduct) -> Collections.emptyList();
        productSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    public void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
        productSyncOptionsBuilder.beforeCreateCallback((newProduct) -> null);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    public void removeOtherCollectionEntries_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.removeOtherCollectionEntries(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherCollectionEntries()).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherCollectionEntries()).isFalse();
    }

    @Test
    public void removeOtherProperties_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.removeOtherProperties(false);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldRemoveOtherProperties()).isNotNull();
        assertThat(productSyncOptions.shouldRemoveOtherProperties()).isFalse();
    }

    @Test
    public void allowUuid_WithTrue_ShouldSetFlag() {
        productSyncOptionsBuilder.allowUuidKeys(true);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isNotNull();
        assertThat(productSyncOptions.shouldAllowUuidKeys()).isTrue();
    }

    @Test
    public void errorCallBack_WithCallBack_ShouldSetCallBack() {
        final BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        productSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getErrorCallBack()).isNotNull();
    }

    @Test
    public void warningCallBack_WithCallBack_ShouldSetCallBack() {
        final Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        productSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
        assertThat(productSyncOptions.getWarningCallBack()).isNotNull();
    }

    @Test
    public void removeOtherSetEntries_WithFalse_ShouldSetFlag() {
        productSyncOptionsBuilder.removeOtherSetEntries(false);

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
            .allowUuidKeys(true)
            .batchSize(30)
            .beforeCreateCallback((newProduct) -> null)
            .beforeUpdateCallback((updateActions, newCategory, oldCategory) -> Collections.emptyList())
            .build();
        assertThat(productSyncOptions).isNotNull();
    }

    @Test
    public void batchSize_WithPositiveValue_ShouldSetBatchSize() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .batchSize(10)
                                                                               .build();
        assertThat(productSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    public void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
        final ProductSyncOptions productSyncOptionsWithZeroBatchSize = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                                                .batchSize(0)
                                                                                                .build();
        assertThat(productSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        final ProductSyncOptions productSyncOptionsWithNegativeBatchSize = ProductSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
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
            .singletonList(ChangeName.of(ofEnglish("name")));
        final List<UpdateAction<Product>> filteredList =
            productSyncOptions.applyBeforeUpdateCallBack(updateActions, mock(ProductDraft.class), mock(Product.class));
        assertThat(filteredList).isSameAs(updateActions);
    }

    @Test
    public void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> Collections.emptyList();
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .beforeUpdateCallback(
                                                                                   beforeUpdateCallback)
                                                                               .build();
        assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();

        final List<UpdateAction<Product>> updateActions = Collections.singletonList(ChangeName.of(ofEnglish("name")));
        final List<UpdateAction<Product>> filteredList =
            productSyncOptions.applyBeforeUpdateCallBack(updateActions, mock(ProductDraft.class), mock(Product.class));
        assertThat(filteredList).isNotEqualTo(updateActions);
        assertThat(filteredList).isEmpty();
    }

    @Test
    public void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
        final Function<ProductDraft, ProductDraft> draftFunction =
                productDraft -> ProductDraftBuilder.of(productDraft)
                                                   .key(productDraft.getKey() + "_filteredKey").build();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                .beforeCreateCallback(draftFunction)
                .build();
        assertThat(productSyncOptions.getBeforeCreateCallback()).isNotNull();

        final ProductDraft resourceDraft = mock(ProductDraft.class);
        when(resourceDraft.getKey()).thenReturn("myKey");


        final Optional<ProductDraft> filteredDraft =
                productSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertThat(filteredDraft).isNotEmpty();
        assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
    }

    @Test
    public void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT).build();
        assertThat(productSyncOptions.getBeforeCreateCallback()).isNull();

        final ProductDraft resourceDraft = mock(ProductDraft.class);
        final Optional<ProductDraft> filteredDraft =
                productSyncOptions.applyBeforeCreateCallBack(resourceDraft);

        assertThat(filteredDraft).containsSame(resourceDraft);
    }
}
