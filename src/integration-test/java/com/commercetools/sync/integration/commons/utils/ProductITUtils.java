package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;

public final class ProductITUtils {
    public static final String PRODUCT_KEY_1_CHANGED_RESOURCE_PATH = "product-key-1-changed.json";
    public static final String PRODUCT_KEY_2_RESOURCE_PATH = "product-key-2.json";
    public static final String PRODUCT_TYPE_RESOURCE_PATH = "product-type.json";

    /**
     * This method blocks to create a product type, which is defined by the JSON resource found in the supplied
     * {@code jsonResourcePath}, in the CTP project defined by the supplied {@code ctpClient}.
     *
     * @param jsonResourcePath defines the path of the JSON resource of the product type.
     * @param ctpClient        defines the CTP project to create the categories on.
     */
    public static ProductType createProductType(@Nonnull final String jsonResourcePath,
                                                @Nonnull final SphereClient ctpClient) {
        final ProductType productTypeFromJson = readObjectFromResource(jsonResourcePath,
            ProductType.class);
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder.of(productTypeFromJson)
                                                                         .build();
        return ctpClient.execute(ProductTypeCreateCommand.of(productTypeDraft))
                        .toCompletableFuture().join();
    }

    /**
     * Deletes all products, product types, categories and types from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the categories from.
     */
    public static void deleteProductSyncTestData(@Nonnull final SphereClient ctpClient) {
        deleteAllProducts(ctpClient);
        deleteProductTypes(ctpClient);
        deleteAllCategories(ctpClient);
        deleteTypes(ctpClient);
    }

    /**
     * Deletes all products from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the categories from.
     */
    public static void deleteAllProducts(@Nonnull final SphereClient ctpClient) {
        final List<CompletableFuture> productDeleteFutures = new ArrayList<>();
        final Consumer<List<Product>> productPageDelete = products -> products.forEach(product -> {
            if (product.getMasterData().isPublished()) {
                product = ctpClient.execute(ProductUpdateCommand.of(product, Unpublish.of()))
                                   .toCompletableFuture().join();
            }
            productDeleteFutures.add(ctpClient.execute(ProductDeleteCommand.of(product))
                                              .toCompletableFuture());
        });

        CtpQueryUtils.queryAll(ctpClient, ProductQuery.of(), productPageDelete)
                     .thenCompose(result -> CompletableFuture
                         .allOf(productDeleteFutures.toArray(new CompletableFuture[productDeleteFutures.size()])))
                     .toCompletableFuture().join();
    }

    /**
     * Deletes all product types from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the categories from.
     */
    private static void deleteProductTypes(@Nonnull final SphereClient ctpClient) {
        final List<CompletableFuture> productTypeDeleteFutures = new ArrayList<>();
        final Consumer<List<ProductType>> productTypePageDelete = productTypes -> productTypes.forEach(productType -> {
            final CompletableFuture<ProductType> deleteFuture =
                ctpClient.execute(ProductTypeDeleteCommand.of(productType)).toCompletableFuture();
            productTypeDeleteFutures.add(deleteFuture);
        });

        CtpQueryUtils.queryAll(ctpClient, ProductTypeQuery.of(), productTypePageDelete)
                     .thenCompose(result -> CompletableFuture
                         .allOf(productTypeDeleteFutures
                             .toArray(new CompletableFuture[productTypeDeleteFutures.size()])))
                     .toCompletableFuture().join();
    }
}
