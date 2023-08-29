package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createReferenceObject;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.*;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
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
  private List<ProductUpdateAction> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    testProductType1 = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    testProductType2 =
        ensureProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final AttributeDefinitionDraft nestedAttributeDef =
        AttributeDefinitionDraftBuilder.of()
            .type(
                attributeTypeBuilder ->
                    attributeTypeBuilder
                        .nestedBuilder()
                        .typeReference(testProductType2.toReference()))
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
                                    .typeReference(testProductType2.toReference())))
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

    CTP_TARGET_CLIENT
        .productTypes()
        .update(testProductType1, addAttributeActions)
        .executeBlocking();
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

  private List<ProductUpdateAction> collectActions(
      @Nonnull final List<ProductUpdateAction> actions,
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
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getKey(), ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("productType-reference");
              final ProductTypeReference attributeValue =
                  (ProductTypeReference) nestedAttribute.getValue();
              assertThat(attributeValue.getId()).isEqualTo(testProductType1.getId());
            });
  }

  @Test
  void sync_withSameNestedProductTypeReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getId(), ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().create(productDraftWithProductTypeReference).executeBlocking();

    final Attribute newNestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getKey(), ProductTypeReference.PRODUCT_TYPE));

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
            .productType(testProductType1.toResourceIdentifier())
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("productType-reference");
              final ProductTypeReference attributeValue =
                  (ProductTypeReference) nestedAttribute.getValue();
              assertThat(attributeValue.getId()).isEqualTo(testProductType1.getId());
            });
  }

  @Test
  void
      sync_withChangedNestedProductTypeReferenceAsAttribute_shouldUpdateProductReferencingExistingProductType() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType1.getId(), ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
            .name(ofEnglish("productName"))
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT.products().create(productDraftWithProductTypeReference).executeBlocking();

    final Attribute newNestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType2.getKey(), ProductTypeReference.PRODUCT_TYPE));

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

    final ProductDraft newProductDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
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

    final Attribute expectedNestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject(testProductType2.getId(), ProductTypeReference.PRODUCT_TYPE));

    assertThat(actions)
        .containsExactly(
            ProductSetAttributeActionBuilder.of()
                .name("nestedAttribute")
                .variantId(1L)
                .value(List.of(expectedNestedAttributeValue))
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("productType-reference");
              final ProductTypeReference attributeValue =
                  (ProductTypeReference) nestedAttribute.getValue();
              assertThat(attributeValue.getId()).isEqualTo(testProductType2.getId());
            });
  }

  @Test
  void sync_withNonExistingNestedProductTypeReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueReferences(
            "productType-reference",
            createReferenceObject("nonExistingKey", ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
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
                          + "is not valid for field 'nestedAttribute.productType-reference'.");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"product-type\\\"}' "
                + "is not valid for field 'nestedAttribute.productType-reference'.");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withNestedProductTypeReferenceSetAsAttribute_shouldCreateProductReferencingExistingProductType() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "productType-reference-set",
            createReferenceObject(testProductType1.getKey(), ProductTypeReference.PRODUCT_TYPE),
            createReferenceObject(testProductType2.getKey(), ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> nested = AttributeAccessor.asNested(attribute);
              final Attribute nestedAttribute = nested.get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("productType-reference-set");
              assertThat(nestedAttribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceSet =
                  AttributeAccessor.asSetReference(nestedAttribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT_TYPE);
                        assertThat(reference.getId()).isEqualTo(testProductType1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT_TYPE);
                        assertThat(reference.getId()).isEqualTo(testProductType2.getId());
                      });
            });
  }

  @Test
  void
      sync_withNestedProductTypeReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Attribute nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "productType-reference-set",
            createReferenceObject(testProductType1.getKey(), ProductTypeReference.PRODUCT_TYPE),
            createReferenceObject("nonExistingKey", ProductTypeReference.PRODUCT_TYPE));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
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
                          + "is not valid for field 'nestedAttribute.productType-reference-set'.");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"product-type\\\"}' "
                + "is not valid for field 'nestedAttribute.productType-reference-set'.");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProductType() {
    // preparation
    final List<Attribute> nestedAttributeValue =
        List.of(
            createNestedAttributeValueSetOfReferences(
                "productType-reference-set",
                createReferenceObject(testProductType1.getKey(), ProductTypeReference.PRODUCT_TYPE),
                createReferenceObject(
                    testProductType2.getKey(), ProductTypeReference.PRODUCT_TYPE)));

    final Attribute productTypeReferenceAttribute =
        AttributeBuilder.of()
            .name("setOfNestedAttribute")
            .value(List.of(nestedAttributeValue))
            .build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productTypeReferenceAttribute)
            .build();

    final ProductDraft productDraftWithProductTypeReference =
        ProductDraftBuilder.of()
            .productType(testProductType1.toResourceIdentifier())
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

    final Optional<Attribute> createdProductTypeReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(productTypeReferenceAttribute.getName());

    assertThat(createdProductTypeReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<List<Attribute>> nested = AttributeAccessor.asSetNested(attribute);
              final Attribute nestedAttribute = nested.get(0).get(0);
              assertThat(nestedAttribute.getName()).isEqualTo("productType-reference-set");
              assertThat(nestedAttribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceSet =
                  AttributeAccessor.asSetReference(nestedAttribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT_TYPE);
                        assertThat(reference.getId()).isEqualTo(testProductType1.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.PRODUCT_TYPE);
                        assertThat(reference.getId()).isEqualTo(testProductType2.getId());
                      });
            });
  }
}
