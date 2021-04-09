package com.commercetools.sync.integration.ctpprojectsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJaneDoe;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_NO_ATTRIBUTES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_NO_KEY_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_FOR_VARIANT_ATTRIBUTES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.utils.ProductTransformUtils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.taxcategories.TaxCategory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductReferenceResolverIT {
  private static ProductType productTypeSource;
  private static ProductType noKeyProductTypeSource;
  private static ProductType productTypeSourceWithReferenceTypeVariantAttribute;

  private static TaxCategory oldTaxCategory;
  private static State oldProductState;
  private static Customer oldCustomer;
  private static ProductProjectionQuery productQuery;
  private static List<Reference<Category>> categoryReferencesWithIds;
  private ProductSync productSync;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  /**
   * Delete all product related test data from target and source projects. Then creates custom types
   * for both CTP projects categories.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);

    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_SOURCE_CLIENT);

    createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    categoryReferencesWithIds =
        getReferencesWithIds(createCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2)));

    createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    createProductType(PRODUCT_TYPE_NO_KEY_RESOURCE_PATH, CTP_TARGET_CLIENT);
    createProductType(
        PRODUCT_TYPE_WITH_REFERENCES_FOR_VARIANT_ATTRIBUTES_RESOURCE_PATH, CTP_TARGET_CLIENT);
    productTypeSourceWithReferenceTypeVariantAttribute =
        createProductType(
            PRODUCT_TYPE_WITH_REFERENCES_FOR_VARIANT_ATTRIBUTES_RESOURCE_PATH, CTP_SOURCE_CLIENT);

    productTypeSource = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);
    noKeyProductTypeSource =
        createProductType(PRODUCT_TYPE_NO_KEY_RESOURCE_PATH, CTP_SOURCE_CLIENT);

    oldTaxCategory = createTaxCategory(CTP_SOURCE_CLIENT);
    oldProductState = createState(CTP_SOURCE_CLIENT, StateType.PRODUCT_STATE);
    createTaxCategory(CTP_TARGET_CLIENT);
    createState(CTP_TARGET_CLIENT, StateType.PRODUCT_STATE);
    oldCustomer = createSampleCustomerJaneDoe(CTP_SOURCE_CLIENT);
    createSampleCustomerJaneDoe(CTP_TARGET_CLIENT);
    productQuery = ProductProjectionQuery.ofStaged();
  }

  /**
   * Deletes Products and Types from target CTP projects, then it populates target CTP project with
   * product test data.
   */
  @BeforeEach
  void setupTest() {
    deleteAllProducts(CTP_TARGET_CLIENT);
    deleteAllProducts(CTP_SOURCE_CLIENT);

    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    productSync = new ProductSync(syncOptions);
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteProductSyncTestData(CTP_SOURCE_CLIENT);
  }

  @Test
  void sync_withNewProductWithExistingCategoryAndProductTypeReferences_ShouldCreateProduct() {
    // preparation
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            productTypeSource.toReference(),
            oldTaxCategory.toReference(),
            oldProductState.toReference(),
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));
    CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withNewProductWithNoProductTypeKey_ShouldFailCreatingTheProduct() {
    // preparation
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            noKeyProductTypeSource.toReference(),
            oldTaxCategory.toReference(),
            oldProductState.toReference(),
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));
    CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts =
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    // test
    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages)
        .containsExactly(
            format(
                "Failed to process the ProductDraft with"
                    + " key:'%s'. Reason: "
                    + ReferenceResolutionException.class.getCanonicalName()
                    + ": "
                    + "Failed to resolve 'product-type' resource identifier on ProductDraft with "
                    + "key:'%s'. Reason: %s",
                productDraft.getKey(),
                productDraft.getKey(),
                BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withNewProductWithStateReferenceVariantAttribute_ShouldCreateProduct() {

    createProductWithReferenceVariantAttribute("state", oldProductState.getId(), "state-reference");

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts = // mapToProductDrafts(products);
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withNewProductWithStateReferenceSetVariantAttribute_ShouldCreateProduct() {
    final List<String> stateIdList = new ArrayList<>();
    stateIdList.add(oldProductState.getId());

    createProductWithReferenceSetVariantAttribute("state", stateIdList, "state-reference-set");

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts =
        // mapToProductDrafts(products);
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withNewProductWithCustomerReferenceVariantAttribute_ShouldCreateProduct() {

    createProductWithReferenceVariantAttribute(
        "customer", oldCustomer.getId(), "customer-reference");

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts = // mapToProductDrafts(products);
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withNewProductWithCustomerReferenceSetVariantAttribute_ShouldCreateProduct() {
    final List<String> customerIdList = new ArrayList<>();
    customerIdList.add(oldCustomer.getId());

    createProductWithReferenceSetVariantAttribute(
        "customer", customerIdList, "customer-reference-set");

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT.execute(productQuery).toCompletableFuture().join().getResults();

    final List<ProductDraft> productDrafts = // mapToProductDrafts(products);
        ProductTransformUtils.toProductDrafts(CTP_SOURCE_CLIENT, referenceIdToKeyCache, products)
            .join();

    final ProductSyncStatistics syncStatistics =
        productSync.sync(productDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  private void createProductWithReferenceVariantAttribute(
      @Nonnull final String typeId, @Nonnull final String id, @Nonnull final String attributeName) {

    ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_NO_ATTRIBUTES_RESOURCE_PATH,
            productTypeSourceWithReferenceTypeVariantAttribute.toReference(),
            oldTaxCategory.toReference(),
            oldProductState.toReference(),
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));
    ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
    attributeValue.put("typeId", typeId);
    attributeValue.put("id", id);
    AttributeDraft attributeDraft = AttributeDraft.of(attributeName, attributeValue);

    ProductVariantDraft productVariantDraft = productDraft.getMasterVariant();
    productVariantDraft =
        ProductVariantDraftBuilder.of(productVariantDraft).plusAttribute(attributeDraft).build();

    productDraft = ProductDraftBuilder.of(productDraft).masterVariant(productVariantDraft).build();
    CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
  }

  private void createProductWithReferenceSetVariantAttribute(
      @Nonnull final String typeId,
      @Nonnull final List<String> idList,
      @Nonnull final String attributeName) {

    ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_NO_ATTRIBUTES_RESOURCE_PATH,
            productTypeSourceWithReferenceTypeVariantAttribute.toReference(),
            oldTaxCategory.toReference(),
            oldProductState.toReference(),
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

    Set<ObjectNode> attributeValueSet = new HashSet<>();

    idList.forEach(
        id -> {
          ObjectNode attributeValue = JsonNodeFactory.instance.objectNode();
          attributeValue.put("typeId", typeId);
          attributeValue.put("id", id);
          attributeValueSet.add(attributeValue);
        });

    AttributeDraft attributeDraft = AttributeDraft.of(attributeName, attributeValueSet);

    ProductVariantDraft productVariantDraft = productDraft.getMasterVariant();
    productVariantDraft =
        ProductVariantDraftBuilder.of(productVariantDraft).plusAttribute(attributeDraft).build();

    productDraft = ProductDraftBuilder.of(productDraft).masterVariant(productVariantDraft).build();
    CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
  }
}
