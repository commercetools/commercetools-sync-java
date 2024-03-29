package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createReferenceObjectJson;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.*;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithNestedReferencedProductsIT {
  private static ProductType productType;

  private ProductSyncOptions syncOptions;
  private Product testProduct1;
  private Product testProduct2;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    final ProductType nestedProductType =
        ensureProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final AttributeDefinitionDraft nestedAttributeDef =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(nestedProductType.toReference()))
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
                                    .typeReference(nestedProductType.toReference())))
            .name("setOfNestedAttribute")
            .label(ofEnglish("setOfNestedAttribute"))
            .isRequired(false)
            .isSearchable(false)
            .build();

    final List<ProductTypeUpdateAction> addAttributeActions =
        List.of(
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(nestedAttributeDef)
                .build(),
            ProductTypeAddAttributeDefinitionActionBuilder.of()
                .attribute(setOfNestedAttributeDef)
                .build());

    CTP_TARGET_CLIENT.productTypes().update(productType, addAttributeActions).executeBlocking();
  }

  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    syncOptions = buildSyncOptions();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo"))
            .slug(ofEnglish("foo-slug"))
            .key("foo")
            .build();

    final ProductDraft productDraft2 =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("foo2"))
            .slug(ofEnglish("foo-slug2"))
            .key("foo2")
            .build();

    testProduct1 =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();
    testProduct2 =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft2)
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
    actions = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (syncException, productDraft, product) ->
                warningCallBackMessages.add(syncException.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (exception, draft, product, updateActions) ->
                collectErrors(exception.getMessage(), exception))
        .beforeUpdateCallback((actions1, productDraft, product1) -> collectActions(actions1))
        .warningCallback(warningCallBack)
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
  }

  @Test
  void sync_withNestedAttributeWithTextAttribute_shouldCreateProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        AttributeBuilder.of().name("text-attr").value("text-attr-value").build();

    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
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
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getValue()).isEqualTo("text-attr-value");
              assertThat(nestedAttribute.getName()).isEqualTo("text-attr");
            });
  }

  @Test
  void sync_withNestedProductReferenceAsAttribute_shouldCreateProductReferencingExistingProduct() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueObjectNodeReferences(
            "product-reference",
            createReferenceObjectJson(testProduct1.getKey(), ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
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
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("product-reference");
              final ProductReference attrValue = (ProductReference) nestedAttribute.getValue();
              assertThat(attrValue.getId()).isEqualTo(testProduct1.getId());
            });
  }

  @Test
  void sync_withNestedProductReferencingItselfAsAttribute_shouldCreateProductReferencingItself() {
    // preparation
    final String sameNewProductKey = "new-product";
    final Attribute nestedProductReferenceAttribute =
        createNestedAttributeValueReferences(
            "product-reference",
            createReferenceObjectJson(sameNewProductKey, ProductReference.PRODUCT));
    final Attribute nestedProductReferenceSetAttribute =
        createNestedAttributeValueSetOfReferences(
            "product-reference-set",
            createReferenceObject(sameNewProductKey, ProductReference.PRODUCT),
            createReferenceObject(testProduct1.getKey(), ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("nestedAttribute")
            .value(List.of(nestedProductReferenceAttribute, nestedProductReferenceSetAttribute))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 1, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0))
        .satisfies(
            productUpdateAction -> {
              assertThat(productUpdateAction).isInstanceOf(ProductSetAttributeAction.class);
              ProductSetAttributeAction castedAction =
                  (ProductSetAttributeAction) productUpdateAction;
              assertThat(castedAction.getName()).isEqualTo("nestedAttribute");
              assertThat(castedAction.getStaged()).isTrue();
              assertThat(castedAction.getValue()).isNotNull();
            });

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute1 = nested.get(0);
              assertThat(nestedAttribute1.getName()).isEqualTo("product-reference");
              final ProductReference attrValue = (ProductReference) nestedAttribute1.getValue();
              assertThat(attrValue.getId()).isEqualTo(createdProduct.getId());
              final Attribute nestedAttribute2 = nested.get(1);
              assertThat(nestedAttribute2.getName()).isEqualTo("product-reference-set");
              final List<Reference> referenceList =
                  AttributeAccessor.asSetReference(nestedAttribute2);
              assertThat(referenceList)
                  .containsExactlyInAnyOrder(
                      ProductReferenceBuilder.of().id(createdProduct.getId()).build(),
                      ProductReferenceBuilder.of().id(testProduct1.getId()).build());
            });
  }

  @Test
  void sync_withSameNestedProductReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final ObjectNode nestedAttributeValue =
        createNestedAttributeValueObjectNodeReferences(
            "product-reference",
            createReferenceObjectJson(testProduct1.getId(), ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("nestedAttribute")
            .value(JsonNodeFactory.instance.arrayNode().add(nestedAttributeValue))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().create(productDraftWithProductReference).executeBlocking();

    final ObjectNode newNestedAttributeValue =
        createNestedAttributeValueObjectNodeReferences(
            "product-reference",
            createReferenceObjectJson(testProduct1.getKey(), ProductReference.PRODUCT));

    final Attribute newProductReferenceAttribute =
        AttributeBuilder.of()
            .name("nestedAttribute")
            .value(JsonNodeFactory.instance.arrayNode().add(newNestedAttributeValue))
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
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("product-reference");
              final ProductReference attrValue = (ProductReference) nestedAttribute.getValue();
              assertThat(attrValue.getId()).isEqualTo(testProduct1.getId());
            });
  }

  @Test
  void
      sync_withChangedNestedProductReferenceAsAttribute_shouldUpdateProductReferencingExistingProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            "product-reference",
            createReferenceObject(testProduct1.getId(), ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().create(productDraftWithProductReference).executeBlocking();

    final Attribute newNestedAttributeValue =
        createNestedAttributeValueReferences(
            "product-reference",
            createReferenceObject(testProduct2.getKey(), ProductReference.PRODUCT));

    final Attribute newProductReferenceAttribute =
        AttributeBuilder.of()
            .name("nestedAttribute")
            .value(List.of(newNestedAttributeValue))
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
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Attribute expectedNestedAttributeValue =
        createNestedAttributeValueReferences(
            "product-reference",
            createReferenceObject(testProduct2.getId(), ProductReference.PRODUCT));
    final JsonNode attributeValueAsJson =
        JsonUtils.toJsonNode(List.of(expectedNestedAttributeValue));

    assertThat(actions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .variantId(1L)
                .name("nestedAttribute")
                .value(attributeValueAsJson)
                .staged(true)
                .build());

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("product-reference");
              final ProductReference attrValue = (ProductReference) nestedAttribute.getValue();
              assertThat(attrValue.getId()).isEqualTo(testProduct2.getId());
            });
  }

  @Test
  void sync_withNonExistingNestedProductReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            "product-reference", createReferenceObject("nonExistingKey", ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final UnresolvedReferencesServiceImpl<WaitingToBeResolvedProducts> unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl<>(syncOptions);
    final Set<WaitingToBeResolvedProducts> waitingToBeResolvedDrafts =
        unresolvedReferencesService
            .fetch(
                Set.of(productDraftWithProductReference.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    assertThat(waitingToBeResolvedDrafts)
        .singleElement()
        .matches(
            waitingToBeResolvedDraft -> {
              assertThat(waitingToBeResolvedDraft.getProductDraft().getKey())
                  .isEqualTo(productDraftWithProductReference.getKey());
              assertThat(waitingToBeResolvedDraft.getMissingReferencedProductKeys())
                  .containsExactly("nonExistingKey");
              return true;
            });
  }

  @Test
  void
      sync_withNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProducts() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "product-reference-set",
            createReferenceObject(testProduct1.getKey(), ProductReference.PRODUCT),
            createReferenceObject(testProduct2.getKey(), ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
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
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("product-reference-set");
              final List<Reference> referenceSet =
                  AttributeAccessor.asSetReference(nestedAttribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(testProduct1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(testProduct2.getId());
                      });
            });
  }

  @Test
  void
      sync_withNestedProductReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "product-reference-set",
            createReferenceObject(testProduct1.getKey(), ProductReference.PRODUCT),
            createReferenceObject("new-product", ProductReference.PRODUCT),
            createReferenceObject("nonExistingKey", ProductReference.PRODUCT));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 1);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();

    final UnresolvedReferencesServiceImpl<WaitingToBeResolvedProducts> unresolvedReferencesService =
        new UnresolvedReferencesServiceImpl<>(syncOptions);
    final Set<WaitingToBeResolvedProducts> waitingToBeResolvedDrafts =
        unresolvedReferencesService
            .fetch(
                Set.of(productDraftWithProductReference.getKey()),
                CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    assertThat(waitingToBeResolvedDrafts)
        .singleElement()
        .matches(
            waitingToBeResolvedDraft -> {
              assertThat(waitingToBeResolvedDraft.getProductDraft().getKey())
                  .isEqualTo(productDraftWithProductReference.getKey());
              assertThat(waitingToBeResolvedDraft.getMissingReferencedProductKeys())
                  .containsExactly("nonExistingKey");
              return true;
            });

    final Product nowExistingProduct =
        CTP_TARGET_CLIENT
            .products()
            .create(
                ProductDraftBuilder.of()
                    .productType(productType.toResourceIdentifier())
                    .name(ofEnglish("nonExistingName"))
                    .slug(ofEnglish("nonExistingSlug"))
                    .key("nonExistingKey")
                    .build())
            .executeBlocking()
            .getBody();

    syncOptions = buildSyncOptions();
    final ProductSync productSync2ndRun = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics2ndRun =
        productSync2ndRun
            .sync(singletonList(productDraftWithProductReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics2ndRun).hasValues(1, 1, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isNotEmpty();

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("product-reference-set");
              final List<Reference> referenceList =
                  AttributeAccessor.asSetReference(nestedAttribute);
              assertThat(referenceList)
                  .containsExactlyInAnyOrder(
                      ProductReferenceBuilder.of().id(createdProduct.getId()).build(),
                      ProductReferenceBuilder.of().id(nowExistingProduct.getId()).build(),
                      ProductReferenceBuilder.of().id(testProduct1.getId()).build());
            });
  }

  @Test
  void
      sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProducts() {
    // preparation
    final List<Attribute> nestedAttributeValue =
        List.of(
            createNestedAttributeValueSetOfReferences(
                "product-reference-set",
                createReferenceObject(testProduct1.getKey(), ProductReference.PRODUCT),
                createReferenceObject(testProduct2.getKey(), ProductReference.PRODUCT)));

    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("setOfNestedAttribute")
            .value(List.of(nestedAttributeValue))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
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
            .sync(singletonList(productDraftWithProductReference))
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
            .withKey(productDraftWithProductReference.getKey())
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
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<List<Attribute>> nested = AttributeAccessor.asSetNested(attribute);
              final Attribute nestedAttribute = nested.get(0).get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("product-reference-set");
              final List<Reference> referenceSet =
                  AttributeAccessor.asSetReference(nestedAttribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(testProduct1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(testProduct2.getId());
                      });
            });
  }
}
