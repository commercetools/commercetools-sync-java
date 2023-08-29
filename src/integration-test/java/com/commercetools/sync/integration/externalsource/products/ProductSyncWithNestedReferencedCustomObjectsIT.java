package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.createCustomObject;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteCustomObject;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectReference;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.*;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithNestedReferencedCustomObjectsIT {
  private static ProductType productType;
  private static CustomObject testCustomObject1;
  private static CustomObject testCustomObject2;

  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> actions;

  private static final String CUSTOM_OBJECT_REFERENCE_ATTR_NAME = "customObject-reference";
  private static final String CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME = "customObject-reference-set";

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    deleteCustomObjects();

    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    final ProductType nestedProductType =
        ensureProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final AttributeDefinitionDraft nestedAttributeDef =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(
                            productTypeReferenceBuilder ->
                                productTypeReferenceBuilder.id(nestedProductType.getId())))
            .name("nestedAttribute")
            .label(ofEnglish("nestedAttribute"))
            .isRequired(false)
            .isSearchable(false)
            .build();

    final AttributeDefinitionDraft setOfNestedAttributeDef =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .setBuilder()
                        .elementType(
                            attributeTypeBuilder1 ->
                                attributeTypeBuilder1
                                    .nestedBuilder()
                                    .typeReference(
                                        productTypeReferenceBuilder ->
                                            productTypeReferenceBuilder.id(
                                                nestedProductType.getId()))))
            .name("setOfNestedAttribute")
            .label(ofEnglish("setOfNestedAttribute"))
            .isRequired(false)
            .isSearchable(false)
            .build();

    final List<ProductTypeUpdateAction> productTypeUpdateActions =
        List.of(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(nestedAttributeDef)
                .build(),
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(setOfNestedAttributeDef)
                .build());

    CTP_TARGET_CLIENT
        .productTypes()
        .update(productType, productTypeUpdateActions)
        .executeBlocking();

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

  private List<ProductUpdateAction> collectActions(
      @Nonnull final List<ProductUpdateAction> actions) {
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
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
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
            .products()
            .withKey(productDraftWithCustomObjectReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute customObjectReferenceAttr = value.get(0);
              assertThat(customObjectReferenceAttr.getName())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_ATTR_NAME);
              assertThat(customObjectReferenceAttr.getValue())
                  .isInstanceOf(CustomObjectReference.class);
              final CustomObjectReference ref =
                  (CustomObjectReference) customObjectReferenceAttr.getValue();
              assertThat(ref.getId()).isEqualTo(testCustomObject1.getId());
            });
  }

  @Test
  void sync_withSameNestedCustomObjectReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                testCustomObject1.getId(), CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().post(productDraftWithCustomObjectReference).executeBlocking();

    final Attribute newNestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute newCustomObjectReferenceAttribute =
        AttributeBuilder.of()
            .name("nestedAttribute")
            .value(List.of(newNestedAttributeValue))
            .build();

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newCustomObjectReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithCustomObjectReference =
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
            .products()
            .withKey(productDraftWithCustomObjectReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute customObjectReferenceAttr = value.get(0);
              assertThat(customObjectReferenceAttr.getName())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_ATTR_NAME);
              assertThat(customObjectReferenceAttr.getValue())
                  .isInstanceOf(CustomObjectReference.class);
              final CustomObjectReference ref =
                  (CustomObjectReference) customObjectReferenceAttr.getValue();
              assertThat(ref.getId()).isEqualTo(testCustomObject1.getId());
            });
  }

  @Test
  void
      sync_withChangedNestedCustomObjectReferenceAsAttribute_shouldUpdateProductReferencingExistingCustomObject() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                testCustomObject1.getId(), CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().post(productDraftWithCustomObjectReference).executeBlocking();

    final Attribute newNestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject2.getContainer(), testCustomObject2.getKey()),
                CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute newCustomObjectReferenceAttribute =
        AttributeBuilder.of()
            .name("nestedAttribute")
            .value(List.of(newNestedAttributeValue))
            .build();

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newCustomObjectReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithCustomObjectReference =
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
            .sync(singletonList(newProductDraftWithCustomObjectReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Attribute expectedNestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                testCustomObject2.getId(), CustomObjectReference.KEY_VALUE_DOCUMENT));

    assertThat(actions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(1L)
                .name("nestedAttribute")
                .value(List.of(expectedNestedAttributeValue))
                .staged(true)
                .build());

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithCustomObjectReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute customObjectReferenceAttr = value.get(0);
              assertThat(customObjectReferenceAttr.getName())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_ATTR_NAME);
              assertThat(customObjectReferenceAttr.getValue())
                  .isInstanceOf(CustomObjectReference.class);
              final CustomObjectReference ref =
                  (CustomObjectReference) customObjectReferenceAttr.getValue();
              assertThat(ref.getId()).isEqualTo(testCustomObject2.getId());
            });
  }

  @Test
  void sync_withNonExistingNestedCustomObjectReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            CUSTOM_OBJECT_REFERENCE_ATTR_NAME,
            createReferenceObject(
                "non-existing-container|non-existing-key",
                CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
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
            .sync(singletonList(productDraftWithCustomObjectReference))
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
                      "The value '{\\\"id\\\":\\\"non-existing-container|non-existing-key\\\",\\\"typeId\\\":\\\"key-value-document\\\"}' "
                          + "is not valid for field 'nestedAttribute.customObject-reference'.");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"non-existing-container|non-existing-key\\\",\\\"typeId\\\":\\\"key-value-document\\\"}' "
                + "is not valid for field 'nestedAttribute.customObject-reference'.");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withNestedCustomObjectReferenceSetAsAttribute_shouldCreateProductReferencingExistingCustomObjects() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObjectReference.KEY_VALUE_DOCUMENT),
            createReferenceObject(
                format("%s|%s", testCustomObject2.getContainer(), testCustomObject2.getKey()),
                CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
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
            .products()
            .withKey(productDraftWithCustomObjectReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> nested = AttributeAccessor.asNested(attribute);
              assertThat(nested).isNotNull();
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME);
              assertThat(nestedAttribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceList = (List<Reference>) nestedAttribute.getValue();
              assertThat(referenceList)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId())
                            .isEqualTo(ReferenceTypeId.KEY_VALUE_DOCUMENT);
                        assertThat(reference.getId()).isEqualTo(testCustomObject1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId())
                            .isEqualTo(ReferenceTypeId.KEY_VALUE_DOCUMENT);
                        assertThat(reference.getId()).isEqualTo(testCustomObject2.getId());
                      });
            });
  }

  @Test
  void
      sync_withNestedCustomObjectReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME,
            createReferenceObject(
                format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                CustomObjectReference.KEY_VALUE_DOCUMENT),
            createReferenceObject(
                "non-existing-container|non-existing-key",
                CustomObjectReference.KEY_VALUE_DOCUMENT));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
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
            .sync(singletonList(productDraftWithCustomObjectReference))
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
                      "The value '{\\\"id\\\":\\\"non-existing-container|non-existing-key\\\",\\\"typeId\\\":\\\"key-value-document\\\"}' "
                          + "is not valid for field 'nestedAttribute.customObject-reference-set'.");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"non-existing-container|non-existing-key\\\",\\\"typeId\\\":\\\"key-value-document\\\"}' "
                + "is not valid for field 'nestedAttribute.customObject-reference-set'.");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingCustomObjects() {
    // preparation
    final List<Attribute> nestedAttributeValue =
        List.of(
            createNestedAttributeValueSetOfReferences(
                CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME,
                createReferenceObject(
                    format("%s|%s", testCustomObject1.getContainer(), testCustomObject1.getKey()),
                    CustomObjectReference.KEY_VALUE_DOCUMENT),
                createReferenceObject(
                    format("%s|%s", testCustomObject2.getContainer(), testCustomObject2.getKey()),
                    CustomObjectReference.KEY_VALUE_DOCUMENT)));

    final Attribute customObjectReferenceAttribute =
        AttributeBuilder.of()
            .name("setOfNestedAttribute")
            .value(List.of(nestedAttributeValue))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(customObjectReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCustomObjectReference =
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
            .products()
            .withKey(productDraftWithCustomObjectReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdCustomObjectReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(customObjectReferenceAttribute.getName());

    assertThat(createdCustomObjectReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<List<Attribute>> value = AttributeAccessor.asSetNested(attribute);
              assertThat(value).isNotNull();
              final Attribute nestedAttribute = value.get(0).get(0);
              assertThat(nestedAttribute.getName())
                  .isEqualTo(CUSTOM_OBJECT_REFERENCE_SET_ATTR_NAME);
              assertThat(nestedAttribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceList = (List<Reference>) nestedAttribute.getValue();
              assertThat(referenceList)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId())
                            .isEqualTo(ReferenceTypeId.KEY_VALUE_DOCUMENT);
                        assertThat(reference.getId()).isEqualTo(testCustomObject1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId())
                            .isEqualTo(ReferenceTypeId.KEY_VALUE_DOCUMENT);
                        assertThat(reference.getId()).isEqualTo(testCustomObject2.getId());
                      });
            });
  }
}
