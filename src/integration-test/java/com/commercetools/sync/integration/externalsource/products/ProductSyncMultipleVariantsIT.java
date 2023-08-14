package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_MULTIPLE_VARIANTS_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_MULTIPLE_VARIANTS_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductDraftBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProductSyncMultipleVariantsIT {

  private static ProductType productType;

  private List<String> errorCallBackMessages;

  private List<String> warningCallBackMessages;

  private List<Throwable> errorCallBackExceptions;

  private ProductSyncOptions syncOptions;

  private Product product;

  /**
   * Delete all product related test data from the target project. Then creates for the target CTP
   * project price a product type, a tax category, 2 categories, custom types for the categories and
   * a product state.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);

    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /**
   * Deletes Products and Types from the target CTP project, then it populates target CTP project
   * with product test data.
   */
  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    syncOptions = buildSyncOptions();

    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_MULTIPLE_VARIANTS_RESOURCE_PATH, productType.toResourceIdentifier())
            .build();

    product =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withReassignmentOfMasterVariant_shouldSyncProductCorrectly() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_MULTIPLE_VARIANTS_RESOURCE_PATH, productType.toResourceIdentifier())
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(List.of(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final ProductVariant fetchedMasterVariant =
        CTP_TARGET_CLIENT
            .productProjections()
            .withId(product.getId())
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjection::getMasterVariant)
            .toCompletableFuture()
            .join();

    assertThat(fetchedMasterVariant.getSku()).isEqualTo(productDraft.getMasterVariant().getSku());
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (exception, oldResource, newResource, updateActions) ->
                collectErrors(exception.getMessage(), exception.getCause()))
        .warningCallback(warningCallBack)
        .build();
  }

  private void collectErrors(final String errorMessage, final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }
}
