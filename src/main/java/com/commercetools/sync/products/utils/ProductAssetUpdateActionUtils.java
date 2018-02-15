package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildChangeAssetNameUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantAssetUpdateActionUtils.buildSetAssetTagsUpdateAction;
import static java.util.Arrays.asList;

public final class ProductAssetUpdateActionUtils {

    @Nonnull
    public static List<UpdateAction<Product>> buildActions(
        final int variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset,
        @Nonnull final ProductSyncOptions syncOptions) {
        final List<UpdateAction<Product>> updateActions = buildUpdateActionsFromOptionals(asList(
            buildChangeAssetNameUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetDescriptionUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetTagsUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetSourcesUpdateAction(variantId, oldAsset, newAsset)

        ));

        final List<UpdateAction<Asset>> updateActions1 = buildCustomUpdateActions(oldAsset, newAsset, variantId + "",
            syncOptions);

        updateActions.addAll(assetCustomUpdateActions);
        return updateActions;
    }

    /**
     * TODO: SHOULD BE REUSED FROM CATEGORY SYNC UTILS!!
     * Given a list of category {@link UpdateAction} elements, where each is wrapped in an {@link Optional}; this method
     * filters out the optionals which are only present and returns a new list of category {@link UpdateAction}
     * elements.
     *
     * @param optionalUpdateActions list of category {@link UpdateAction} elements,
     *                              where each is wrapped in an {@link Optional}.
     * @return a List of category update actions from the optionals that were present in the
     * {@code optionalUpdateActions} list parameter.
     */
    @Nonnull
    private static List<UpdateAction<Product>> buildUpdateActionsFromOptionals(
        @Nonnull final List<Optional<UpdateAction<Product>>> optionalUpdateActions) {
        return optionalUpdateActions.stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());
    }
}
