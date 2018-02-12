package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereRequest;
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
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.states.StateType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndCompose;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.deleteSupplyChannels;
import static java.util.stream.Collectors.toList;

public final class ProductITUtils {

    /**
     * Deletes all products, product types, categories and types from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the product types from.
     */
    public static void deleteProductSyncTestData(@Nonnull final SphereClient ctpClient) {
        deleteAllProducts(ctpClient);
        deleteProductTypes(ctpClient);
        deleteAllCategories(ctpClient);
        deleteTypes(ctpClient);
        deleteSupplyChannels(ctpClient);
        deleteStates(ctpClient, StateType.PRODUCT_STATE);
        deleteTaxCategories(ctpClient);
    }

    /**
     * Unpublishes all published products, then deletes all products from the CTP project defined by the
     * {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the products from.
     */
    public static void deleteAllProducts(@Nonnull final SphereClient ctpClient) {
        queryAndCompose(ctpClient, ProductQuery::of, product -> deleteProduct(ctpClient, product));
    }

    /**
     * If the {@code product} is published, issues an unpublish request followed by a delete. Otherwise,
     * issues a delete request right away.
     *
     * @param ctpClient defines the CTP project to delete the product from.
     * @param product the product to be deleted.
     * @return a {@link CompletionStage} containing the deleted product.
     */
    @Nonnull
    private static CompletionStage<Product> deleteProduct(@Nonnull final SphereClient ctpClient,
                                                          @Nonnull final Product product) {
        return product.getMasterData().isPublished()
            ? unpublishAndDelete(ctpClient, product) : deleteUnpublishedProduct(ctpClient, product);
    }

    /**
     * Issues an unpublish request followed by a delete.
     *
     * @param ctpClient defines the CTP project to delete the product from.
     * @param product the product to be unpublished and deleted.
     * @return a {@link CompletionStage} containing the deleted product.
     */
    @Nonnull
    private static CompletionStage<Product> unpublishAndDelete(@Nonnull final SphereClient ctpClient,
                                                               @Nonnull final Product product) {
        return ctpClient.execute(buildUnpublishRequest(product))
                        .thenCompose(unpublishedProduct ->
                            deleteUnpublishedProduct(ctpClient, unpublishedProduct));
    }

    /**
     * Note: This method assumes the product is unpublished.
     *
     * @param ctpClient the client defining the CTP project to delete the product from.
     * @param product   the product to be deleted.
     * @return a {@link CompletionStage} containing the deleted product. If the product supplied was already unpublished
     *          the method will return a completion stage that completed exceptionally.
     */
    @Nonnull
    private static CompletionStage<Product> deleteUnpublishedProduct(@Nonnull final SphereClient ctpClient,
                                                                     @Nonnull final Product product) {
        return ctpClient.execute(ProductDeleteCommand.of(product));
    }

    /**
     * Builds an unpublish request for the supplied product.
     *
     * @param product defines the product to build an un publish request for.
     * @return an unpublish request for the supplied product.
     */
    @Nonnull
    private static SphereRequest<Product> buildUnpublishRequest(@Nonnull final Product product) {
        return ProductUpdateCommand.of(product, Unpublish.of());
    }

    /**
     * Gets the supplied {@link ProductDraft} with the price channel reference attached on its variants' prices.
     *
     * @param productDraft     the product draft to attach the channel reference on its variants' prices.
     * @param channelReference the channel reference to attach on the product draft's variants' prices.
     * @return the product draft with the supplied channel reference attached on the product draft's variants' prices.
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
