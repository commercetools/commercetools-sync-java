package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_ID_FIELD;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createReferenceObject;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncWithReferencedProductsAnyOrderIT {
    private static ProductType productType;


    private ProductSyncOptions syncOptions;
    private Product product;
    private Product product2;
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
        syncOptions = buildSyncOptions();

        final ProductDraft productDraft = ProductDraftBuilder
            .of(productType, ofEnglish("foo"), ofEnglish("foo-slug"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of().key("foo").sku("foo").build())
            .key("foo")
            .build();

        final ProductDraft productDraft2 = ProductDraftBuilder
            .of(productType, ofEnglish("foo2"), ofEnglish("foo-slug-2"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of().key("foo2").sku("foo2").build())
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
        final AttributeDraft productReferenceAttributeX =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(),
                "X"));

        final ArrayNode refSet = JsonNodeFactory.instance.arrayNode();
        refSet.add(createReferenceObject("X1", Product.referenceTypeId()));
        refSet.add(createReferenceObject("X2", Product.referenceTypeId()));
        refSet.add(createReferenceObject("X3", Product.referenceTypeId()));
        refSet.add(createReferenceObject("X4", Product.referenceTypeId()));
        final AttributeDraft productReferenceSetAttribute = AttributeDraft.of("product-reference-set", refSet);

        final ProductDraft productDraftExisting = ProductDraftBuilder
            .of(productType, ofEnglish("foo"), ofEnglish("foo-slug"), emptyList())
            .key("foo")
            .masterVariant(ProductVariantDraftBuilder
                .of().key("foo").sku("foo")
                .attributes(productReferenceAttributeX, productReferenceSetAttribute).build())
            .build();

        final ProductVariantDraft parentMasterVariant = ProductVariantDraftBuilder
            .of()
            .sku("parent-product-master-variant")
            .key("parent-product-master-variant")
            .build();

        final ProductDraft parentProduct = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlug"), parentMasterVariant)
            .key("parent-product")
            .build();

        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(),
                parentProduct.getKey()));

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of()
            .key("new-product-master-variant")
            .sku("new-product-master-variant")
            .attributes(productReferenceAttribute)
            .build();

        final ProductDraft childProduct = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlugp"), masterVariant)
            .key("new-product")
            .build();

        final ProductDraft x = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlugX"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of().key("X").sku("X").build())
            .key("X")
            .build();
        final ProductDraft x1 = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlugX1"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of().key("X1").sku("X1").build())
            .key("X1")
            .build();

        final AttributeDraft productReferenceAttributeX1 =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(),
                "X1"));

        final ProductDraft x2 = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlugX2"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of().key("X2").sku("X2").attributes(productReferenceAttributeX1)
                                                     .build())
            .key("X2")
            .build();


        final AttributeDraft productReferenceAttributeX2 =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(),
                "X2"));


        final ProductDraft x3 = ProductDraftBuilder
            .of(productType, ofEnglish("productName"), ofEnglish("productSlugX3"), emptyList())
            .masterVariant(ProductVariantDraftBuilder.of()
                                                     .key("X3")
                                                     .sku("X3")
                                                     .attributes(productReferenceAttributeX2)
                                                     .build())
            .key("X3")
            .build();


        // test
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            productSync
                .sync(asList(childProduct, parentProduct, productDraftExisting, x, x1, x2, x3))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(2, 2, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(actions).isEmpty();


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(childProduct.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_TYPE_ID_FIELD).asText())
                .isEqualTo(Product.referenceTypeId());
            assertThat(attribute.getValueAsJsonNode().get(REFERENCE_ID_FIELD).asText())
                .isEqualTo(product.getId());
        });
    }
}
