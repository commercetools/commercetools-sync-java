package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.sync.products.helpers.ProductReferenceResolver.PRODUCT_TYPE_DOES_NOT_EXIST;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.producttypes.ProductType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductReferenceResolverIT {

  private static ProductType productType;
  private static List<Category> categories;
  private List<Throwable> errorCallBackExceptions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupPerTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
  }

  private void clearSyncTestCollections() {
    errorCallBackExceptions = new ArrayList<>();
  }

  private ProductSyncOptions getProductSyncOptions() {
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<UpdateAction<Product>>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackExceptions.add(exception.getCause());
            };

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT).errorCallback(errorCallBack).build();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withNewProductWithInvalidCategoryReferences_ShouldFailCreatingTheProduct() {
    // Create a list of category references that contains one valid and one invalid reference.
    final Set<ResourceIdentifier<Category>> invalidCategoryReferences = new HashSet<>();
    invalidCategoryReferences.add(ResourceIdentifier.ofId(categories.get(0).getId()));
    invalidCategoryReferences.add(ResourceIdentifier.ofId(null));

    // Create a product with the invalid category references. (i.e. not ready for reference
    // resolution).
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            referenceOfId(productType.getKey()),
            null,
            null,
            invalidCategoryReferences,
            createRandomCategoryOrderHints(invalidCategoryReferences));

    final ProductSync productSync = new ProductSync(getProductSyncOptions());
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(productDraft)));

    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).hasSize(1);
    final Throwable exception = errorCallBackExceptions.get(0);
    assertThat(exception)
        .isExactlyInstanceOf(CompletionException.class)
        .hasMessageContaining(
            "Failed to resolve 'category' resource identifier on ProductDraft "
                + "with key:'productKey1'")
        .hasMessageContaining(format("Reason: %s", BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
  }

  @Test
  void sync_withNewProductWithNonExistingProductTypeReference_ShouldFailCreatingTheProduct() {
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            ResourceIdentifier.ofKey("non-existing-key"),
            null,
            null,
            Collections.emptySet(),
            null);

    final ProductSync productSync = new ProductSync(getProductSyncOptions());
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(productDraft)));

    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions).hasSize(1);
    final Throwable exception = errorCallBackExceptions.get(0);
    assertThat(exception)
        .isExactlyInstanceOf(CompletionException.class)
        .hasMessageContaining(
            "Failed to resolve 'product-type' resource identifier on "
                + "ProductDraft with key:'productKey1'")
        .hasMessageContaining(
            format("Reason: %s", format(PRODUCT_TYPE_DOES_NOT_EXIST, "non-existing-key")));
  }
}
