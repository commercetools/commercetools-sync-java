package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.*;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;

import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.client.error.ErrorResponseException;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryDraftBuilder;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetAttributeActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.AttributeDefinitionDraft;
import com.commercetools.api.models.product_type.AttributeDefinitionDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeAddAttributeDefinitionAction;
import com.commercetools.api.models.product_type.ProductTypeAddAttributeDefinitionActionBuilder;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithNestedReferencedCategoriesIT {
  private static ProductType productType;
  private static Category testCategory1;
  private static Category testCategory2;

  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> actions;

  private static final String ATTRIBUTE_NAME_FIELD = "name";
  private static final String ATTRIBUTE_VALUE_FIELD = "value";

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    final ProductType nestedProductType =
        createProductType(PRODUCT_TYPE_WITH_REFERENCES_RESOURCE_PATH, CTP_TARGET_CLIENT);

    final AttributeDefinitionDraft nestedAttributeDef =
        AttributeDefinitionDraftBuilder.of()
                        .type(attributeTypeBuilder -> attributeTypeBuilder.nestedBuilder().typeReference(productTypeReferenceBuilder -> productTypeReferenceBuilder.id(nestedProductType.getId())))
                .name(
                "nestedAttribute")
                        .label(
                ofEnglish("nestedAttribute"))
                .isRequired(
                false)
            .isSearchable(false)
            .build();

    final AttributeDefinitionDraft setOfNestedAttributeDef =
        AttributeDefinitionDraftBuilder.of()
                        .type(attributeTypeBuilder -> attributeTypeBuilder.setBuilder()
                                .elementType(attributeTypeBuilder1 -> attributeTypeBuilder1.nestedBuilder().typeReference(productTypeReferenceBuilder -> productTypeReferenceBuilder.id(nestedProductType.getId()))))
                .name(
                "setOfNestedAttribute")
                        .label(
                ofEnglish("setOfNestedAttribute"))
                        .isRequired(
                false)
            .isSearchable(false)
            .build();

      final List<ProductTypeUpdateAction> productTypeUpdateActions = List.of(
              ProductTypeAddAttributeDefinitionActionBuilder.of().attribute(nestedAttributeDef).build(),
              ProductTypeAddAttributeDefinitionActionBuilder.of().attribute(setOfNestedAttributeDef).build());

    CTP_TARGET_CLIENT.productTypes().update(productType, productTypeUpdateActions).executeBlocking();

    final CategoryDraft category1Draft =
        CategoryDraftBuilder.of().name(ofEnglish("cat1-name")).slug(ofEnglish("cat1-slug"))
            .key("cat1-key")
            .build();

    testCategory1 =
        CTP_TARGET_CLIENT
                .categories()
                .create(category1Draft)
                .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final CategoryDraft category2Draft =
        CategoryDraftBuilder.of().name(ofEnglish("cat2-name")).slug(ofEnglish("cat2-slug"))
            .key("cat2-key")
            .build();

    testCategory2 =
        CTP_TARGET_CLIENT
                .categories()
                .create(category2Draft)
            .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
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
      @Nonnull final ProductProjection productP) {
    this.actions.addAll(actions);
    return actions;
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void
      sync_withNestedCategoryReferenceAsAttribute_shouldCreateProductReferencingExistingCategory() {
    // preparation
    final Map<String, Object> nestedAttributeValue =
        createNestedAttributeValueReferences(
            "category-reference",
            CategoryReferenceBuilder.of().id(testCategory1.getKey()).build()
        );

    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of().productType(productType.toResourceIdentifier()).name(ofEnglish("productName")).slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCategoryReference))
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
                .withKey(productDraftWithCategoryReference.getKey())
                .get()
                .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCategoryReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdCategoryReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute categoryReferenceAttr = value.get(0);
              assertThat(categoryReferenceAttr.getName()).isEqualTo("category-reference");
              final CategoryReference categoryReferenceAttrValue = (CategoryReference) categoryReferenceAttr.getValue();
              assertThat(categoryReferenceAttrValue.getId()).isEqualTo(testCategory1.getId());
            });
  }

  @Test
  void sync_withSameNestedCategoryReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final Map<String, Object> nestedAttributeValue =
        createNestedAttributeValueReferences(
            "category-reference",
            CategoryReferenceBuilder.of().id(testCategory1.getKey()).build());

    final Attribute categoryReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of().productType(productType.toResourceIdentifier()).name(ofEnglish("productName"))
    .slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
            .products()
            .create(productDraftWithCategoryReference)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture()
        .join();

    final Map<String, Object> newNestedAttributeValue =
            createNestedAttributeValueReferences(
                    "category-reference",
                    CategoryReferenceBuilder.of().id(testCategory1.getKey()).build());


    final Attribute newProductReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();


    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithProductReference =
        ProductDraftBuilder.of()
                .productType(productType.toResourceIdentifier()).name(ofEnglish("productName")).slug(ofEnglish("productSlug")).masterVariant(newMasterVariant)
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
                .withKey(productDraftWithCategoryReference.getKey())
                .get()
                .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCategoryReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdCategoryReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute categoryReferenceAttr = value.get(0);
              assertThat(categoryReferenceAttr.getName()).isEqualTo("category-reference");
              final CategoryReference categoryReferenceAttrValue = (CategoryReference) categoryReferenceAttr.getValue();
              assertThat(categoryReferenceAttrValue.getId()).isEqualTo(testCategory1.getId());
            });
  }

  @Test
  void
      sync_withChangedNestedCategoryReferenceAsAttribute_shouldUpdateProductReferencingExistingCategory() {
    // preparation
    final Map<String, Object> nestedAttributeValue =
            createNestedAttributeValueReferences(
                    "category-reference",
                    CategoryReferenceBuilder.of().id(testCategory1.getKey()).build());


    final Attribute categoryReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of()
                .productType(productType.toResourceIdentifier()).name(ofEnglish("productName")).slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
            .products()
            .create(productDraftWithCategoryReference)
            .executeBlocking();

    final Map<String, Object> newNestedAttributeValue =
            createNestedAttributeValueReferences(
                    "category-reference",
                    CategoryReferenceBuilder.of().id(testCategory2.getKey()).build());

    final Attribute newProductReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(newNestedAttributeValue)).build();

    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithCategoryReference =
        ProductDraftBuilder.of()
                .productType(
                productType.toResourceIdentifier()).name(ofEnglish("productName")).slug(ofEnglish("productSlug")).masterVariant(newMasterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(newProductDraftWithCategoryReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Map<String, Object> expectedNestedAttributeValue =
            createNestedAttributeValueReferences(
                    "category-reference",
                    CategoryReferenceBuilder.of().id(testCategory2.getKey()).build());

    final Attribute expectedCategoryReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    assertThat(actions)
        .containsExactly(
                ProductSetAttributeActionBuilder.of().variantId(1L).name(expectedCategoryReferenceAttribute.getName()).value(expectedCategoryReferenceAttribute.getValue()).staged(true).build()
        );

    final Product createdProduct =
        CTP_TARGET_CLIENT
                .products()
                .withKey(productDraftWithCategoryReference.getKey())
                .get()
                .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCategoryReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdCategoryReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute categoryReferenceAttr = value.get(0);
              assertThat(categoryReferenceAttr.getName()).isEqualTo("category-reference");
              final CategoryReference categoryReferenceAttrValue = (CategoryReference) categoryReferenceAttr.getValue();
              assertThat(categoryReferenceAttrValue.getId()).isEqualTo(testCategory2.getId());
            });
  }

  @Test
  void sync_withNonExistingNestedCategoryReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Map<String, Object> nestedAttributeValue =
            createNestedAttributeValueReferences(
                    "category-reference",
                    CategoryReferenceBuilder.of().id("getKey").build());

    final Attribute categoryReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of()
                .productType(productType.toResourceIdentifier())
                .name(ofEnglish("productName"))
    .slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCategoryReference))
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
              final BadRequestException badRequestException = (BadRequestException) completionException.getCause();
              assertThat(badRequestException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' is not valid for field 'nestedAttribute.category-reference'.");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' is not valid for field 'nestedAttribute.category-reference'.");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withNestedCategoryReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
    // preparation
    final Map<String, Object> nestedAttributeValue =
        createNestedAttributeValueSetOfReferences(
            "category-reference-set",
            CategoryReferenceBuilder.of().id(testCategory1.getKey()).build(),
            CategoryReferenceBuilder.of().id(testCategory2.getKey()).build());

    final Attribute categoryReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of()
                .productType(productType.toResourceIdentifier())
                .name(ofEnglish("productName"))
    .slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCategoryReference))
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
                .withKey(productDraftWithCategoryReference.getKey())
                .get()
                .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCategoryReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdCategoryReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final List<Attribute> value = (List<Attribute>) attribute.getValue();
              final Attribute categoryReferenceAttr = value.get(0);
              assertThat(categoryReferenceAttr.getName()).isEqualTo("category-reference-set");
              final List<CategoryReference> categoryReferenceAttrValue = (List<CategoryReference>) categoryReferenceAttr.getValue();
              assertThat(categoryReferenceAttrValue).hasSize(2)
                      .anySatisfy(categoryReference -> categoryReference.getId().equals(testCategory1.getId()))
                      .anySatisfy(categoryReference -> categoryReference.getId().equals(testCategory2.getId()));
            });
  }

  @Test
  void
      sync_withNestedCategoryReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Map<String, Object> nestedAttributeValue =
            createNestedAttributeValueSetOfReferences(
                    "category-reference-set",
                    CategoryReferenceBuilder.of().id(testCategory1.getKey()).build(),
                    CategoryReferenceBuilder.of().id("nonExistingKey").build());

    final Attribute categoryReferenceAttribute =
            AttributeBuilder.of().name("nestedAttribute").value(List.of(nestedAttributeValue)).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of().productType(productType.toResourceIdentifier())
                .name(ofEnglish("productName")).slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCategoryReference))
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
              final BadRequestException badRequestException = (BadRequestException) completionException.getCause();
              assertThat(badRequestException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' is not valid for field 'nestedAttribute.category-reference-set'.");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' is not valid for field 'nestedAttribute.category-reference-set'.");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void
      sync_withSetOfNestedProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
    // preparation
//    final ArrayNode nestedAttributeValue =
//        createArrayNode(
//            createNestedAttributeValueSetOfReferences(
//                "category-reference-set",
//                createReferenceObject(testCategory1.getKey(), Category.referenceTypeId()),
//                createReferenceObject(testCategory2.getKey(), Category.referenceTypeId())));

      final Map<String, Object> nestedAttributeValue =
              createNestedAttributeValueSetOfReferences(
                      "category-reference-set",
                      CategoryReferenceBuilder.of().id(testCategory1.getKey()).build(),
                      CategoryReferenceBuilder.of().id(testCategory2.getKey()).build());

      final Attribute categoryReferenceAttribute =
              AttributeBuilder.of().name("setOfNestedAttribute").value(List.of(List.of(nestedAttributeValue))).build();

//      final AttributeDraft categoryReferenceAttribute =
//        AttributeDraft.of("setOfNestedAttribute", createArrayNode(nestedAttributeValue));

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
        ProductDraftBuilder.of()
                .productType(productType.toResourceIdentifier())
                .name(ofEnglish("productName"))
      .slug(ofEnglish("productSlug")).masterVariant(masterVariant)
            .key("new-product")
            .build();

    // test
    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithCategoryReference))
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
                .withKey(productDraftWithCategoryReference.getKey())
                .get()
                .execute()
                .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();

    final Optional<Attribute> createdCategoryReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdCategoryReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
                final List<Attribute> value = (List<Attribute>) attribute.getValue();
                assertThat(value).isNotNull();

//              final JsonNode setOfNestedAttributeNameField =
//                  attribute.getValueAsJsonNode().get(0).get(0).get(ATTRIBUTE_NAME_FIELD);
//              final JsonNode setOfNestedAttributeValueField =
//                  attribute.getValueAsJsonNode().get(0).get(0).get(ATTRIBUTE_VALUE_FIELD);
//
//              assertThat(setOfNestedAttributeNameField.asText())
//                  .isEqualTo("category-reference-set");
//              assertThat(setOfNestedAttributeValueField).isInstanceOf(ArrayNode.class);
//              final ArrayNode referenceSet = (ArrayNode) setOfNestedAttributeValueField;
//              assertThat(referenceSet)
//                  .hasSize(2)
//                  .anySatisfy(
//                      reference -> {
//                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
//                            .isEqualTo(Category.referenceTypeId());
//                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
//                            .isEqualTo(testCategory1.getId());
//                      })
//                  .anySatisfy(
//                      reference -> {
//                        assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText())
//                            .isEqualTo(Category.referenceTypeId());
//                        assertThat(reference.get(REFERENCE_ID_FIELD).asText())
//                            .isEqualTo(testCategory2.getId());
//                      });
            });
  }
}
