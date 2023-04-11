package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.sync.sdk2.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getReferenceWithKeyReplaced;
import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.DiscountedPriceDraftBuilder;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.common.PriceTierDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.ProductReference;
import com.commercetools.api.models.product.ProductReferenceBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.products.models.PriceCustomTypeAdapter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class VariantReferenceResolutionUtils {

  /**
   * Returns an {@link java.util.List}&lt;{@link ProductVariantDraft}&gt; consisting of the results
   * of applying the mapping from {@link ProductVariant} to {@link ProductVariantDraft} with
   * considering reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *        <td>variants.prices.channel</td>
   *        <td>{@link com.commercetools.api.models.channel.ChannelReference}</td>
   *        <td>{@link com.commercetools.api.models.channel.ChannelResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.customerGroup *</td>
   *        <td>{@link com.commercetools.api.models.customer_group.CustomerGroupReference}</td>
   *        <td>{@link com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.assets.custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.attributes on {@link java.util.List}&lt;{@link Attribute} *</td>
   *        <td>{@link com.commercetools.api.models.product_type.ProductTypeReference} (example for ProductType)</td>
   *        <td>{@link com.commercetools.api.models.product_type.ProductTypeReference} (with key replaced with id field)</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should be cached(idToKey value fetched and stored
   * in a map). Any reference that is not cached will have its id in place and not replaced by the
   * key will be considered as existing resources on the target commercetools project and the
   * library will issues an update/create API request without reference resolution.
   *
   * @param productVariants the product variants without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link ProductVariantDraft} built from the supplied {@link
   *     java.util.List} of {@link ProductVariant}.
   */
  @Nonnull
  public static List<ProductVariantDraft> mapToProductVariantDrafts(
      @Nonnull final List<ProductVariant> productVariants,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return productVariants.stream()
        .filter(Objects::nonNull)
        .map(variants -> mapToProductVariantDraft(variants, referenceIdToKeyCache))
        .collect(toList());
  }

  @Nonnull
  private static ProductVariantDraft mapToProductVariantDraft(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return ProductVariantDraftBuilder.of(toVariantDraft(productVariant))
        .prices(mapToPriceDrafts(productVariant, referenceIdToKeyCache))
        .attributes(replaceAttributesReferencesIdsWithKeys(productVariant, referenceIdToKeyCache))
        .assets(mapToAssetDrafts(productVariant.getAssets(), referenceIdToKeyCache))
        .build();
  }

  @Nonnull
  static List<PriceDraft> mapToPriceDrafts(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return productVariant.getPrices().stream()
        .map(
            price ->
                PriceDraftBuilder.of(toPriceDraft(price))
                    .custom(
                        mapToCustomFieldsDraft(
                            PriceCustomTypeAdapter.of(price), referenceIdToKeyCache))
                    .channel(
                        getResourceIdentifierWithKey(price.getChannel(), referenceIdToKeyCache))
                    .customerGroup(
                        getResourceIdentifierWithKey(
                            price.getCustomerGroup(), referenceIdToKeyCache))
                    .build())
        .collect(toList());
  }

  public static ProductVariantDraft toVariantDraft(final ProductVariant productVariant) {
    return ProductVariantDraftBuilder.of()
        .prices(
            Optional.ofNullable(productVariant.getPrices())
                .map(
                    prices ->
                        prices.stream()
                            .map(VariantReferenceResolutionUtils::toPriceDraft)
                            .collect(toList()))
                .orElse(null))
        .key(productVariant.getKey())
        .attributes(productVariant.getAttributes())
        .images(productVariant.getImages())
        .sku(productVariant.getSku())
        .assets(
            Optional.ofNullable(productVariant.getAssets())
                .map(
                    assets ->
                        assets.stream()
                            .map(
                                asset ->
                                    AssetDraftBuilder.of()
                                        .tags(asset.getTags())
                                        .key(asset.getKey())
                                        .description(asset.getDescription())
                                        .sources(asset.getSources())
                                        .custom(
                                            Optional.ofNullable(asset.getCustom())
                                                .flatMap(
                                                    custom ->
                                                        Optional.ofNullable(custom.toDraftBuilder())
                                                            .map(CustomFieldsDraftBuilder::build))
                                                .orElse(null))
                                        .name(asset.getName())
                                        .build())
                            .collect(toList()))
                .orElse(null))
        .build();
  }

  private static PriceDraft toPriceDraft(final Price price) {
    return PriceDraftBuilder.of()
        .value(price.getValue())
        .channel(
            Optional.ofNullable(price.getChannel())
                .map(channel -> ChannelResourceIdentifierBuilder.of().id(channel.getId()).build())
                .orElse(null))
        .key(price.getKey())
        .country(price.getCountry())
        .custom(
            Optional.ofNullable(price.getCustom())
                .map(
                    customFields ->
                        CustomFieldsDraftBuilder.of()
                            .type(price.getCustom().getType().toResourceIdentifier())
                            .fields(price.getCustom().getFields())
                            .build())
                .orElse(null))
        .customerGroup(
            Optional.ofNullable(price.getCustomerGroup())
                .map(
                    customerGroup ->
                        CustomerGroupResourceIdentifierBuilder.of()
                            .id(customerGroup.getId())
                            .build())
                .orElse(null))
        .discounted(
            Optional.ofNullable(price.getDiscounted())
                .map(
                    discounted ->
                        DiscountedPriceDraftBuilder.of()
                            .discount(discounted.getDiscount())
                            .value(discounted.getValue())
                            .build())
                .orElse(null))
        .tiers(
            Optional.ofNullable(price.getTiers())
                .map(
                    tiers ->
                        tiers.stream()
                            .map(
                                priceTier ->
                                    PriceTierDraftBuilder.of()
                                        .value(priceTier.getValue())
                                        .minimumQuantity(priceTier.getMinimumQuantity())
                                        .build())
                            .collect(toList()))
                .orElse(null))
        .validFrom(price.getValidFrom())
        .validUntil(price.getValidUntil())
        .build();
  }

  /**
   * Takes a product variant that is supposed to have all its attribute product references and
   * product set references id's cached(fetch and store key value for the reference id) in order to
   * be able to replace the reference ids with the corresponding keys for the references. This
   * method returns as a result a {@link java.util.List} of {@link Attribute} that has all product
   * references with keys replacing the ids.
   *
   * <p>Any product reference that is not cached(reference id is not present in the map) will have
   * it's id in place and not replaced by the key.
   *
   * @param productVariant the product variant to replace its attribute product references ids with
   *     keys.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link Attribute} that has all product references with keys
   *     replacing the ids.
   */
  @Nonnull
  static List<Attribute> replaceAttributesReferencesIdsWithKeys(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return productVariant.getAttributes().stream()
        .map(
            attribute -> {
              final AttributeBuilder attributeBuilder =
                  replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache)
                      .map(
                          productReference -> {
                            return AttributeBuilder.of()
                                .name(attribute.getName())
                                .value(productReference);
                          })
                      .orElseGet(
                          () ->
                              replaceAttributeReferenceSetIdsWithKeys(
                                      attribute, referenceIdToKeyCache)
                                  .map(
                                      productReferenceSet ->
                                          AttributeBuilder.of()
                                              .name(attribute.getName())
                                              .value(productReferenceSet))
                                  .orElseGet(
                                      () ->
                                          AttributeBuilder.of()
                                              .name(attribute.getName())
                                              .value(attribute.getValue())));

              return attributeBuilder.build();
            })
        .collect(toList());
  }

  static Optional<ProductReference> replaceAttributeReferenceIdWithKey(
      @Nonnull final Attribute attribute,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return getProductReference(attribute)
        .map(
            productReference ->
                getReferenceWithKeyReplaced(
                    productReference,
                    () ->
                        ProductReferenceBuilder.of()
                            .id(referenceIdToKeyCache.get(productReference.getId()))
                            .build(),
                    referenceIdToKeyCache));
  }

  private static Optional<ProductReference> getProductReference(
      @Nonnull final Attribute attribute) {
    return Optional.of(attribute)
        .filter(VariantReferenceResolutionUtils::isProductReference)
        .map(productReferenceAttribute -> (ProductReference) productReferenceAttribute.getValue());
  }

  static Optional<Set<ProductReference>> replaceAttributeReferenceSetIdsWithKeys(
      @Nonnull final Attribute attribute,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return getProductReferenceSet(attribute)
        .map(
            productReferenceSet ->
                productReferenceSet.stream()
                    .map(
                        productReference ->
                            getReferenceWithKeyReplaced(
                                productReference,
                                () ->
                                    ProductReferenceBuilder.of()
                                        .id(referenceIdToKeyCache.get(productReference.getId()))
                                        .build(),
                                referenceIdToKeyCache))
                    .collect(toSet()));
  }

  private static Optional<Set<ProductReference>> getProductReferenceSet(
      @Nonnull final Attribute attribute) {
    return Optional.of(attribute)
        .filter(VariantReferenceResolutionUtils::isProductReferenceSet)
        .map(
            productReferenceSetAttribute ->
                (Set<ProductReference>) productReferenceSetAttribute.getValue());
  }

  static boolean isProductReference(@Nonnull final Attribute attribute) {
    return attribute.getValue() instanceof ProductReference;
  }

  static boolean isProductReferenceSet(@Nonnull final Attribute attribute) {
    final boolean isSet = attribute.getValue() instanceof Collection;
    if (isSet) {
      final Collection<Object> collection = (Collection<Object>) attribute.getValue();
      final Iterator<Object> iterator = collection.iterator();

      if (iterator.hasNext()) {
        return iterator.next() instanceof ProductReference;
      }
    }

    return false;
  }

  private VariantReferenceResolutionUtils() {}
}
