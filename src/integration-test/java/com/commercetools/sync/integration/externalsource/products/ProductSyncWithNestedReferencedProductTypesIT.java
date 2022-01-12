package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.externalsource.products.ProductSyncWithNestedReferencedProductsIT.createArrayNode;
import static com.commercetools.sync.integration.externalsource.products.ProductSyncWithNestedReferencedProductsIT.createNestedAttributeValueReferences;
import static com.commercetools.sync.integration.externalsource.products.ProductSyncWithNestedReferencedProductsIT.createNestedAttributeValueSetOfReferences;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.products.attributes.SetAttributeType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.commands.updateactions.AddAttributeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithNestedReferencedProductTypesIT {
  private static ProductType testProductType1;
  private static ProductType testProductType2;

  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<UpdateAction<Product>> actions;

  private static final String ATTRIBUTE_NAME_FIELD = "name";
  private static final String ATTRIBUTE_VALUE_FIELD = "value";

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    testProductType1 = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    testProductType2 =
        createProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final AttributeDefinitionDraft nestedAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(testProductType2),
                "nestedAttribute",
                ofEnglish("nestedAttribute"),
                false)
            .searchable(false)
            .build();

    final AttributeDefinitionDraft setOfNestedAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(NestedAttributeType.of(testProductType2)),
                "setOfNestedAttribute",
                ofEnglish("setOfNestedAttribute"),
                false)
            .searchable(false)
            .build();

    final ProductTypeUpdateCommand productTypeUpdateCommand =
        ProductTypeUpdateCommand.of(
            testProductType1,
            asList(
                AddAttributeDefinition.of(nestedAttributeDef),
                AddAttributeDefinition.of(setOfNestedAttributeDef)));

    CTP_TARGET_CLIENT.execute(productTypeUpdateCommand).toCompletableFuture().join();
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
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
        .beforeUpdateCallback(this::collectActions)
        .warningCallback(warningCallback)
        .build();
  }

  private void collectErrors(final String errorMessage, final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<UpdateAction<Product>> collectActions(
      @Nonnull final List<UpdateAction<Product>> actions,
      @Nonnull final ProductDraft productDraft,
      @Nonnull final ProductProjection product) {
    this.actions.addAll(actions);
    return actions;
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void
      sync_withNestedProductTypeReferenceAsAttribute_shouldCreateProductReferencingExistingProductType() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getKey(), ProductType.referenceTypeId()));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeReferenceValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(nestedAttributeNameField.asText()).isEqualTo("productType-reference");
              assertThat(nestedAttributeReferenceValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(nestedAttributeReferenceValueField.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(testProductType1.getId());
            });
  }

  @Test
  void sync_withSameNestedProductTypeReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getId(), ProductType.referenceTypeId()));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .execute(ProductCreateCommand.of(productDraftWithProductTypeReference))
        .toCompletableFuture()
        .join();

    final ObjectNode newNestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getKey(), ProductType.referenceTypeId()));

    final AttributeDraft newProductReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(newNestedAttributeValue));

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductReference =
        ProductDraftBuilder.of(
                testProductType1,
                ofEnglish("productName"),
                ofEnglish("productSlug"),
                newMasterVariant)
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);
              assertThat(nestedAttributeNameField.asText()).isEqualTo("productType-reference");
              assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(testProductType1.getId());
            });
  }

  @Test
  void
      sync_withChangedNestedProductTypeReferenceAsAttribute_shouldUpdateProductReferencingExistingProductType() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getId(), ProductType.referenceTypeId()));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .execute(ProductCreateCommand.of(productDraftWithProductTypeReference))
        .toCompletableFuture()
        .join();

    final ObjectNode newNestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType2.getKey(), ProductType.referenceTypeId()));

    final AttributeDraft newProductReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(newNestedAttributeValue));

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1,
                ofEnglish("productName"),
                ofEnglish("productSlug"),
                newMasterVariant)
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

    final ObjectNode expectedNestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType2.getId(), ProductType.referenceTypeId()));

    final AttributeDraft expectedProductTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(expectedNestedAttributeValue));
    assertThat(actions)
        .containsExactly(SetAttribute.of(1, expectedProductTypeReferenceAttribute, true));

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductTypeReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(nestedAttributeNameField.asText()).isEqualTo("productType-reference");
              assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductType.referenceTypeId());
              assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(testProductType2.getId());
            });
  }

  @Test
  void sync_withNonExistingNestedProductTypeReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject("nonExistingKey", ProductType.referenceTypeId()));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
              assertThat(error).hasCauseExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) error.getCause();
              assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                          + "is not valid for field 'nestedAttribute.productType-reference'");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                + "is not valid for field 'nestedAttribute.productType-reference'");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withNestedProductTypeReferenceSetAsAttribute_shouldCreateProductReferencingExistingProductType() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "productType-reference-set",
            createReferenceObject(testProductType1.getKey(), ProductType.referenceTypeId()),
            createReferenceObject(testProductType2.getKey(), ProductType.referenceTypeId()));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(nestedAttributeNameField.asText()).isEqualTo("productType-reference-set");
              assertThat(nestedAttributeValueField).isInstanceOf(ArrayNode.class);
              final ArrayNode referenceSet = (ArrayNode) nestedAttributeValueField;
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(ProductType.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testProductType1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(ProductType.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testProductType2.getId());
                      });
            });
  }

  @Test
  void
      sync_withNestedProductTypeReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "productType-reference-set",
            createReferenceObject(testProductType1.getKey(), ProductType.referenceTypeId()),
            createReferenceObject("nonExistingKey", ProductType.referenceTypeId()));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
              assertThat(error).hasCauseExactlyInstanceOf(ErrorResponseException.class);
              final ErrorResponseException errorResponseException =
                  (ErrorResponseException) error.getCause();
              assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                          + "is not valid for field 'nestedAttribute.productType-reference-set'");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\"typeId\":\"product-type\",\"id\":\"nonExistingKey\"}' "
                + "is not valid for field 'nestedAttribute.productType-reference-set'");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProductType() {
    // preparation
    final ArrayNode nestedAttributeValue =
        createArrayNode(
            createNestedAttributeValueSetOfReferences(
                "productType-reference-set",
                createReferenceObject(testProductType1.getKey(), ProductType.referenceTypeId()),
                createReferenceObject(testProductType2.getKey(), ProductType.referenceTypeId())));

    final AttributeDraft productTypeReferenceAttribute =
        AttributeDraft.of("setOfNestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of(
                testProductType1, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode setOfNestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode setOfNestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(setOfNestedAttributeNameField.asText())
                  .isEqualTo("productType-reference-set");
              assertThat(setOfNestedAttributeValueField).isInstanceOf(ArrayNode.class);
              final ArrayNode referenceSet = (ArrayNode) setOfNestedAttributeValueField;
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(ProductType.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testProductType1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(ProductType.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testProductType2.getId());
                      });
            });
  }
}
