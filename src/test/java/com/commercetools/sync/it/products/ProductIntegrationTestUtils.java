package com.commercetools.sync.it.products;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductDraftDsl;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.ProductTypeDraftDsl;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

import static com.commercetools.sync.it.products.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.it.products.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.it.products.SphereClientUtils.fetchAndProcess;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;

/**
 * Empty.
 */
public class ProductIntegrationTestUtils {

    /**
     * Empty.
     */
    public static void deleteProductTypes(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, ProductIntegrationTestUtils::productTypeQuerySupplier,
                ProductTypeDeleteCommand::of);
    }

    /**
     * Empty.
     */
    public static void deleteProducts(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, ProductIntegrationTestUtils::productQuerySupplier,
                ProductDeleteCommand::of);
    }

    /**
     * Empty.
     */
    public static void deleteProducts() {
        deleteProducts(CTP_SOURCE_CLIENT);
        deleteProductTypes(CTP_SOURCE_CLIENT);
        deleteProducts(CTP_TARGET_CLIENT);
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    /**
     * Empty.
     */
    public static void populateSourceProject() {
        populateProduct(populateProductType());
    }

    /**
     * Empty.
     */
    public static Product populateProduct(final ProductType productType) {
        Product mockMainProduct = getMockMainProduct();
        ProductData current = mockMainProduct.getMasterData().getCurrent();
        List<ProductVariantDraft> allVariants = current.getAllVariants().stream()
                .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
                .collect(Collectors.toList());
        ProductDraftDsl build = ProductDraftBuilder.of(productType, current.getName(), current.getSlug(),
                allVariants).build();

        return CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(build)).toCompletableFuture().join();
    }

    /**
     * Empty.
     */
    public static void populateTargetProject() {
    }

    private static ProductType getMockMainProductType() {
        return readObjectFromResource("product-type-main.json", ProductType.typeReference());
    }

    private static Product getMockMainProduct() {
        return readObjectFromResource("product.json", Product.typeReference());
    }

    private static ProductType populateProductType() {
        ProductTypeDraftDsl build = ProductTypeDraftBuilder.of(getMockMainProductType()).build();
        return CTP_SOURCE_CLIENT.execute(ProductTypeCreateCommand.of(build)).toCompletableFuture().join();
    }

    private static ProductQuery productQuerySupplier() {
        return ProductQuery.of().withLimit(QUERY_MAX_LIMIT);
    }

    private static ProductTypeQuery productTypeQuerySupplier() {
        return ProductTypeQuery.of().withLimit(QUERY_MAX_LIMIT);
    }

    private static LocalizedString en(final String string) {
        return LocalizedString.ofEnglish(string);
    }
}
