package com.commercetools.sync.integration.ctpprojectsource.products.templates.beforeupdatecallback;

import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_NO_VARS_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_WITH_VARS_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductDraftBuilder;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.products.templates.beforeupdatecallback.KeepOtherVariantsSync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeepOtherVariantsSyncIT {

  private static ProductType productType;
  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Delete all product related test data from target project. Then creates a productType for the
   * products of the target CTP project.
   */
  @BeforeAll
  static void setupAllTests() {
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /**
   * 1. Clears all sync collections used for test assertions. 2. Deletes all products from target
   * CTP project 3. Creates an instance for {@link ProductSyncOptions} that will be used in the
   * test. 4. Creates a product in the target CTP project with 1 variant other than the master
   * variant.
   */
  @BeforeEach
  void setupPerTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    syncOptions = getProductSyncOptions();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  private ProductSyncOptions getProductSyncOptions() {
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };

    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallBack)
        .warningCallback(warningCallBack)
        .beforeUpdateCallback(
            (updateActions, newProductDraft, oldProduct1) ->
                KeepOtherVariantsSync.keepOtherVariants(updateActions))
        .build();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withRemovedVariants_shouldNotRemoveVariants() {
    // Create a product with variants in target project
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_WITH_VARS_RESOURCE_PATH, productType.toResourceIdentifier())
            .build();
    final Product oldProduct =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    // A product without variants which should be synced
    final ProductDraft newProductDraft =
        createProductDraftBuilder(
                PRODUCT_NO_VARS_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build())
            .build();

    // old product has 1 variant
    assertThat(oldProduct.getMasterData().getStaged().getVariants()).hasSize(1);
    // new product has no variants
    assertThat(newProductDraft.getVariants()).isEmpty();

    final ProductSync productSync = new ProductSync(syncOptions);

    // test
    final ProductSyncStatistics syncStatistics =
        (productSync.sync(singletonList(newProductDraft))).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Assert that the variant wasn't removed
    final Product product =
        CTP_TARGET_CLIENT
            .products()
            .withKey(oldProduct.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    assertThat(product).isNotNull();
    assertThat(product.getMasterData().getCurrent().getVariants()).hasSize(2);
  }
}
