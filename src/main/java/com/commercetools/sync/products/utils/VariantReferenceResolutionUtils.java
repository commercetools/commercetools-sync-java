package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.AssetReferenceResolutionUtils.mapToAssetDrafts;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.*;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.List;
import java.util.Objects;
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
        .attributes(productVariant.getAttributes())
        .assets(mapToAssetDrafts(productVariant.getAssets(), referenceIdToKeyCache))
        .build();
  }

  @Nonnull
  static List<PriceDraft> mapToPriceDrafts(
      @Nonnull final ProductVariant productVariant,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {

    return PriceUtils.createPriceDraft(productVariant.getPrices(), referenceIdToKeyCache);
  }

  private VariantReferenceResolutionUtils() {}
}
