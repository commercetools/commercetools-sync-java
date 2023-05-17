package com.commercetools.sync.sdk2.producttypes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeChangeNameActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ProductTypeSyncOptionsBuilderTest {
  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private final ProductTypeSyncOptionsBuilder productTypeSyncOptionsBuilder =
      ProductTypeSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateProductTypeSyncOptionsBuilder() {
    final ProductTypeSyncOptionsBuilder builder = ProductTypeSyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildProductSyncOptions() {
    final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
    assertThat(productTypeSyncOptions).isNotNull();
    assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNull();
    assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNull();
    assertThat(productTypeSyncOptions.getErrorCallback()).isNull();
    assertThat(productTypeSyncOptions.getWarningCallback()).isNull();
    assertThat(productTypeSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    assertThat(productTypeSyncOptions.getBatchSize())
        .isEqualTo(ProductTypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    assertThat(productTypeSyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeUpdateCallback_WithFilterAsCallback_ShouldSetCallback() {
    final TriFunction<
            List<ProductTypeUpdateAction>,
            ProductTypeDraft,
            ProductType,
            List<ProductTypeUpdateAction>>
        beforeUpdateCallback =
            (updateActions, newProductType, oldProductType) -> Collections.emptyList();
    productTypeSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
    assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    productTypeSyncOptionsBuilder.beforeCreateCallback((newProductType) -> null);

    final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
    assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  public void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException,
            Optional<ProductTypeDraft>,
            Optional<ProductType>,
            List<ProductTypeUpdateAction>>
        mockErrorCallback = (exception, newDraft, old, actions) -> {};
    productTypeSyncOptionsBuilder.errorCallback(mockErrorCallback);

    final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
    assertThat(productTypeSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  public void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<ProductTypeDraft>, Optional<ProductType>>
        mockWarningCallBack = (exception, newDraft, old) -> {};
    productTypeSyncOptionsBuilder.warningCallback(mockWarningCallBack);

    final ProductTypeSyncOptions productTypeSyncOptions = productTypeSyncOptionsBuilder.build();
    assertThat(productTypeSyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final ProductTypeSyncOptionsBuilder instance = productTypeSyncOptionsBuilder.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(ProductTypeSyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(productTypeSyncOptionsBuilder);
  }

  @Test
  void productTypeSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newProduct) -> null)
            .beforeUpdateCallback(
                (updateActions, newProductType, oldProductType) -> Collections.emptyList())
            .build();
    assertThat(productTypeSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    assertThat(productTypeSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final ProductTypeSyncOptions productTypeSyncOptionsWithZeroBatchSize =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();

    assertThat(productTypeSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(ProductTypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    final ProductTypeSyncOptions productTypeSyncOptionsWithNegativeBatchSize =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();

    assertThat(productTypeSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(ProductTypeSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullCallback_ShouldReturnIdenticalList() {
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNull();

    final List<ProductTypeUpdateAction> updateActions =
        singletonList(ProductTypeChangeNameActionBuilder.of().name("name").build());
    final List<ProductTypeUpdateAction> filteredList =
        productTypeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductTypeDraft.class), mock(ProductType.class));
    assertThat(filteredList).isSameAs(updateActions);
  }

  @Test
  void applyBeforeUpdateCallBack_WithNullReturnCallback_ShouldReturnEmptyList() {
    final TriFunction<
            List<ProductTypeUpdateAction>,
            ProductTypeDraft,
            ProductType,
            List<ProductTypeUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> null;
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ProductTypeUpdateAction> updateActions =
        singletonList(ProductTypeChangeNameActionBuilder.of().name("name").build());
    final List<ProductTypeUpdateAction> filteredList =
        productTypeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductTypeDraft.class), mock(ProductType.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  private interface MockTriFunction
      extends TriFunction<
          List<ProductTypeUpdateAction>,
          ProductTypeDraft,
          ProductType,
          List<ProductTypeUpdateAction>> {}

  @Test
  void applyBeforeUpdateCallBack_WithEmptyUpdateActions_ShouldNotApplyBeforeUpdateCallback() {
    final MockTriFunction beforeUpdateCallback = mock(MockTriFunction.class);

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();

    assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ProductTypeUpdateAction> updateActions = emptyList();
    final List<ProductTypeUpdateAction> filteredList =
        productTypeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductTypeDraft.class), mock(ProductType.class));

    assertThat(filteredList).isEmpty();
    verify(beforeUpdateCallback, never()).apply(any(), any(), any());
  }

  @Test
  void applyBeforeUpdateCallBack_WithCallback_ShouldReturnFilteredList() {
    final TriFunction<
            List<ProductTypeUpdateAction>,
            ProductTypeDraft,
            ProductType,
            List<ProductTypeUpdateAction>>
        beforeUpdateCallback = (updateActions, newCategory, oldCategory) -> Collections.emptyList();
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    assertThat(productTypeSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<ProductTypeUpdateAction> updateActions =
        singletonList(ProductTypeChangeNameActionBuilder.of().name("name").build());
    final List<ProductTypeUpdateAction> filteredList =
        productTypeSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(ProductTypeDraft.class), mock(ProductType.class));
    assertThat(filteredList).isNotEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<ProductTypeDraft, ProductTypeDraft> draftFunction =
        productTypeDraft ->
            ProductTypeDraftBuilder.of(productTypeDraft)
                .name("testName")
                .description("testDescription")
                .key(productTypeDraft.getKey() + "_filteredKey")
                .build();

    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNotNull();

    final ProductTypeDraft resourceDraft = mock(ProductTypeDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");

    final Optional<ProductTypeDraft> filteredDraft =
        productTypeSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft)
        .hasValueSatisfying(
            productTypeDraft ->
                assertThat(productTypeDraft.getKey()).isEqualTo("myKey_filteredKey"));
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).build();
    assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNull();

    final ProductTypeDraft resourceDraft = mock(ProductTypeDraft.class);
    final Optional<ProductTypeDraft> filteredDraft =
        productTypeSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<ProductTypeDraft, ProductTypeDraft> draftFunction = productDraft -> null;
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    assertThat(productTypeSyncOptions.getBeforeCreateCallback()).isNotNull();

    final ProductTypeDraft resourceDraft = mock(ProductTypeDraft.class);
    final Optional<ProductTypeDraft> filteredDraft =
        productTypeSyncOptions.applyBeforeCreateCallback(resourceDraft);

    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final ProductTypeSyncOptions productTypeSyncOptions =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    assertThat(productTypeSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final ProductTypeSyncOptions productTypeSyncOptionsWithZeroCacheSize =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();

    assertThat(productTypeSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    final ProductTypeSyncOptions productTypeSyncOptionsWithNegativeCacheSize =
        ProductTypeSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();

    assertThat(productTypeSyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
  }
}
