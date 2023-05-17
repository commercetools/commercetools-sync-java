package com.commercetools.sync.integration.sdk2.externalsource.products;

import static com.commercetools.sync.integration.sdk2.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.helpers.ProductReferenceResolver.PRODUCT_TYPE_DOES_NOT_EXIST;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeReferenceBuilder;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    categories = ensureCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
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
            List<ProductUpdateAction>>
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
    final List<CategoryReference> invalidCategoryReferences = new ArrayList<>();
    invalidCategoryReferences.add(
        CategoryReferenceBuilder.of().id(categories.get(0).getId()).build());
    invalidCategoryReferences.add(CategoryReferenceBuilder.of().id("").build());

    // Create a product with the invalid category references. (i.e. not ready for reference
    // resolution).
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            ProductTypeReferenceBuilder.of().id(productType.getId()).build(),
            null,
            null,
            invalidCategoryReferences,
            createRandomCategoryOrderHints(invalidCategoryReferences));

    final ProductSync productSync = new ProductSync(getProductSyncOptions());
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

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
            ProductTypeResourceIdentifierBuilder.of().key("non-existing-key").build(),
            null,
            null,
            Collections.emptyList(),
            null);

    final ProductSync productSync = new ProductSync(getProductSyncOptions());
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

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

  @Test
  void sync_withNewProductWithNullVariants_ShouldCreateNewProduct() {
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            ProductTypeReferenceBuilder.of().id(productType.getId()).build(),
            null,
            null,
            null,
            null);
    productDraft.setVariants((List<ProductVariantDraft>) null);

    final ProductSync productSync = new ProductSync(getProductSyncOptions());
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
  }
}
