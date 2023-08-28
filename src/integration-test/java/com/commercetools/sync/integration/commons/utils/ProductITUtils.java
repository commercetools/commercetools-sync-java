package com.commercetools.sync.integration.commons.utils;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelPagedQueryResponse;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.DiscountedPrice;
import com.commercetools.api.models.common.DiscountedPriceDraft;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.PriceTier;
import com.commercetools.api.models.common.PriceTierDraft;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductUpdateActionBuilder;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.neovisionaries.i18n.CountryCode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.money.CurrencyUnit;

public final class ProductITUtils {

  public static final String CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY =
      "commercetools-sync-java.UnresolvedReferencesService.productDrafts";

  /**
   * Deletes all products, product types, categories and types from the CTP project defined by the
   * {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the product types from.
   */
  public static void deleteProductSyncTestData(@Nonnull final ProjectApiRoot ctpClient) {
    deleteAllProducts(ctpClient);
    ProductTypeITUtils.deleteProductTypes(ctpClient);
    CategoryITUtils.deleteAllCategories(ctpClient);
    ITUtils.deleteTypes(ctpClient);
    ChannelITUtils.deleteChannels(ctpClient);
    StateITUtils.deleteStates(ctpClient, null);
    TaxCategoryITUtils.deleteTaxCategories(ctpClient);
    CustomerGroupITUtils.deleteCustomerGroups(ctpClient);
    CustomerITUtils.deleteCustomers(ctpClient);
    CustomObjectITUtils.deleteWaitingToBeResolvedCustomObjects(
        ctpClient, CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY);
  }

  /**
   * Unpublishes all published products, then deletes all products from the CTP project defined by
   * the {@code ctpClient}.
   *
   * @param ctpClient defines the CTP project to delete the products from.
   */
  public static void deleteAllProducts(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
            ctpClient.products().get(),
            products -> {
              CompletableFuture.allOf(
                      products.stream()
                          .map(product -> safeDeleteProduct(ctpClient, product))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  /**
   * If the {@code product} is published, issues an unpublish request followed by a delete.
   * Otherwise, issues a delete request right away.
   *
   * @param ctpClient defines the CTP project to delete the product from.
   * @param product the product to be deleted.
   * @return a {@link java.util.concurrent.CompletionStage} containing the deleted product.
   */
  @Nonnull
  private static CompletionStage<Product> safeDeleteProduct(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final Product product) {
    return product.getMasterData().getPublished()
        ? unpublishAndDeleteProduct(ctpClient, product)
        : deleteProduct(ctpClient, product);
  }

  /**
   * Issues an unpublish request followed by a delete.
   *
   * @param ctpClient defines the CTP project to delete the product from.
   * @param product the product to be unpublished and deleted.
   * @return a {@link java.util.concurrent.CompletionStage} containing the deleted product.
   */
  @Nonnull
  private static CompletionStage<Product> unpublishAndDeleteProduct(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final Product product) {
    return ctpClient
        .products()
        .update(product)
        .with(builder -> builder.plus(ProductUpdateActionBuilder::unpublishBuilder))
        .execute()
        .thenApply(productApiHttpResponse -> productApiHttpResponse.getBody())
        .thenCompose(unpublishedProduct -> deleteProduct(ctpClient, unpublishedProduct));
  }

  /**
   * Note: This method assumes the product is unpublished.
   *
   * @param ctpClient the client defining the CTP project to delete the product from.
   * @param product the product to be deleted.
   * @return a {@link java.util.concurrent.CompletionStage} containing the deleted product. If the
   *     product supplied was already unpublished the method will return a completion stage that
   *     completed exceptionally.
   */
  @Nonnull
  private static CompletionStage<Product> deleteProduct(
      @Nonnull final ProjectApiRoot ctpClient, @Nonnull final Product product) {
    return ctpClient.products().delete(product).execute().thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Gets the supplied {@link ProductDraft} with the price reference attached on all its variants'
   * prices.
   *
   * @param productDraft the product draft to attach the channel reference on its variants' prices.
   * @param channelReference the channel reference to attach on the product draft's variants'
   *     prices.
   * @return the product draft with the supplied references attached on the product draft's
   *     variants' prices.
   */
  public static ProductDraft getDraftWithPriceReferences(
      @Nonnull final ProductDraft productDraft,
      @Nullable final ChannelResourceIdentifier channelReference,
      @Nullable final CustomFieldsDraft customFieldsDraft) {
    final List<ProductVariantDraft> allVariants =
        productDraft.getVariants().stream()
            .map(
                productVariant -> {
                  final List<PriceDraft> priceDraftsWithChannelReferences =
                      getPriceDraftsWithReferences(
                          productVariant, channelReference, customFieldsDraft);
                  return ProductVariantDraft.builder(productVariant)
                      .prices(priceDraftsWithChannelReferences)
                      .build();
                })
            .collect(toList());

    return ofNullable(productDraft.getMasterVariant())
        .map(
            masterVariant -> {
              final List<PriceDraft> priceDraftsWithReferences =
                  getPriceDraftsWithReferences(masterVariant, channelReference, customFieldsDraft);
              final ProductVariantDraft masterVariantWithPriceDrafts =
                  ProductVariantDraftBuilder.of(masterVariant)
                      .prices(priceDraftsWithReferences)
                      .build();

              return ProductDraft.builder(productDraft)
                  .masterVariant(masterVariantWithPriceDrafts)
                  .variants(allVariants)
                  .build();
            })
        .orElse(ProductDraftBuilder.of(productDraft).variants(allVariants).build());
  }

  /**
   * Builds a list of {@link PriceDraft} elements which are identical to the supplied {@link
   * ProductVariantDraft}'s list of prices and sets the channel and custom type references on the
   * prices if they are not null.
   *
   * @param productVariant the product variant to create an identical price list from.
   * @param channelReference the channel reference to set on the resulting price drafts.
   * @param customFieldsDraft the custom fields to set on the resulting price drafts.
   * @return a list of {@link PriceDraft} elements which are identical to the supplied {@link
   *     ProductVariantDraft}'s list of prices and sets the channel and custom type references on
   *     the prices if they are not null.
   */
  @Nonnull
  private static List<PriceDraft> getPriceDraftsWithReferences(
      @Nonnull final ProductVariantDraft productVariant,
      @Nullable final ChannelResourceIdentifier channelReference,
      @Nullable final CustomFieldsDraft customFieldsDraft) {

    return productVariant.getPrices().stream()
        .map(PriceDraftBuilder::of)
        .map(
            priceDraftBuilder ->
                ofNullable(channelReference)
                    .map(priceDraftBuilder::channel)
                    .orElse(priceDraftBuilder))
        .map(
            priceDraftBuilder ->
                ofNullable(customFieldsDraft)
                    .map(priceDraftBuilder::custom)
                    .orElse(priceDraftBuilder))
        .map(PriceDraftBuilder::build)
        .collect(toList());
  }

  /**
   * This method blocks to create a price custom Type on the CTP project defined by the supplied
   * {@code ctpClient}, with the supplied data.
   *
   * @param typeKey the type key
   * @param locale the locale to be used for specifying the type name and field definitions names.
   * @param name the name of the custom type.
   * @param ctpClient defines the CTP project to create the type on.
   */
  public static Type ensurePricesCustomType(
      @Nonnull final String typeKey,
      @Nonnull final Locale locale,
      @Nonnull final String name,
      @Nonnull final ProjectApiRoot ctpClient) {

    return ITUtils.createTypeIfNotAlreadyExisting(
        typeKey, locale, name, Collections.singletonList(ResourceTypeId.PRODUCT_PRICE), ctpClient);
  }

  /**
   * Builds a {@link PriceDraft} element
   *
   * @param amount the amount to create a price draft.
   * @param currencyUnits the currency unit of the amount.
   * @param customerGroupId the customer Group Id to create the reference.
   * @param validFrom the date from when it is valid.
   * @param validUntil the date until when it is valid.
   * @param channelId the channel Id to create the reference.
   * @param customFieldsDraft the custom fields to set on the resulting price draft.
   * @param discountedPrice a {@link DiscountedPrice} to the price draft.
   * @param priceTiers a list of {@link PriceTier}.
   * @return a {@link PriceDraft} element
   */
  @Nonnull
  public static PriceDraft createPriceDraft(
      @Nonnull final BigDecimal amount,
      @Nonnull final CurrencyUnit currencyUnits,
      @Nonnull final CountryCode countryCode,
      @Nullable final String customerGroupId,
      @Nullable final ZonedDateTime validFrom,
      @Nullable final ZonedDateTime validUntil,
      @Nullable final String channelId,
      @Nullable final CustomFieldsDraft customFieldsDraft,
      @Nullable final DiscountedPriceDraft discountedPrice,
      @Nullable final List<PriceTierDraft> priceTiers) {
    return PriceDraftBuilder.of()
        .value(
            moneyBuilder ->
                moneyBuilder
                    .centAmount(amount.longValue())
                    .currencyCode(currencyUnits.getCurrencyCode()))
        .country(countryCode.getAlpha2())
        .customerGroup(
            customerGroupId == null
                ? null
                : CustomerGroupResourceIdentifierBuilder.of().id(customerGroupId).build())
        .validFrom(validFrom)
        .validUntil(validUntil)
        .channel(
            channelId == null ? null : ChannelResourceIdentifierBuilder.of().id(channelId).build())
        .custom(customFieldsDraft)
        .discounted(discountedPrice)
        .tiers(priceTiers)
        .build();
  }

  public static Channel ensureChannel(ChannelDraft channelDraft, ProjectApiRoot projectApiRoot) {
    Channel channel =
        projectApiRoot
            .channels()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", channelDraft.getKey())
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ChannelPagedQueryResponse::getResults)
            .thenApply(channels -> channels.isEmpty() ? null : channels.get(0))
            .join();

    if (channel == null) {

      channel =
          projectApiRoot
              .channels()
              .create(channelDraft)
              .execute()
              .thenApply(ApiHttpResponse::getBody)
              .toCompletableFuture()
              .join();
    }

    return channel;
  }

  private ProductITUtils() {}
}
