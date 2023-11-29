package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createReferenceObjectJson;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.impl.UnresolvedReferencesServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.utils.json.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithReferencedProductsIT {
  private static ProductType productType;

  private ProductSyncOptions syncOptions;
  private Product product;
  private Product product2;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
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

    product = CTP_TARGET_CLIENT.products().create(productDraft).executeBlocking().getBody();
    product2 = CTP_TARGET_CLIENT.products().create(productDraft2).executeBlocking().getBody();
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
  void sync_withProductReferenceAsAttribute_shouldCreateProductReferencingExistingProduct() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product.getKey(), ProductReference.PRODUCT))
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
              final ProductReference productReference = (ProductReference) attribute.getValue();
              assertThat(productReference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(productReference.getId()).isEqualTo(product.getId());
            });
  }

  @Test
  void sync_withProductReferencingItselfAsAttribute_shouldCreateProductReferencingItself() {
    // preparation
    final String sameNewProductKey = "new-product-key";
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(sameNewProductKey, ProductReference.PRODUCT))
            .build();
    final Set<Reference> references =
        Set.of(
            createReferenceObject(sameNewProductKey, ProductReference.PRODUCT),
            createReferenceObject(product.getKey(), ProductReference.PRODUCT));

    final Attribute productReferenceSetAttribute =
        AttributeBuilder.of().name("product-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute, productReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithProductReference =
        ProductDraftBuilder.of()
            .productType(productType.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key(sameNewProductKey)
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
    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithProductReference.getKey())
            .get()
            .execute()
            .toCompletableFuture()
            .join()
            .getBody();

    final JsonNode referenceObjectSameProduct =
        JsonUtils.toJsonNode(
            createReferenceObject(createdProduct.getId(), ProductReference.PRODUCT));
    final JsonNode referenceObjectOtherProduct =
        JsonUtils.toJsonNode(createReferenceObject(product.getId(), ProductReference.PRODUCT));
    assertThat(actions).hasSize(2);
    assertThat(actions.get(0))
        .satisfies(
            productUpdateAction -> {
              assertThat(productUpdateAction)
                  .isInstanceOf(ProductSetAttributeInAllVariantsAction.class);
              ProductSetAttributeInAllVariantsAction castedAction =
                  (ProductSetAttributeInAllVariantsAction) productUpdateAction;
              assertThat(castedAction.getName()).isEqualTo("product-reference");
              assertThat(castedAction.getStaged()).isTrue();
              assertThat(castedAction.getValue()).isEqualTo(referenceObjectSameProduct);
            });
    assertThat(actions.get(1))
        .satisfies(
            productUpdateAction -> {
              assertThat(productUpdateAction)
                  .isInstanceOf(ProductSetAttributeInAllVariantsAction.class);
              ProductSetAttributeInAllVariantsAction castedAction =
                  (ProductSetAttributeInAllVariantsAction) productUpdateAction;
              assertThat(castedAction.getName()).isEqualTo("product-reference-set");
              assertThat(castedAction.getStaged()).isTrue();
              List<JsonNode> valueAsList =
                  JsonUtils.fromJsonNode(
                      (JsonNode) castedAction.getValue(), new TypeReference<>() {});
              assertThat(valueAsList)
                  .containsExactlyInAnyOrder(
                      referenceObjectSameProduct, referenceObjectOtherProduct);
            });

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final ProductReference productReference = (ProductReference) attribute.getValue();
              assertThat(productReference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
              assertThat(productReference.getId()).isEqualTo(createdProduct.getId());
            });

    final Optional<Attribute> createdProductReferenceSetAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceSetAttribute.getName());

    assertThat(createdProductReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Reference> referenceList = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceList)
                  .containsExactlyInAnyOrder(
                      ProductReferenceBuilder.of().id(createdProduct.getId()).build(),
                      ProductReferenceBuilder.of().id(product.getId()).build());
            });
  }

  @Test
  void sync_withSameProductReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product.getId(), ProductReference.PRODUCT))
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

    final Attribute newProductReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product.getKey(), ProductReference.PRODUCT))
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
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductReference.PRODUCT);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(product.getId());
            });
  }

  @Test
  void sync_withChangedProductReferenceAsAttribute_shouldUpdateProductReferencingExistingProduct() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product.getId(), ProductReference.PRODUCT))
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

    final Attribute newProductReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product2.getKey(), ProductReference.PRODUCT))
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
    final ObjectNode expectedAttribute =
        createReferenceObjectJson(product2.getId(), ProductReference.PRODUCT);
    assertThat(actions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("product-reference")
                .value(expectedAttribute)
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
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductReference.PRODUCT);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(product2.getId());
            });
  }

  @Test
  void sync_withNonExistingProductReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject("nonExistingKey", ProductReference.PRODUCT))
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
  void sync_withProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProducts() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product.getKey(), ProductReference.PRODUCT))
            .build();

    final Set<Reference> references =
        Set.of(
            createReferenceObject(product.getKey(), ProductReference.PRODUCT),
            createReferenceObject(product2.getKey(), ProductReference.PRODUCT));

    final Attribute productReferenceSetAttribute =
        AttributeBuilder.of().name("product-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute, productReferenceSetAttribute)
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
              final JsonNode attributeValue = JsonUtils.toJsonNode(attribute.getValue());
              assertThat(attributeValue.get(REFERENCE_TYPE_ID_FIELD).asText())
                  .isEqualTo(ProductReference.PRODUCT);
              assertThat(attributeValue.get(REFERENCE_ID_FIELD).asText())
                  .isEqualTo(product.getId());
            });

    final Optional<Attribute> createdProductReferenceSetAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceSetAttribute.getName());

    assertThat(createdProductReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceSet = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(product.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT);
                        assertThat(reference.getId()).isEqualTo(product2.getId());
                      });
            });
  }

  @Test
  void sync_withProductReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Attribute productReferenceAttribute =
        AttributeBuilder.of()
            .name("product-reference")
            .value(createReferenceObject(product.getKey(), ProductReference.PRODUCT))
            .build();

    final Set<Reference> references =
        Set.of(
            createReferenceObject("nonExistingKey", ProductReference.PRODUCT),
            createReferenceObject("new-product", ProductReference.PRODUCT),
            createReferenceObject(product2.getKey(), ProductReference.PRODUCT));

    final Attribute productReferenceSetAttribute =
        AttributeBuilder.of().name("product-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute, productReferenceSetAttribute)
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

    final Product missingProductCreated =
        CTP_TARGET_CLIENT
            .products()
            .create(
                ProductDraftBuilder.of()
                    .productType(productType.toResourceIdentifier())
                    .name(ofEnglish("noExistingName"))
                    .slug(ofEnglish("noExistingSlug"))
                    .key("nonExistingKey")
                    .build())
            .executeBlocking()
            .getBody();
    // rebuild syncOptions to clear old statistics
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

    final Optional<Attribute> createdProductReferenceSetAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productReferenceSetAttribute.getName());

    assertThat(createdProductReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Reference> referenceList = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceList)
                  .containsExactlyInAnyOrder(
                      ProductReferenceBuilder.of().id(createdProduct.getId()).build(),
                      ProductReferenceBuilder.of().id(missingProductCreated.getId()).build(),
                      ProductReferenceBuilder.of().id(product2.getId()).build());
            });
  }
}
