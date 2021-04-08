package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.createCustomObject;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteCustomObject;
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
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customobjects.CustomObject;
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

class ProductSyncWithNestedReferencedCustomObjectsIT {
  private static ProductType productType;
  private static CustomObject<JsonNode> testCustomObject1;
  private static CustomObject<JsonNode> testCustomObject2;

  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<UpdateAction<Product>> actions;

  private static final String ATTRIBUTE_NAME_FIELD = "name";
  private static final String ATTRIBUTE_VALUE_FIELD = "value";
  private static final String CUSTOM_OBJECT_REFERENCE_ATTR_NAME = "customObject-reference";
  private static final String CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME = "customObject-reference-set";

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteCustomObjects();

    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    final ProductType nestedProductType =
        createProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final AttributeDefinitionDraft nestedAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                NestedAttributeType.of(nestedProductType),
                "nestedAttribute",
                ofEnglish("nestedAttribute"),
                false)
            .searchable(false)
            .build();

    final AttributeDefinitionDraft setOfNestedAttributeDef =
        AttributeDefinitionDraftBuilder.of(
                SetAttributeType.of(NestedAttributeType.of(nestedProductType)),
                "setOfNestedAttribute",
                ofEnglish("setOfNestedAttribute"),
                false)
            .searchable(false)
            .build();

    final ProductTypeUpdateCommand productTypeUpdateCommand =
        ProductTypeUpdateCommand.of(
            productType,
            asList(
                AddAttributeDefinition.of(nestedAttributeDef),
                AddAttributeDefinition.of(setOfNestedAttributeDef)));

    CTP_TARGET_CLIENT.execute(productTypeUpdateCommand).toCompletableFuture().join();

    final ObjectNode customObject1Value =
        JsonNodeFactory.instance.objectNode().put("name", "value1");

    testCustomObject1 =
        createCustomObject(CTP_TARGET_CLIENT, "key1", "container1", customObject1Value);

    final ObjectNode customObject2Value =
        JsonNodeFactory.instance.objectNode().put("name", "value2");

    testCustomObject2 =
        createCustomObject(CTP_TARGET_CLIENT, "key2", "container2", customObject2Value);
  }

  private static void deleteCustomObjects() {
    deleteCustomObject(CTP_TARGET_CLIENT, "key1", "container1");
    deleteCustomObject(CTP_TARGET_CLIENT, "key2", "container2");
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
        .beforeUpdateCallback((actions1, productDraft, product1) -> collectActions(actions1))
        .warningCallback(warningCallback)
        .build();
  }

  private void collectErrors(final String errorMessage, final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  private List<UpdateAction<Product>> collectActions(
      @Nonnull final List<UpdateAction<Product>> actions) {
    this.actions.addAll(actions);
    return actions;
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteCustomObjects();
  }

  @Test
  void
      sync_withNestedCustomObjectReferenceAsAttribute_shouldCreateProductReferencingExistingCustomObject() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObject.referenceTypeId()));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCustomObjectReference))
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
            .execute(ProductByKeyGet.of(productDraftWithCustomObjectReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeReferenceValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(nestedAttributeNameField.asText())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_ATTR_NAME);
              assertThat(nestedAttributeReferenceValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(CustomObject.referenceTypeId());
              assertThat(nestedAttributeReferenceValueField.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(testCustomObject1.getId());
            });
  }

  @Test
  void sync_withSameNestedCustomObjectReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(testCustomObject1.getId(), CustomObject.referenceTypeId()));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .execute(ProductCreateCommand.of(productDraftWithCustomObjectReference))
        .toCompletableFuture()
        .join();

    final ObjectNode newNestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObject.referenceTypeId()));

    final AttributeDraft newCustomObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(newNestedAttributeValue));

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newCustomObjectReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(newProductDraftWithCustomObjectReference))
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
            .execute(ProductByKeyGet.of(productDraftWithCustomObjectReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);
              assertThat(nestedAttributeNameField.asText())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_ATTR_NAME);
              assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(CustomObject.referenceTypeId());
              assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(testCustomObject1.getId());
            });
  }

  @Test
  void
      sync_withChangedNestedCustomObjectReferenceAsAttribute_shouldUpdateProductReferencingExistingCustomObject() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(testCustomObject1.getId(), CustomObject.referenceTypeId()));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .execute(ProductCreateCommand.of(productDraftWithCustomObjectReference))
        .toCompletableFuture()
        .join();

    final ObjectNode newNestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject2.getContainer(), testCustomObject2.getKey()),
                CustomObject.referenceTypeId()));

    final AttributeDraft newCustomObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(newNestedAttributeValue));

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newCustomObjectReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(newProductDraftWithCustomObjectReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final ObjectNode expectedNestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(testCustomObject2.getId(), CustomObject.referenceTypeId()));

    final AttributeDraft expectedCustomObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(expectedNestedAttributeValue));
    assertThat(actions)
        .containsExactly(SetAttribute.of(1, expectedCustomObjectReferenceAttribute, true));

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCustomObjectReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(nestedAttributeNameField.asText())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_ATTR_NAME);
              assertThat(nestedAttributeValueField.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(CustomObject.referenceTypeId());
              assertThat(nestedAttributeValueField.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(testCustomObject2.getId());
            });
  }

  @Test
  void sync_withNonExistingNestedCustomObjectReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                "non-existing-container|non-existing-key", CustomObject.referenceTypeId()));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCustomObjectReference))
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
                      "The value '{\"typeId\":\"key-value-document\","
                          + "\"id\":\"non-existing-container|non-existing-key\"}' "
                          + "is not valid for field 'nestedAttribute.customObject-reference'");
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            message ->
                assertThat(message)
                    .contains(
                        "The value '{\"typeId\":\"key-value-document\","
                            + "\"id\":\"non-existing-container|non-existing-key\"}' "
                            + "is not valid for field 'nestedAttribute.customObject-reference'"));
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withNestedCustomObjectReferenceSetAsAttribute_shouldCreateProductReferencingExistingCustomObjects() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObject.referenceTypeId()),
            createReferenceObject(
                format("%s|%s", testCustomObject2.getContainer(), testCustomObject2.getKey()),
                CustomObject.referenceTypeId()));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCustomObjectReference))
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
            .execute(ProductByKeyGet.of(productDraftWithCustomObjectReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode nestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode nestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(nestedAttributeNameField.asText())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME);
              assertThat(nestedAttributeValueField).isInstanceOf(ArrayNode.class);
              final ArrayNode referenceSet = (ArrayNode) nestedAttributeValueField;
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(CustomObject.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testCustomObject1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(CustomObject.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testCustomObject2.getId());
                      });
            });
  }

  @Test
  void
      sync_withNestedCustomObjectReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObject.referenceTypeId()),
            createReferenceObject(
                "non-existing-container|non-existing-key", CustomObject.referenceTypeId()));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("nestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCustomObjectReference))
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
                      "The value '{\"typeId\":\"key-value-document\","
                          + "\"id\":\"non-existing-container|non-existing-key\"}' "
                          + "is not valid for field 'nestedAttribute.customObject-reference-set'");
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .hasOnlyOneElementSatisfying(
            message ->
                assertThat(message)
                    .contains(
                        "The value '{\"typeId\":\"key-value-document\","
                            + "\"id\":\"non-existing-container|non-existing-key\"}' "
                            + "is not valid for field 'nestedAttribute.customObject-reference-set'"));
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingCustomObjects() {
    // preparation
    final ArrayNode nestedAttributeValue =
        createArrayNode(
            createNestedAttributeValueSetOfReferences(
                CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME,
                createReferenceObject(
                    format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                    CustomObject.referenceTypeId()),
                createReferenceObject(
                    format("%s|%s", testCustomObject2.getContainer(), testCustomObject2.getKey()),
                    CustomObject.referenceTypeId())));

    final AttributeDraft customObjectReferenceAttribute =
        AttributeDraft.of("setOfNestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of(
                productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCustomObjectReference))
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
            .execute(ProductByKeyGet.of(productDraftWithCustomObjectReference.getKey()))
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final JsonNode setOfNestedAttributeNameField =
                  attribute.getValueAsJsonNode().get(0).get(0).get(ATTRIBUTE_NAME_FIELD);
              final JsonNode setOfNestedAttributeValueField =
                  attribute.getValueAsJsonNode().get(0).get(0).get(ATTRIBUTE_VALUE_FIELD);

              assertThat(setOfNestedAttributeNameField.asText())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME);
              assertThat(setOfNestedAttributeValueField).isInstanceOf(ArrayNode.class);
              final ArrayNode referenceSet = (ArrayNode) setOfNestedAttributeValueField;
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(CustomObject.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testCustomObject1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
                            .isEqualTo(CustomObject.referenceTypeId());
                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
                            .isEqualTo(testCustomObject2.getId());
                      });
            });
  }
}
