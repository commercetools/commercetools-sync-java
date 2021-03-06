package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static com.commercetools.sync.commons.utils.ResourceIdentifierUtils.REFERENCE_TYPE_ID_FIELD;
import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;
import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.attributes.AttributeAccess;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.types.Type;
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
   * Returns an {@link List}&lt;{@link ProductVariantDraft}&gt; consisting of the results of
   * applying the mapping from {@link ProductVariant} to {@link ProductVariantDraft} with
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
   *        <td>{@link Reference}&lt;{@link Channel}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Channel}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.customerGroup *</td>
   *        <td>{@link Reference}&lt;{@link CustomerGroup}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link CustomerGroup}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.assets.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.attributes on {@link List}&lt;{@link Attribute} *</td>
   *        <td>{@link Reference}&lt;{@link ProductType}&gt; (example for ProductType)</td>
   *        <td>{@link Reference}&lt;{@link ProductType}&gt; (with key replaced with id field)</td>
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
   * @return a {@link List} of {@link ProductVariantDraft} built from the supplied {@link List} of
   *     {@link ProductVariant}.
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
    return ProductVariantDraftBuilder.of(productVariant)
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
                PriceDraftBuilder.of(price)
                    .custom(mapToCustomFieldsDraft(price, referenceIdToKeyCache))
                    .channel(
                        getResourceIdentifierWithKey(price.getChannel(), referenceIdToKeyCache))
                    .customerGroup(
                        getResourceIdentifierWithKey(
                            price.getCustomerGroup(), referenceIdToKeyCache))
                    .build())
        .collect(toList());
  }

  /**
   * Takes a product variant that is supposed to have all its attribute product references and
   * product set references id's cached(fetch and store key value for the reference id) in order to
   * be able to replace the reference ids with the corresponding keys for the references. This
   * method returns as a result a {@link List} of {@link AttributeDraft} that has all product
   * references with keys replacing the ids.
   *
   * <p>Any product reference that is not cached(reference id is not present in the map) will have
   * it's id in place and not replaced by the key.
   *
   * @param productVariant the product variant to replace its attribute product references ids with
   *     keys.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link AttributeDraft} that has all product references with keys
   *     replacing the ids.
   */
  @Nonnull
  static List<AttributeDraft> replaceAttributesReferencesIdsWithKeys(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return productVariant.getAttributes().stream()
        .map(
            attribute ->
                replaceAttributeReferenceIdWithKey(attribute, referenceIdToKeyCache)
                    .map(
                        productReference ->
                            AttributeDraft.of(attribute.getName(), productReference))
                    .orElseGet(
                        () ->
                            replaceAttributeReferenceSetIdsWithKeys(
                                    attribute, referenceIdToKeyCache)
                                .map(
                                    productReferenceSet ->
                                        AttributeDraft.of(attribute.getName(), productReferenceSet))
                                .orElseGet(
                                    () ->
                                        AttributeDraft.of(
                                            attribute.getName(), attribute.getValueAsJsonNode()))))
        .collect(toList());
  }

  @SuppressWarnings(
      "ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
  static Optional<Reference<Product>> replaceAttributeReferenceIdWithKey(
      @Nonnull final Attribute attribute,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return getProductReference(attribute)
        .map(
            productReference ->
                getReferenceWithKeyReplaced(
                    productReference,
                    () ->
                        Product.referenceOfId(referenceIdToKeyCache.get(productReference.getId())),
                    referenceIdToKeyCache));
  }

  private static Optional<Reference<Product>> getProductReference(
      @Nonnull final Attribute attribute) {
    return Optional.of(attribute)
        .filter(VariantReferenceResolutionUtils::isProductReference)
        .map(
            productReferenceAttribute ->
                productReferenceAttribute.getValue(AttributeAccess.ofProductReference()));
  }

  static Optional<Set<Reference<Product>>> replaceAttributeReferenceSetIdsWithKeys(
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
                                    Product.referenceOfId(
                                        referenceIdToKeyCache.get(productReference.getId())),
                                referenceIdToKeyCache))
                    .collect(toSet()));
  }

  private static Optional<Set<Reference<Product>>> getProductReferenceSet(
      @Nonnull final Attribute attribute) {
    return Optional.of(attribute)
        .filter(VariantReferenceResolutionUtils::isProductReferenceSet)
        .map(
            productReferenceSetAttribute ->
                productReferenceSetAttribute.getValue(AttributeAccess.ofProductReferenceSet()));
  }

  static boolean isProductReference(@Nonnull final Attribute attribute) {
    final JsonNode valueAsJsonNode = attribute.getValueAsJsonNode();
    return !(valueAsJsonNode instanceof ArrayNode) && isValueAProductReference(valueAsJsonNode);
  }

  static boolean isProductReferenceSet(@Nonnull final Attribute attribute) {
    final JsonNode valueAsJsonNode = attribute.getValueAsJsonNode();

    if (valueAsJsonNode instanceof ArrayNode) {
      final Iterator<JsonNode> setIterator = valueAsJsonNode.elements();

      if (setIterator.hasNext()) {
        return isValueAProductReference(setIterator.next());
      }
    }

    return false;
  }

  private static boolean isValueAProductReference(@Nonnull final JsonNode valueAsJsonNode) {
    if (valueAsJsonNode.isContainerNode()) {
      final JsonNode typeIdNode = valueAsJsonNode.get(REFERENCE_TYPE_ID_FIELD);
      return typeIdNode != null && Product.referenceTypeId().equals(typeIdNode.asText());
    }
    return false;
  }

  private VariantReferenceResolutionUtils() {}
}
