package com.commercetools.sync.integration.sdk2.ctpprojectsource.products;

import static com.commercetools.sync.integration.sdk2.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.CustomerITUtils.ensureSampleCustomerJaneDoe;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.StateITUtils.ensureState;
import static com.commercetools.sync.integration.sdk2.commons.utils.TaxCategoryITUtils.ensureTaxCategory;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.commons.helpers.BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.customer.CustomerReferenceBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateReferenceBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.sdk2.products.utils.ProductTransformUtils;
import io.vrap.rmf.base.client.ApiHttpResponse;
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
  private static List<CategoryReference> categoryReferencesWithIds;
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
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_SOURCE_CLIENT);

    ensureCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    categoryReferencesWithIds =
        getReferencesWithIds(ensureCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2)));

    ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    //    ensureProductType(PRODUCT_TYPE_NO_KEY_RESOURCE_PATH, CTP_TARGET_CLIENT);
    ensureProductType(
        PRODUCT_TYPE_WITH_REFERENCES_FOR_VARIANT_ATTRIBUTES_RESOURCE_PATH, CTP_TARGET_CLIENT);
    productTypeSourceWithReferenceTypeVariantAttribute =
        ensureProductType(
            PRODUCT_TYPE_WITH_REFERENCES_FOR_VARIANT_ATTRIBUTES_RESOURCE_PATH, CTP_SOURCE_CLIENT);

    productTypeSource = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);
    noKeyProductTypeSource =
        createProductType(PRODUCT_TYPE_NO_KEY_RESOURCE_PATH, CTP_SOURCE_CLIENT);

    oldTaxCategory = ensureTaxCategory(CTP_SOURCE_CLIENT);
    oldProductState = ensureState(CTP_SOURCE_CLIENT, StateTypeEnum.PRODUCT_STATE);
    ensureTaxCategory(CTP_TARGET_CLIENT);
    ensureState(CTP_TARGET_CLIENT, StateTypeEnum.PRODUCT_STATE);
    oldCustomer = ensureSampleCustomerJaneDoe(CTP_SOURCE_CLIENT);
    ensureSampleCustomerJaneDoe(CTP_TARGET_CLIENT);
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
  static void tearDown() {}

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
    CTP_SOURCE_CLIENT.products().create(productDraft).executeBlocking();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

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
    CTP_SOURCE_CLIENT.products().create(productDraft).executeBlocking();

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

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

    createProductWithStateReferenceVariantAttribute(oldProductState.getId());

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

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

    createProductWithStateReferenceSetVariantAttribute(stateIdList);

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

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

    createProductWithCustomerReferenceVariantAttribute(oldCustomer.getId());

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

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

    createProductWithCustomerReferenceSetVariantAttribute(customerIdList, "customer-reference-set");

    final List<ProductProjection> products =
        CTP_SOURCE_CLIENT
            .productProjections()
            .get()
            .withStaged(true)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductProjectionPagedQueryResponse::getResults)
            .join();

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

  private void createProductWithStateReferenceVariantAttribute(@Nonnull final String stateId) {
    final StateReference reference = StateReferenceBuilder.of().id(stateId).build();
    this.createProductWithReferenceVariantAttribute(reference, "state-reference");
  }

  private void createProductWithCustomerReferenceVariantAttribute(
      @Nonnull final String customerId) {
    final CustomerReference reference = CustomerReferenceBuilder.of().id(customerId).build();
    this.createProductWithReferenceVariantAttribute(reference, "customer-reference");
  }

  private void createProductWithReferenceVariantAttribute(
      @Nonnull final Reference reference, @Nonnull final String attributeName) {
    ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_NO_ATTRIBUTES_RESOURCE_PATH,
            productTypeSourceWithReferenceTypeVariantAttribute.toReference(),
            oldTaxCategory.toReference(),
            oldProductState.toReference(),
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

    final Attribute attributeDraft =
        AttributeBuilder.of().name(attributeName).value(reference).build();

    final ProductVariantDraft productVariantDraft = productDraft.getMasterVariant();
    productVariantDraft.getAttributes().add(attributeDraft);

    productDraft = ProductDraftBuilder.of(productDraft).masterVariant(productVariantDraft).build();
    CTP_SOURCE_CLIENT.products().create(productDraft).executeBlocking();
  }

  private void createProductWithCustomerReferenceSetVariantAttribute(
      @Nonnull final List<String> idList, @Nonnull final String attributeName) {
    final Set<CustomerReference> attributeValueSet = new HashSet<>();

    idList.forEach(
        id -> {
          final CustomerReference customerReference = CustomerReferenceBuilder.of().id(id).build();
          attributeValueSet.add(customerReference);
        });

    this.createProductWithReferenceSetVariantAttribute(attributeValueSet, attributeName);
  }

  private void createProductWithStateReferenceSetVariantAttribute(
      @Nonnull final List<String> idList) {
    final Set<StateReference> attributeValueSet = new HashSet<>();

    idList.forEach(
        id -> {
          final StateReference stateReference = StateReferenceBuilder.of().id(id).build();
          attributeValueSet.add(stateReference);
        });
    this.createProductWithReferenceSetVariantAttribute(attributeValueSet, "state-reference-set");
  }

  private void createProductWithReferenceSetVariantAttribute(
      final Set<? extends Reference> references, final String attributeName) {
    ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_NO_ATTRIBUTES_RESOURCE_PATH,
            productTypeSourceWithReferenceTypeVariantAttribute.toReference(),
            oldTaxCategory.toReference(),
            oldProductState.toReference(),
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

    final Attribute attribute = AttributeBuilder.of().name(attributeName).value(references).build();

    ProductVariantDraft productVariantDraft = productDraft.getMasterVariant();
    productVariantDraft.getAttributes().add(attribute);
    productVariantDraft = ProductVariantDraftBuilder.of(productVariantDraft).build();

    productDraft = ProductDraftBuilder.of(productDraft).masterVariant(productVariantDraft).build();
    CTP_SOURCE_CLIENT
        .products()
        .create(productDraft)
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .join();
  }
}
