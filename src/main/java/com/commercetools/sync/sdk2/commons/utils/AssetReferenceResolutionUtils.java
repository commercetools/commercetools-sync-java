package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.sync.sdk2.products.models.AssetCustomTypeAdapter;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class AssetReferenceResolutionUtils {

  /**
   * Takes an asset list that is supposed to have all its asset's custom references id's are cached
   * in the map in order to be able to fetch the keys for the custom references. This method returns
   * as a result a {@link java.util.List} of {@link AssetDraft} that has all custom references with
   * keys.
   *
   * <p>Any custom reference that is not in the map(cache) will have its id in place and not
   * replaced by the key.
   *
   * @param assets the list of assets to replace their custom ids with keys.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link AssetDraft} that has all channel references with
   *     keys.
   */
  @Nonnull
  public static List<AssetDraft> mapToAssetDrafts(
      @Nonnull final List<Asset> assets,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return assets.stream()
        .map(
            asset ->
                AssetDraftBuilder.of()
                    .name(asset.getName())
                    .sources(asset.getSources())
                    .description(asset.getDescription())
                    .key(asset.getKey())
                    .tags(asset.getTags())
                    .custom(
                        mapToCustomFieldsDraft(
                            AssetCustomTypeAdapter.of(asset), referenceIdToKeyCache))
                    .build())
        .collect(toList());
  }

  private AssetReferenceResolutionUtils() {}
}
