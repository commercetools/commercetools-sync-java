package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static java.util.stream.Collectors.toList;

import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class AssetReferenceResolutionUtils {

  /**
   * Takes an asset list that is supposed to have all its assets' custom references expanded in
   * order to be able to fetch the keys for the custom references. This method returns as a result a
   * {@link List} of {@link AssetDraft} that has all custom references with keys.
   *
   * <p>Any custom reference that is not expanded will have its id in place and not replaced by the
   * key.
   *
   * @param assets the list of assets to replace their custom ids with keys.
   * @return a {@link List} of {@link AssetDraft} that has all channel references with keys.
   */
  @Nonnull
  public static List<AssetDraft> mapToAssetDrafts(@Nonnull final List<Asset> assets) {
    return assets.stream()
        .map(asset -> AssetDraftBuilder.of(asset).custom(mapToCustomFieldsDraft(asset)).build())
        .collect(toList());
  }

  private AssetReferenceResolutionUtils() {}
}
