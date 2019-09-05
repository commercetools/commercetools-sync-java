package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_ID_FIELD;
import static com.commercetools.sync.products.helpers.VariantReferenceResolver.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncWithReferencedProductsIT {
    private static ProductType productType;


    private ProductSyncOptions syncOptions;
    private Product product;
    private Product product2;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> actions;

    /**
     * Delete all product related test data from the target project. Then creates for the target CTP project
     * a product type.
     */
    @BeforeAll
    static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * Deletes Products from the target CTP project, then it populates target CTP project with product test
     * data.
     */
    @BeforeEach
    void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();

        final ProductDraft productDraft = ProductDraftBuilder
            .of(productType, ofEnglish("foo"), ofEnglish("foo-slug"), emptyList())
            .key("foo")
            .build();

        final ProductDraft productDraft2 = ProductDraftBuilder
            .of(productType, ofEnglish("foo2"), ofEnglish("foo-slug-2"), emptyList())
            .key("foo2")
            .build();

        product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
        product2 = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft2)));
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
    void sync_withProductReferenceAsAttribute_shouldCreateProductReferencingExistingProduct() {
        // preparation
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product.getKey()));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
        });
    }

    @Test
    void sync_withSameProductReferenceAsAttribute_shouldNotSyncAnythingNew() {
        // preparation
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithProductReference))
                         .toCompletableFuture()
                         .join();

        final AttributeDraft newProductReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product.getKey()));
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
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
        });
    }

    @Test
    void sync_withChangedProductReferenceAsAttribute_shouldUpdateProductReferencingExistingProduct() {
        // preparation
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
            .key("new-product")
            .build();

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraftWithProductReference))
                         .toCompletableFuture()
                         .join();


        final AttributeDraft newProductReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product2.getKey()));
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
        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        final AttributeDraft expectedAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product2.getId()));
        assertThat(actions).containsExactly(SetAttributeInAllVariants.of(expectedAttribute, true));


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText()).isEqualTo(product2.getId());
        });
    }

    @Test
    void sync_withNonExistingProductReferenceAsAttribute_ShouldFailCreatingTheProduct() {
        // preparation
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), "nonExistingKey"));
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(error -> {
                assertThat(error).isInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) error;
                assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
                assertThat(error.getMessage())
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'product-reference'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'product-reference'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }

    @Test
    void sync_withProductReferenceSetAsAttribute_shouldCreateProductReferencingExistingProducts() {
        // preparation
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product.getKey()));

        final HashSet<Reference<Product>> references = new HashSet<>();
        references.add(Reference.of(Product.referenceTypeId(), product.getKey()));
        references.add(Reference.of(Product.referenceTypeId(), product2.getKey()));

        final AttributeDraft productReferenceSetAttribute =
            AttributeDraft.of("product-reference-set", references);


        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute, productReferenceSetAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
        });

        final Optional<Attribute> createdProductReferenceSetAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceSetAttribute.getName());

        assertThat(createdProductReferenceSetAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode()).isInstanceOf(ArrayNode.class);
            final ArrayNode referenceSet = (ArrayNode) attribute.getValueAsJsonNode();
            assertThat(referenceSet)
                .hasSize(2)
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product.getId());
                })
                .anySatisfy(reference -> {
                    assertThat(reference.get(REFERENCE_TYPE_ID_FIELD).asText()).isEqualTo("product");
                    assertThat(reference.get(REFERENCE_ID_FIELD).asText()).isEqualTo(product2.getId());
                });
        });
    }

    @Test
    void sync_withProductReferenceSetContainingANonExistingReference_shouldFailCreatingTheProduct() {
        // preparation
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product.getKey()));

        final HashSet<Reference<Product>> references = new HashSet<>();
        references.add(Reference.of(Product.referenceTypeId(), "nonExistingKey"));
        references.add(Reference.of(Product.referenceTypeId(), product2.getKey()));

        final AttributeDraft productReferenceSetAttribute =
            AttributeDraft.of("product-reference-set", references);


        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .sku("sku")
            .key("new-product-master-variant")
            .attributes(productReferenceAttribute, productReferenceSetAttribute)
            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), masterVariant)
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
        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(error -> {
                assertThat(error).isInstanceOf(ErrorResponseException.class);
                final ErrorResponseException errorResponseException = (ErrorResponseException) error;
                assertThat(errorResponseException.getStatusCode()).isEqualTo(400);
                assertThat(error.getMessage())
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'product-reference-set'");
            });
        assertThat(errorCallBackMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message)
                    .contains("The value '{\"typeId\":\"product\",\"id\":\"nonExistingKey\"}' "
                        + "is not valid for field 'product-reference-set'"));
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();
    }
}
