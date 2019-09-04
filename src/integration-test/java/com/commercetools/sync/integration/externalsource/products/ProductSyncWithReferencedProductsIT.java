package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductSyncWithReferencedProductsIT {
    private static ProductType productType;


    private ProductSyncOptions syncOptions;
    private Product product;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

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

        product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(this::collectErrors)
                                        .warningCallback(warningCallBack)
                                        .build();
    }

    private void collectErrors(final String errorMessage, final Throwable exception) {
        errorCallBackMessages.add(errorMessage);
        errorCallBackExceptions.add(exception);
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


        final Product createdProduct = CTP_TARGET_CLIENT
            .execute(ProductByKeyGet.of(productDraftWithProductReference.getKey()))
            .toCompletableFuture()
            .join();

        final Optional<Attribute> createdProductReferenceAttribute =
            createdProduct.getMasterData().getStaged().getMasterVariant()
                          .findAttribute(productReferenceAttribute.getName());

        assertThat(createdProductReferenceAttribute).hasValueSatisfying(attribute -> {
           assertThat(attribute.getValueAsJsonNode().get("typeId").asText()).isEqualTo("product");
           assertThat(attribute.getValueAsJsonNode().get("id").asText()).isEqualTo(product.getId());
        });
    }
}
