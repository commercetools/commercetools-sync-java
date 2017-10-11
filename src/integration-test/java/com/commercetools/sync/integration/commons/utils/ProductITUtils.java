package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.utils.CtpQueryUtils;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductDeleteCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteSupplyChannels;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.stream.Collectors.toList;

public final class ProductITUtils {
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
        deleteSupplyChannels(ctpClient);
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
     * Builds the query for fetching products from the source CTP project with all the needed expansions.
     * @return the query for fetching products from the source CTP project with all the needed expansions.
     */
    public static ProductQuery getProductQuery() {
        return ProductQuery.of().withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                           .withExpansionPaths(ProductExpansionModel::productType)
                           .plusExpansionPaths(productProductExpansionModel ->
                               productProductExpansionModel.masterData().staged().categories())
                           .plusExpansionPaths(channelExpansionModel ->
                               channelExpansionModel.masterData().staged().allVariants().prices().channel());
    }

    /**
     * Gets the supplied {@link ProductDraft} with the price channel reference attached.
     *
     * @param productDraft TODO
     * @param channelReference TODO
     * @return TODO.
     */
    public static ProductDraft getDraftWithPriceChannelReferences(@Nonnull final ProductDraft productDraft,
                                                            @Nonnull final Reference<Channel> channelReference) {
        final List<ProductVariantDraft> allVariants = productDraft
            .getVariants().stream().map(productVariant -> {
                final List<PriceDraft> priceDraftsWithChannelReferences =
                    productVariant.getPrices().stream()
                                  .map(price -> PriceDraftBuilder.of(price).channel(channelReference).build())
                                  .collect(toList());
                return ProductVariantDraftBuilder.of(productVariant)
                                                 .prices(priceDraftsWithChannelReferences)
                                                 .build();
            })
            .collect(toList());
        final List<PriceDraft> masterVariantPriceDrafts = productDraft
            .getMasterVariant().getPrices().stream().map(price -> PriceDraftBuilder.of(price)
                                                                                   .channel(channelReference)
                                                                                   .build()).collect(toList());
        return ProductDraftBuilder.of(productDraft)
                                  .masterVariant(ProductVariantDraftBuilder.of(productDraft.getMasterVariant())
                                                                           .prices(masterVariantPriceDrafts).build())
                                  .variants(allVariants)
                                  .build();
    }
}
