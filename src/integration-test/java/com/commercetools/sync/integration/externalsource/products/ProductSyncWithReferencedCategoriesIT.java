package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.CategoryCreateCommand;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncWithReferencedCategoriesIT {
    private static ProductType productType;


    private ProductSyncOptions syncOptions;
    private Category category;
    private Category category2;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> actions;

    @BeforeAll
    static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    @BeforeEach
    void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        deleteAllCategories(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();

        final CategoryDraft category1Draft = CategoryDraftBuilder
            .of(ofEnglish("cat1-name"), ofEnglish("cat1-slug"))
            .key("cat1-key")
            .build();

        category = CTP_TARGET_CLIENT
            .execute(CategoryCreateCommand.of(category1Draft))
            .toCompletableFuture()
            .join();

        final CategoryDraft category2Draft = CategoryDraftBuilder
            .of(ofEnglish("cat2-name"), ofEnglish("cat2-slug"))
            .key("cat2-key")
            .build();

        category2 = CTP_TARGET_CLIENT
            .execute(CategoryCreateCommand.of(category2Draft))
            .toCompletableFuture()
            .join();
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
        actions = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(this::collectErrors)
                                        .beforeUpdateCallback(this::collectActions)
                                        .warningCallback(warningCallBack)
                                        .build();
    }

    private void collectErrors(@Nullable final String errorMessage, @Nullable final Throwable exception) {
        errorCallBackMessages.add(errorMessage);
        errorCallBackExceptions.add(exception);
    }

    private List<UpdateAction<Product>> collectActions(@Nonnull final List<UpdateAction<Product>> actions,
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
    void sync_withCategoryReferenceAsAttribute_shouldCreateProductReferencingExistingCategory() {
        // preparation
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category.getKey()));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(category.getId());
        });
    }

    @Test
    void sync_withSameCategoryReferenceAsAttribute_shouldNotSyncAnythingNew() {
        // preparation
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithCategoryReference))
                         .toCompletableFuture()
                         .join();

        final AttributeDraft newProductReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category.getKey()));
        final ProductVariantDraft newMasterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newProductReferenceAttribute)
            .build();

        final ProductDraft newProductDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
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
        assertThat(syncStatistics).hasValues(1, 0, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(category.getId());
        });
    }

    @Test
    void sync_withChangedCategoryReferenceAsAttribute_shouldUpdateProductReferencingExistingCategory() {
        // preparation
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithCategoryReference))
                         .toCompletableFuture()
                         .join();


        final AttributeDraft newCategoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category2.getKey()));
        final ProductVariantDraft newMasterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(newCategoryReferenceAttribute)
            .build();

        final ProductDraft newProductDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), newMasterVariant)
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
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        final AttributeDraft expectedAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category2.getId()));
        assertThat(actions).containsExactly(SetAttributeInAllVariants.of(expectedAttribute, true));


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(category2.getId());
        });
    }

    @Test
    void sync_withNonExistingCategoryReferenceAsAttribute_ShouldFailCreatingTheProduct() {
        // preparation
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), "nonExistingKey"));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(error -> {
                assertThat(error).isInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) error;
                assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
                assertThat(error.getMessage())
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'category-reference'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'category-reference'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void sync_withCategoryReferenceSetAsAttribute_shouldCreateProductReferencingExistingCategories() {
        // preparation
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category.getKey()));

        final HashSet<Reference<Category>> references = new HashSet<>();
        references.add(Reference.of(Category.referenceTypeId(), category.getKey()));
        references.add(Reference.of(Category.referenceTypeId(), category2.getKey()));

        final AttributeDraft categoryReferenceSetAttribute =
            AttributeDraft.of("category-reference-set", references);


        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute, categoryReferenceSetAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithCategoryReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Category.referenceTypeId());
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(category.getId());
        });

        final Optional<Attribute> createdCategoryReferenceSetAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(categoryReferenceSetAttribute.getName());

        assertThat(createdCategoryReferenceSetAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode()).isInstanceOf(ArrayNode.class);
            final ArrayNode referenceSet = (ArrayNode) attribute.getValueAsJsonNode();
            assertThat(referenceSet)
                .hasSize(2)
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(Category.referenceTypeId());
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(category.getId());
                })
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo(Category.referenceTypeId());
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(category2.getId());
                });
        });
    }

    @Test
    void sync_withCategoryReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
        // preparation
        final AttributeDraft categoryReferenceAttribute =
            AttributeDraft.of("category-reference", Reference.of(Category.referenceTypeId(), category.getKey()));

        final HashSet<Reference<Category>> references = new HashSet<>();
        references.add(Reference.of(Category.referenceTypeId(), "nonExistingKey"));
        references.add(Reference.of(Category.referenceTypeId(), category2.getKey()));

        final AttributeDraft categoryReferenceSetAttribute =
            AttributeDraft.of("category-reference-set", references);


        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(categoryReferenceAttribute, categoryReferenceSetAttribute)
            .build();

        final ProductDraft productDraftWithCategoryReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(error -> {
                assertThat(error).isInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) error;
                assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
                assertThat(error.getMessage())
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'category-reference-set'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"category\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'category-reference-set'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }
}
