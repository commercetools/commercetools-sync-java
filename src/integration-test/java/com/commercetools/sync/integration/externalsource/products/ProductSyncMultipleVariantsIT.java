package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.*;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionByIdGet;
import io.sphere.sdk.producttypes.ProductType;
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
                PRODUCT_KEY_1_MULTIPLE_VARIANTS_RESOURCE_PATH, productType.toReference())
            .build();

    product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
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
                PRODUCT_KEY_2_MULTIPLE_VARIANTS_RESOURCE_PATH, productType.toReference())
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(productDraft)));

    AssertionsForStatistics.assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final ProductVariant fetchedMasterVariant =
        CTP_TARGET_CLIENT
            .execute(ProductProjectionByIdGet.of(product.getId(), ProductProjectionType.STAGED))
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
