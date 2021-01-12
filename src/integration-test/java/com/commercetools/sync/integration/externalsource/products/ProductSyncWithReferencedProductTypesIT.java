package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithReferencedProductTypesIT {
  private static ProductType productType;
  private static ProductType productType2;

  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<UpdateAction<Product>> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    productType2 = createProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    deleteAllCategories(CTP_TARGET_CLIENT);
    syncOptions = buildSyncOptions();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    actions = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<Product>> warningCallback =
        (syncException, productDraft, product) ->
            warningCallBackMessages.add(syncException.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (syncException, draft, product, updateActions) ->
                collectErrors(syncException.getMessage(), syncException))
        .beforeUpdateCallback(this::collectActions)
        .warningCallback(warningCallback)
        .build();
  }

  private void collectErrors(
      @Nullable final String errorMessage, @Nullable final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<UpdateAction<Product>> collectActions(
      @Nonnull final List<UpdateAction<Product>> actions,
      @Nonnull final ProductDraft productDraft,
      @Nonnull final Product product) {
    this.actions.addAll(actions);
    return actions;
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void
      sync_withProductTypeReferenceAsAttribute_shouldCreateProductReferencingExistingProductType() {
    // preparation
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference",
            Reference.of(ProductType.referenceTypeId(), productType.getKey()));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithProductTypeReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductTypeReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType.getId());
            });
  }

  @Test
  void sync_withSameProductTypeReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference", Reference.of(ProductType.referenceTypeId(), productType));
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .execute(ProductCreateCommand.of(productDraftWithProductTypeReference))
        .toCompletableFuture()
        .join();

    final AttributeDraft newProductReferenceAttribute =
        AttributeDraft.of(
            "productType-reference",
            Reference.of(ProductType.referenceTypeId(), productType.getKey()));
    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(newProductDraftWithProductReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductTypeReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType.getId());
            });
  }

  @Test
  void
      sync_withChangedProductTypeReferenceAsAttribute_shouldUpdateProductReferencingExistingProductType() {
    // preparation
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference", Reference.of(ProductType.referenceTypeId(), productType));
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .execute(ProductCreateCommand.of(productDraftWithProductTypeReference))
        .toCompletableFuture()
        .join();

    final AttributeDraft newProductTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference",
            Reference.of(ProductType.referenceTypeId(), productType2.getKey()));
    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductTypeReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(newProductDraftWithProductTypeReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    final AttributeDraft expectedAttribute =
        AttributeDraft.of(
            "productType-reference",
            Reference.of(ProductType.referenceTypeId(), productType2.getId()));
    assertThat(actions).containsExactly(SetAttributeInAllVariants.of(expectedAttribute, true));

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductTypeReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType2.getId());
            });
  }

  @Test
  void sync_withNonExistingProductTypeReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference", Reference.of(ProductType.referenceTypeId(), "nonExistingKey"));
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithProductTypeReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            error -> {
              assertThat(error).hasCauseExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) error.getCause();
              assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                          + "is not valid for field 'productType-reference'");
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            message ->
                assertThat(message)
                    .contains(
                        "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                            + "is not valid for field 'productType-reference'"));
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withProductTypeReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
    // preparation
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference",
            Reference.of(ProductType.referenceTypeId(), productType.getKey()));

    final HashSet<Reference<ProductType>> references = new HashSet<>();
    references.add(Reference.of(ProductType.referenceTypeId(), productType.getKey()));
    references.add(Reference.of(ProductType.referenceTypeId(), productType2.getKey()));

    final AttributeDraft productTypeReferenceSetAttribute =
        AttributeDraft.of("productType-reference-set", references);

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute, productTypeReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithProductTypeReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductTypeReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType.getId());
            });

    final Optional<Attribute> createdProductTypeReferenceSetAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceSetAttribute.getName());

    assertThat(createdProductTypeReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValueAsJsonNode()).isInstanceOf(ArrayNode.class);
              final ArrayNode referenceSet = (ArrayNode) attribute.getValueAsJsonNode();
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(ProductType.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(productType.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(ProductType.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(productType2.getId());
                      });
            });
  }

  @Test
  void
      sync_withProductTypeReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of(
            "productType-reference",
            Reference.of(ProductType.referenceTypeId(), productType.getKey()));

    final HashSet<Reference<ProductType>> references = new HashSet<>();
    references.add(Reference.of(ProductType.referenceTypeId(), "nonExistingKey"));
    references.add(Reference.of(ProductType.referenceTypeId(), productType2.getKey()));

    final AttributeDraft productTypeReferenceSetAttribute =
        AttributeDraft.of("productType-reference-set", references);

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute, productTypeReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithProductTypeReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            error -> {
              assertThat(error).hasCauseExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) error.getCause();
              assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                          + "is not valid for field 'productType-reference-set'");
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            message ->
                assertThat(message)
                    .contains(
                        "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                            + "is not valid for field 'productType-reference-set'"));
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }
}
