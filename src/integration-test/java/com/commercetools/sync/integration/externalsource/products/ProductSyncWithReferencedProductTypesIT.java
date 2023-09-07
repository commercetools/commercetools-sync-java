package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createReferenceObjectJson;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.sdk2.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.*;
import java.util.concurrent.CompletionException;
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
  private List<ProductUpdateAction> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    productType2 = ensureProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);
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
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallback =
            (syncException, productDraft, product) ->
                warningCallBackMessages.add(syncException.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (syncException, draft, product, updateActions) ->
                collectErrors(syncException.getMessage(), syncException))
        .beforeUpdateCallback((actions1, productDraft, product1) -> collectActions(actions1))
        .warningCallback(warningCallback)
        .build();
  }

  private void collectErrors(
      @Nullable final String errorMessage, @Nullable final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<ProductUpdateAction> collectActions(
      @Nonnull final List<ProductUpdateAction> actions) {
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
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType.getKey(), ProductTypeReference.PRODUCT_TYPE))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
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
            .products()
            .withKey(productDraftWithProductTypeReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductTypeReference.PRODUCT_TYPE);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType.getId());
            });
  }

  @Test
  void sync_withSameProductTypeReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType.getId(), ProductTypeReference.PRODUCT_TYPE))
            .build();
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().create(productDraftWithProductTypeReference).executeBlocking();

    final Attribute newProductReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType.getKey(), ProductTypeReference.PRODUCT_TYPE))
            .build();
    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(newMasterVariant)
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
            .products()
            .withKey(productDraftWithProductTypeReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductTypeReference.PRODUCT_TYPE);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType.getId());
            });
  }

  @Test
  void
      sync_withChangedProductTypeReferenceAsAttribute_shouldUpdateProductReferencingExistingProductType() {
    // preparation
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType.getId(), ProductTypeReference.PRODUCT_TYPE))
            .build();
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().create(productDraftWithProductTypeReference).executeBlocking();

    final Attribute newProductTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType2.getKey(), ProductTypeReference.PRODUCT_TYPE))
            .build();
    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductTypeReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(newMasterVariant)
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
    final ObjectNode expectedAttribute =
        createReferenceObjectJson(productType2.getId(), ProductTypeReference.PRODUCT_TYPE);
    assertThat(actions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("productType-reference")
                .value(expectedAttribute)
                .staged(true)
                .build());

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithProductTypeReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductTypeReference.PRODUCT_TYPE);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(productType2.getId());
            });
  }

  @Test
  void sync_withNonExistingProductTypeReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject("nonExistingKey", ProductTypeReference.PRODUCT_TYPE))
            .build();
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
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
        .singleElement()
        .matches(
            error -> {
              assertThat(error).hasCauseExactlyInstanceOf(CompletionException.class);
              final CompletionException completionException =
                  (CompletionException) error.getCause();
              assertThat(completionException).hasCauseExactlyInstanceOf(BadRequestException.class);
              final BadRequestException badRequestException =
                  (BadRequestException) completionException.getCause();
              assertThat(badRequestException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"product-type\\\"}' "
                          + "is not valid for field 'productType-reference'");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"product-type\\\"}' "
                + "is not valid for field 'productType-reference'");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withProductTypeReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
    // preparation
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType.getKey(), ProductTypeReference.PRODUCT_TYPE))
            .build();

    final Set<Reference> references =
        Set.of(
            createReferenceObject(productType.getKey(), ProductTypeReference.PRODUCT_TYPE),
            createReferenceObject(productType2.getKey(), ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceSetAttribute =
        AttributeBuilder.of().name("productType-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute, productTypeReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
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
            .products()
            .withKey(productDraftWithProductTypeReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductTypeReference.PRODUCT_TYPE);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
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
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceSet = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT_TYPE);
                        assertThat(reference.getId()).isEqualTo(productType.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT_TYPE);
                        assertThat(reference.getId()).isEqualTo(productType2.getId());
                      });
            });
  }

  @Test
  void
      sync_withProductTypeReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("productType-reference")
            .value(createReferenceObject(productType.getKey(), ProductTypeReference.PRODUCT_TYPE))
            .build();

    final Set<Reference> references =
        Set.of(
            createReferenceObject("nonExistingKey", ProductTypeReference.PRODUCT_TYPE),
            createReferenceObject(productType2.getKey(), ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceSetAttribute =
        AttributeBuilder.of().name("productType-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute, productTypeReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
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
        .singleElement()
        .matches(
            error -> {
              assertThat(error).hasCauseExactlyInstanceOf(CompletionException.class);
              final CompletionException completionException =
                  (CompletionException) error.getCause();
              assertThat(completionException).hasCauseExactlyInstanceOf(BadRequestException.class);
              final BadRequestException badRequestException =
                  (BadRequestException) completionException.getCause();
              assertThat(badRequestException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"product-type\\\"}' "
                          + "is not valid for field 'productType-reference-set'");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"product-type\\\"}' "
                + "is not valid for field 'productType-reference-set'");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }
}
