package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createReferenceObjectJson;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.ReferenceTypeId;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncWithReferencedCategoriesIT {
  private static ProductType productType;
  private static Category category;
  private static Category category2;

  private ProductSyncOptions syncOptions;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> actions;

  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    category = createCategoryInProjectWithPrefix("cat1", CTP_TARGET_CLIENT);
    category2 = createCategoryInProjectWithPrefix("cat2", CTP_TARGET_CLIENT);
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
            (syncException, draft, product) ->
                warningCallBackMessages.add(syncException.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (exception, draft, product, updateActions) ->
                collectErrors(exception.getMessage(), exception))
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

  private static Category createCategoryInProjectWithPrefix(
      final String prefix, final ProjectApiRoot ctpClient) {
    final CategoryDraft categoryDraft =
        CategoryDraftBuilder.of()
            .name(ofEnglish(prefix + "-name"))
            .slug(ofEnglish(prefix + "-slug"))
            .key(prefix + "-key")
            .build();

    return ctpClient
        .categories()
        .create(categoryDraft)
        .execute()
        .toCompletableFuture()
        .join()
        .getBody();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withCategoryReferenceAsAttribute_shouldCreateProductReferencingExistingCategory() {
    // preparation
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of()
            .name("category-reference")
            .value(CategoryReferenceBuilder.of().id(category.getKey()).build())
            .build();
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
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
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
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.CATEGORY);
              assertThat(reference.getId()).isEqualTo(category.getId());
            });
  }

  @Test
  void sync_withSameCategoryReferenceAsAttribute_shouldNotSyncAnythingNew() {
    // preparation
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of().name("category-reference").value(category.toReference()).build();
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
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .products()
        .create(productDraftWithCategoryReference)
        .execute()
        .toCompletableFuture()
        .join();

    final Attribute newProductReferenceAttribute =
        AttributeBuilder.of()
            .name("category-reference")
            .value(CategoryReferenceBuilder.of().id(category.getKey()).build())
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
            .withKey(productDraftWithCategoryReference.getKey())
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
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.CATEGORY);
              assertThat(reference.getId()).isEqualTo(category.getId());
            });
  }

  @Test
  void
      sync_withChangedCategoryReferenceAsAttribute_shouldUpdateProductReferencingExistingCategory() {
    // preparation
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of().name("category-reference").value(category.toReference()).build();
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
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
            .key("new-product")
            .build();

    CTP_TARGET_CLIENT
        .products()
        .create(productDraftWithCategoryReference)
        .execute()
        .toCompletableFuture()
        .join();

    final Attribute newCategoryReferenceAttribute =
        AttributeBuilder.of()
            .name("category-reference")
            .value(CategoryReferenceBuilder.of().id(category2.getKey()).build())
            .build();
    final ProductVariantDraft newMasterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newCategoryReferenceAttribute)
            .build();

    final ProductDraft newProductDraftWithCategoryReference =
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
            .sync(singletonList(newProductDraftWithCategoryReference))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions)
        .containsExactly(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("category-reference")
                .value(createReferenceObjectJson(category2.getId(), CategoryReference.CATEGORY))
                .staged(true)
                .build());

    final Product createdProduct =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraftWithCategoryReference.getKey())
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
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.CATEGORY);
              assertThat(reference.getId()).isEqualTo(category2.getId());
            });
  }

  @Test
  void sync_withNonExistingCategoryReferenceAsAttribute_ShouldFailCreatingTheProduct() {
    // preparation
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of()
            .name("category-reference")
            .value(CategoryReferenceBuilder.of().id("nonExistingKey").build())
            .build();
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
            .slug(ofEnglish("productSlug"))
            .masterVariant(masterVariant)
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
              final BadRequestException badRequestException =
                  (BadRequestException) completionException.getCause();
              assertThat(badRequestException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' "
                          + "is not valid for field 'category-reference'");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' "
                + "is not valid for field 'category-reference'");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }

  @Test
  void sync_withCategoryReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
    // preparation
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of()
            .name("category-reference")
            .value(CategoryReferenceBuilder.of().id(category.getKey()).build())
            .build();

    final List<CategoryReference> references =
        List.of(
            CategoryReferenceBuilder.of().id(category.getKey()).build(),
            CategoryReferenceBuilder.of().id(category2.getKey()).build());

    final Attribute categoryReferenceSetAttribute =
        AttributeBuilder.of().name("category-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute, categoryReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
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
            .toCompletableFuture()
            .join()
            .getBody();

    final Optional<Attribute> createdProductReferenceAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceAttribute.getName());

    assertThat(createdProductReferenceAttribute)
        .hasValueSatisfying(
            attribute -> {
              final Reference reference = AttributeAccessor.asReference(attribute);
              assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.CATEGORY);
              assertThat(reference.getId()).isEqualTo(category.getId());
            });

    final Optional<Attribute> createdCategoryReferenceSetAttribute =
        createdProduct
            .getMasterData()
            .getStaged()
            .getMasterVariant()
            .findAttribute(categoryReferenceSetAttribute.getName());

    assertThat(createdCategoryReferenceSetAttribute)
        .hasValueSatisfying(
            attribute -> {
              assertThat(attribute.getValue()).isInstanceOf(List.class);
              final List<Reference> referenceSet = AttributeAccessor.asSetReference(attribute);
              assertThat(referenceSet)
                  .hasSize(2)
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.CATEGORY);
                        assertThat(reference.getId()).isEqualTo(category.getId());
                      })
                  .anySatisfy(
                      reference -> {
                        assertThat(reference.getTypeId()).isEqualTo(ReferenceTypeId.CATEGORY);
                        assertThat(reference.getId()).isEqualTo(category2.getId());
                      });
            });
  }

  @Test
  void sync_withCategoryReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
    // preparation
    final Attribute categoryReferenceAttribute =
        AttributeBuilder.of()
            .name("category-reference")
            .value(CategoryReferenceBuilder.of().id(category.getKey()).build())
            .build();

    final List<CategoryReference> references =
        List.of(
            CategoryReferenceBuilder.of().id("nonExistingKey").build(),
            CategoryReferenceBuilder.of().id(category2.getKey()).build());

    final Attribute categoryReferenceSetAttribute =
        AttributeBuilder.of().name("category-reference-set").value(references).build();

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute, categoryReferenceSetAttribute)
            .build();

    final ProductDraft productDraftWithCategoryReference =
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
              final BadRequestException badRequestException =
                  (BadRequestException) completionException.getCause();
              assertThat(badRequestException.getStatusCode()).isEqualTo(400);
              assertThat(error.getMessage())
                  .contains(
                      "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' "
                          + "is not valid for field 'category-reference-set'");
              return true;
            });
    assertThat(errorCallBackMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "The value '{\\\"id\\\":\\\"nonExistingKey\\\",\\\"typeId\\\":\\\"category\\\"}' "
                + "is not valid for field 'category-reference-set'");
    assertThat(warningCallBackMessages).isEmpty();
    assertThat(actions).isEmpty();
  }
}
