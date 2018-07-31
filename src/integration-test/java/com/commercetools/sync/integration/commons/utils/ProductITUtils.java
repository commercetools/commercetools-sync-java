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
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.ChannelITUtils.deleteChannels;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.deleteCustomerGroups;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndCompose;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.deleteTaxCategories;
import static java.util.Optional.ofNullable;
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
        deleteChannels(ctpClient);
        deleteStates(ctpClient, StateType.PRODUCT_STATE);
        deleteTaxCategories(ctpClient);
        deleteCustomerGroups(ctpClient);
    }

    /**
     * Unpublishes all published products, then deletes all products from the CTP project defined by the
     * {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the products from.
     */
    public static void deleteAllProducts(@Nonnull final SphereClient ctpClient) {
        queryAndCompose(ctpClient, ProductQuery.of(), product -> safeDeleteProduct(ctpClient, product));
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
    private static CompletionStage<Product> safeDeleteProduct(@Nonnull final SphereClient ctpClient,
                                                              @Nonnull final Product product) {
        return product.getMasterData().isPublished()
            ? unpublishAndDeleteProduct(ctpClient, product) : deleteProduct(ctpClient, product);
    }

    /**
     * Issues an unpublish request followed by a delete.
     *
     * @param ctpClient defines the CTP project to delete the product from.
     * @param product the product to be unpublished and deleted.
     * @return a {@link CompletionStage} containing the deleted product.
     */
    @Nonnull
    private static CompletionStage<Product> unpublishAndDeleteProduct(@Nonnull final SphereClient ctpClient,
                                                                      @Nonnull final Product product) {
        return ctpClient.execute(buildUnpublishRequest(product))
                        .thenCompose(unpublishedProduct ->
                            deleteProduct(ctpClient, unpublishedProduct));
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
    private static CompletionStage<Product> deleteProduct(@Nonnull final SphereClient ctpClient,
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
     * Gets the supplied {@link ProductDraft} with the price reference attached on all its variants' prices.
     *
     * <p>TODO: GITHUB ISSUE#152
     *
     * @param productDraft     the product draft to attach the channel reference on its variants' prices.
     * @param channelReference the channel reference to attach on the product draft's variants' prices.
     * @return the product draft with the supplied references attached on the product draft's variants' prices.
     */
    public static ProductDraft getDraftWithPriceReferences(@Nonnull final ProductDraft productDraft,
                                                           @Nullable final Reference<Channel> channelReference,
                                                           @Nullable final CustomFieldsDraft customFieldsDraft) {
        final List<ProductVariantDraft> allVariants = productDraft
            .getVariants().stream().map(productVariant -> {
                final List<PriceDraft> priceDraftsWithChannelReferences = getPriceDraftsWithReferences(productVariant,
                    channelReference, customFieldsDraft);
                return ProductVariantDraftBuilder.of(productVariant)
                                                 .prices(priceDraftsWithChannelReferences)
                                                 .build();
            })
            .collect(toList());

        return ofNullable(productDraft.getMasterVariant())
            .map(masterVariant -> {
                final List<PriceDraft> priceDraftsWithReferences = getPriceDraftsWithReferences(masterVariant,
                    channelReference, customFieldsDraft);
                final ProductVariantDraft masterVariantWithPriceDrafts =
                    ProductVariantDraftBuilder.of(masterVariant)
                                              .prices(priceDraftsWithReferences)
                                              .build();

                return ProductDraftBuilder.of(productDraft)
                                          .masterVariant(masterVariantWithPriceDrafts)
                                          .variants(allVariants)
                                          .build();
            })
            .orElse(ProductDraftBuilder.of(productDraft).variants(allVariants).build());
    }

    /**
     * Builds a list of {@link PriceDraft} elements which are identical to the supplied {@link ProductVariantDraft}'s
     * list of prices and sets the channel and custom type references on the prices if they are not null.
     *
     * <p>TODO: GITHUB ISSUE#152
     *
     * @param productVariant the product variant to create an identical price list from.
     * @param channelReference the channel reference to set on the resulting price drafts.
     * @param customFieldsDraft the custom fields to set on the resulting price drafts.
     * @return a list of {@link PriceDraft} elements which are identical to the supplied {@link ProductVariantDraft}'s
     *         list of prices and sets the channel and custom type references on the prices if they are not null.
     */
    @Nonnull
    private static List<PriceDraft> getPriceDraftsWithReferences(
        @Nonnull final ProductVariantDraft productVariant,
        @Nullable final Reference<Channel> channelReference,
        @Nullable final CustomFieldsDraft customFieldsDraft) {

        return productVariant.getPrices()
                             .stream()
                             .map(PriceDraftBuilder::of)
                             .map(priceDraftBuilder -> ofNullable(channelReference).map(priceDraftBuilder::channel)
                                                                                   .orElse(priceDraftBuilder))
                             .map(priceDraftBuilder -> ofNullable(customFieldsDraft).map(priceDraftBuilder::custom)
                                                                                    .orElse(priceDraftBuilder))
                             .map(PriceDraftBuilder::build)
                             .collect(toList());
    }

    /**
     * This method blocks to create a price custom Type on the CTP project defined by the supplied
     * {@code ctpClient}, with the supplied data.
     * @param typeKey   the type key
     * @param locale    the locale to be used for specifying the type name and field definitions names.
     * @param name      the name of the custom type.
     * @param ctpClient defines the CTP project to create the type on.
     */
    public static Type createPricesCustomType(
        @Nonnull final String typeKey,
        @Nonnull final Locale locale,
        @Nonnull final String name,
        @Nonnull final SphereClient ctpClient) {

        return createTypeIfNotAlreadyExisting(
            typeKey, locale, name, ResourceTypeIdsSetBuilder.of().addPrices(), ctpClient);
    }
}
