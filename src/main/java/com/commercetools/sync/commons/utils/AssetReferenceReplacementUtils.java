package com.commercetools.sync.commons.utils;

import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceReplacementUtils.replaceCustomTypeIdWithKeys;
import static java.util.stream.Collectors.toList;

public final class AssetReferenceReplacementUtils {

    /**
     * Takes a an asset list that is supposed to have all its assets' custom references expanded in order to be able to
     * fetch the keys and replace the reference ids with the corresponding keys for the custom references. This method
     * returns as a result a {@link List} of {@link AssetDraft} that has all custom references with keys replacing the
     * ids.
     *
     * <p>Any custom reference that is not expanded will have it's id in place and not replaced by the key.
     *
     * @param assets the list of assets to replace their custom ids with keys.
     * @return a {@link List} of {@link AssetDraft} that has all channel references with keys replacing the ids.
     */
    @Nonnull
    public static List<AssetDraft> replaceAssetsReferencesIdsWithKeys(@Nonnull final List<Asset> assets) {
        return assets.stream().map(asset ->
            AssetDraftBuilder.of(asset)
                             .custom(replaceCustomTypeIdWithKeys(asset)).build())
                     .collect(toList());
    }

    private AssetReferenceReplacementUtils() {
    }
}
