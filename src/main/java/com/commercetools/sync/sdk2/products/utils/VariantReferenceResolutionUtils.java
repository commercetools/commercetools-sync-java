package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static com.commercetools.sync.sdk2.products.utils.PriceUtils.createPriceDraft;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.*;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeReferenceImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    return ProductVariantDraftBuilder.of()
        .sku(productVariant.getSku())
        .key(productVariant.getKey())
        .prices(mapToPriceDrafts(productVariant, referenceIdToKeyCache))
        .attributes(replaceAttributesReferencesIdsWithKeys(productVariant, referenceIdToKeyCache))
        .assets(mapToAssetDrafts(productVariant.getAssets(), referenceIdToKeyCache))
        .build();
  }

  @Nonnull
  static List<PriceDraft> mapToPriceDrafts(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return createPriceDraft(productVariant.getPrices(), referenceIdToKeyCache);
  }

  /**
   * Takes a product variant that is supposed to have all its attribute product references and
   * product set references id's cached(fetch and store key value for the reference id) in order to
   * be able to replace the reference ids with the corresponding keys for the references. This
   * method returns as a result a {@link List} of {@link Attribute} that has all product references
   * with keys replacing the ids.
   *
   * <p>Any product reference that is not cached(reference id is not present in the map) will have
   * it's id in place and not replaced by the key.
   *
   * @param productVariant the product variant to replace its attribute product references ids with
   *     keys.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link List} of {@link Attribute} that has all product references with keys replacing
   *     the ids.
   */
  @Nonnull
  static List<Attribute> replaceAttributesReferencesIdsWithKeys(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final List<Attribute> productTypeReferenceAttributes =
        productVariant.getAttributes().stream()
            .filter(attribute -> attribute.getValue() instanceof ProductTypeReferenceImpl)
            .map(attribute -> (ProductTypeReference) attribute.getValue())
            .map(
                productReference ->
                    getResourceIdentifierWithKey(
                        productReference,
                        referenceIdToKeyCache,
                        (id, key) -> ProductResourceIdentifierBuilder.of().id(id).key(key).build()))
            .map(
                productTypeResourceIdentifier ->
                    AttributeBuilder.of().value(productTypeResourceIdentifier).build())
            .collect(toList());
    final List<Attribute> productSetTypeReferenceAttributes =
        Optional.of(
                productVariant.getAttributes().stream()
                    .filter(
                        attribute ->
                            attribute.getValue() instanceof List
                                && ((List) attribute.getValue())
                                    .stream()
                                        .anyMatch(
                                            setAttr -> setAttr instanceof ProductTypeReferenceImpl))
                    .map(
                        attribute ->
                            AttributeAccessor.asSetReference(attribute).stream()
                                .map(
                                    productReference ->
                                        getResourceIdentifierWithKey(
                                            productReference,
                                            referenceIdToKeyCache,
                                            (id, key) ->
                                                ProductResourceIdentifierBuilder.of()
                                                    .id(id)
                                                    .key(key)
                                                    .build()))
                                .collect(toList()))
                    .map(
                        productTypeResourceIdentifiers ->
                            AttributeBuilder.of().value(productTypeResourceIdentifiers).build())
                    .collect(toList()))
            .orElse(Collections.emptyList());

    final List<Attribute> joined = new ArrayList<>();
    joined.addAll(productTypeReferenceAttributes);
    joined.addAll(productSetTypeReferenceAttributes);
    return joined;
  }

  private VariantReferenceResolutionUtils() {}
}
