package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class ProductITUtils {


    public static void deleteProductSyncTestData(@Nonnull final SphereClient sphereClient) {
        deleteProducts(sphereClient);
        deleteProductTypes(sphereClient);
        deleteAllCategories(sphereClient);
        deleteTypes(sphereClient);
    }

    public static ProductType buildProductType(@Nonnull final String resourceAsJsonString,
                                               @Nonnull final SphereClient sphereClient) {
        final ProductType productTypeFromJson = readObjectFromResource(resourceAsJsonString,
            ProductType.typeReference());
        final ProductTypeDraft productTypeDraft = ProductTypeDraftBuilder.of(productTypeFromJson)
                                                                         .build();
        return sphereClient.execute(ProductTypeCreateCommand.of(productTypeDraft))
                           .toCompletableFuture().join();
    }


    public static ProductDraft buildProductDraft(@Nonnull final String resourceAsJsonString,
                                                 @Nonnull final ProductType productType,
                                                 @Nonnull final ProductSyncOptions syncOptions) {
        return createProductDraftBuilder(resourceAsJsonString, productType, syncOptions)
            .categories(emptyList())
            .categoryOrderHints(null)
            .build();
    }

    public static ProductDraft buildProductDraft(@Nonnull final String resourceAsJsonString,
                                          @Nonnull final ProductType productType,
                                          @Nonnull final List<Category> categories,
                                          @Nonnull final CategoryOrderHints categoryOrderHints,
                                          @Nonnull final ProductSyncOptions syncOptions) {
        return createProductDraftBuilder(resourceAsJsonString, productType, syncOptions)
            .categories(categories.stream().map(Category::toReference).collect(toList()))
            .categoryOrderHints(categoryOrderHints)
            .build();
    }

    public static void deleteProducts(@Nonnull final SphereClient sphereClient) {
        final List<CompletableFuture> productDeleteFutures = new ArrayList<>();
        final Consumer<List<Product>> productPageDelete = products -> products.forEach(product -> {
            if (product.getMasterData().isPublished()) {
                product = sphereClient.execute(ProductUpdateCommand.of(product, Unpublish.of()))
                                                 .toCompletableFuture().join();
            }
            productDeleteFutures.add(sphereClient.execute(ProductDeleteCommand.of(product))
                                                 .toCompletableFuture());
        });

        CtpQueryUtils.queryAll(sphereClient, ProductQuery.of(), productPageDelete)
                     .thenCompose(result -> CompletableFuture
                         .allOf(productDeleteFutures.toArray(new CompletableFuture[productDeleteFutures.size()])))
                     .toCompletableFuture().join();
    }

    private static void deleteProductTypes(@Nonnull final SphereClient sphereClient) {
        final List<CompletableFuture> productTypeDeleteFutures = new ArrayList<>();
        final Consumer<List<ProductType>> productTypePageDelete = productTypes -> productTypes.forEach(productType -> {
            final CompletableFuture<ProductType> deleteFuture =
                sphereClient.execute(ProductTypeDeleteCommand.of(productType)).toCompletableFuture();
            productTypeDeleteFutures.add(deleteFuture);
        });

        CtpQueryUtils.queryAll(sphereClient, ProductTypeQuery.of(), productTypePageDelete)
                     .thenCompose(result -> CompletableFuture
                         .allOf(productTypeDeleteFutures
                             .toArray(new CompletableFuture[productTypeDeleteFutures.size()])))
                     .toCompletableFuture().join();
    }

    public static CategoryOrderHints buildRandomCategoryOrderHints(@Nonnull final List<Category> categories) {
        final Map<String, String> categoryOrderHints = new HashMap<>();
        categories.forEach(category -> {
            final double randomDouble = ThreadLocalRandom.current().nextDouble(0, 1);
            categoryOrderHints.put(category.getId(), valueOf(randomDouble ));
        });
        return CategoryOrderHints.of(categoryOrderHints);
    }
}
