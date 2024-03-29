package com.commercetools.sync.products;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductChangeNameActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductSyncOptionsBuilderTest {
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private ProductSyncOptionsBuilder productSyncOptionsBuilder =
      ProductSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateProductSyncOptionsBuilder() {
    final ProductSyncOptionsBuilder builder = ProductSyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildProductSyncOptions() {
    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    assertThat(productSyncOptions).isNotNull();
    assertThat(productSyncOptions.getSyncFilter()).isNotNull();
    assertThat(productSyncOptions.getSyncFilter()).isSameAs(SyncFilter.of());
    Assertions.assertThat(productSyncOptions.getBeforeUpdateCallback()).isNull();
    Assertions.assertThat(productSyncOptions.getBeforeCreateCallback()).isNull();
    Assertions.assertThat(productSyncOptions.getErrorCallback()).isNull();
    Assertions.assertThat(productSyncOptions.getWarningCallback()).isNull();
    Assertions.assertThat(productSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    Assertions.assertThat(productSyncOptions.getBatchSize())
        .isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    Assertions.assertThat(productSyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void syncFilter_WithNoSyncFilter_ShouldSetDefaultFilter() {
    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    assertThat(productSyncOptions.getSyncFilter()).isNotNull();
    assertThat(productSyncOptions.getSyncFilter()).isSameAs(SyncFilter.of());
  }

  @Test
  void syncFilter_WithSyncFilter_ShouldSetFilter() {
    productSyncOptionsBuilder.syncFilter(SyncFilter.ofWhiteList(ActionGroup.IMAGES));

    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    assertThat(productSyncOptions.getSyncFilter()).isNotNull();
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        beforeUpdateCallback = (updateActions, newProduct, oldProduct) -> emptyList();
    productSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    Assertions.assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    productSyncOptionsBuilder.beforeCreateCallback((newProduct) -> null);

    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    Assertions.assertThat(productSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  public void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        mockErrorCallback = (exception, newDraft, old, actions) -> {};
    productSyncOptionsBuilder.errorCallback(mockErrorCallback);

    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    Assertions.assertThat(productSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  public void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        mockWarningCallBack = (exception, newDraft, old) -> {};
    productSyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final ProductSyncOptions productSyncOptions = productSyncOptionsBuilder.build();
    Assertions.assertThat(productSyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final ProductSyncOptionsBuilder instance = productSyncOptionsBuilder.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(ProductSyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(productSyncOptionsBuilder);
  }

  @Test
  void productSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newProduct) -> null)
            .beforeUpdateCallback((updateActions, newCategory, oldCategory) -> emptyList())
            .build();
    assertThat(productSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    Assertions.assertThat(productSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final ProductSyncOptions productSyncOptionsWithZeroBatchSize =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();
    Assertions.assertThat(productSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final ProductSyncOptions productSyncOptionsWithNegativeBatchSize =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();
    Assertions.assertThat(productSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(ProductSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(productSyncOptions.getBeforeUpdateCallback()).isNull();

    final List<ProductUpdateAction> updateActions =
        Collections.singletonList(
            ProductChangeNameActionBuilder.of().name(LocalizedString.ofEnglish("name")).build());
    final List<ProductUpdateAction> filteredList =
        productSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductDraft.class), mock(ProductProjection.class));
    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();
    Assertions.assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ProductUpdateAction> updateActions =
        Collections.singletonList(
            ProductChangeNameActionBuilder.of().name(LocalizedString.ofEnglish("name")).build());
    final List<ProductUpdateAction> filteredList =
        productSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductDraft.class), mock(ProductProjection.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();

    Assertions.assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ProductUpdateAction> updateActions = emptyList();
    final List<ProductUpdateAction> filteredList =
        productSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductDraft.class), mock(ProductProjection.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> emptyList();
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).beforeUpdateCallback(beforeUpdateCallback).build();
    Assertions.assertThat(productSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ProductUpdateAction> updateActions =
        Collections.singletonList(
            ProductChangeNameActionBuilder.of().name(LocalizedString.ofEnglish("name")).build());
    final List<ProductUpdateAction> filteredList =
        productSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductDraft.class), mock(ProductProjection.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<ProductDraft, ProductDraft> draftFunction =
        productDraft ->
            ProductDraftBuilder.of(productDraft)
                .key(productDraft.getKey() + "_filteredKey")
                .build();

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    Assertions.assertThat(productSyncOptions.getBeforeCreateCallback()).isNotNull();

    final ProductDraft resourceDraft = mock(ProductDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getProductType())
        .thenReturn(ProductTypeResourceIdentifierBuilder.of().id("productTypeId").build());
    when(resourceDraft.getName()).thenReturn(LocalizedString.ofEnglish("myName"));
    when(resourceDraft.getSlug()).thenReturn(LocalizedString.ofEnglish("mySlug"));

    final Optional<ProductDraft> filteredDraft =
        productSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isNotEmpty();
    assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(productSyncOptions.getBeforeCreateCallback()).isNull();

    final ProductDraft resourceDraft = mock(ProductDraft.class);
    final Optional<ProductDraft> filteredDraft =
        productSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithNullReturnCallback_ShouldReturnEmptyOptional() {
    final Function<ProductDraft, ProductDraft> draftFunction = productDraft -> null;
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    Assertions.assertThat(productSyncOptions.getBeforeCreateCallback()).isNotNull();

    final ProductDraft resourceDraft = mock(ProductDraft.class);
    final Optional<ProductDraft> filteredDraft =
        productSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    Assertions.assertThat(productSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final ProductSyncOptions productSyncOptionsWithZeroCacheSize =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();
    Assertions.assertThat(productSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    final ProductSyncOptions productSyncOptionsWithNegativeCacheSize =
        ProductSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();
    Assertions.assertThat(productSyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
  }
}
