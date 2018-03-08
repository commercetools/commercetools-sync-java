package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.Arrays.asList;

public final class ProductVariantAssetUpdateActionUtils {

    /**
     * Compares all the fields of an {@link Asset} and an {@link AssetDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Asset} and the {@link AssetDraft}
     * have identical fields, then no update action is needed and hence an empty {@link List} is returned.
     *
     * @param variantId   the variantId needed for building the update action.
     * @param oldAsset    the asset which should be updated.
     * @param newAsset    the asset draft where we get the new fields.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the error callback within the utility, in case of errors.
     * @return A list with the update actions or an empty list if the asset fields are identical.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildActions(
        @Nonnull final Integer variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset,
        @Nonnull final ProductSyncOptions syncOptions) {

        final List<UpdateAction<Product>> updateActions = buildUpdateActionsFromOptionals(asList(
            buildChangeAssetNameUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetDescriptionUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetTagsUpdateAction(variantId, oldAsset, newAsset),
            buildSetAssetSourcesUpdateAction(variantId, oldAsset, newAsset)
        ));

        updateActions.addAll(buildCustomUpdateActions(variantId, oldAsset, newAsset, syncOptions));
        return updateActions;
    }

    /**
     * TODO: THIS WILL BE REMOVED AS SOON AS GITHUB ISSUE#255 is resolved.
     * Given a list of product {@link UpdateAction}s, where each is wrapped in an {@link Optional}; this method
     * filters out the optionals which are only present and returns a new list of category {@link UpdateAction}
     * elements.
     *
     * @param optionalUpdateActions list of product {@link UpdateAction} where each is wrapped in an {@link Optional}.
     * @return a List of product update actions from the optionals that were present in the
     *         {@code optionalUpdateActions} list parameter.
     */
    @Nonnull
    private static List<UpdateAction<Product>> buildUpdateActionsFromOptionals(
        @Nonnull final List<Optional<UpdateAction<Product>>> optionalUpdateActions) {
        return optionalUpdateActions.stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());
    }

    /**
     * Compares the {@link LocalizedString} names of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same name, then no update action is needed and hence an empty {@link Optional}
     * is returned.
     *
     * @param variantId the variantId needed for building the update action.
     * @param oldAsset  the asset which should be updated.
     * @param newAsset  the asset draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildChangeAssetNameUpdateAction(
        @Nonnull final Integer variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getName(), newAsset.getName(),
            () -> ChangeAssetName.ofAssetKeyAndVariantId(variantId, oldAsset.getKey(),
                newAsset.getName(), true));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same description, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * @param variantId the variantId needed for building the update action.
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetAssetDescriptionUpdateAction(
        @Nonnull final Integer variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getDescription(), newAsset.getDescription(),
            () -> SetAssetDescription.ofVariantIdAndAssetKey(variantId, oldAsset.getKey(),
                newAsset.getDescription(), true));
    }

    /**
     * Compares the tags of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same tags, then no update action is needed and hence an empty {@link Optional} is
     * returned.
     *
     * @param variantId the variantId needed for building the update action.
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new tags.
     * @return A filled optional with the update action or an empty optional if the tags are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetAssetTagsUpdateAction(
        @Nonnull final Integer variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getTags(), newAsset.getTags(),
            () -> SetAssetTags.ofVariantIdAndAssetKey(variantId, oldAsset.getKey(),
                newAsset.getTags(), true));
    }

    /**
     * Compares the sources of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same sources, then no update action is needed and hence an empty {@link Optional}
     * is returned.
     *
     * @param variantId the variantId needed for building the update action.
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new sources.
     * @return A filled optional with the update action or an empty optional if the sources are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetAssetSourcesUpdateAction(
        @Nonnull final Integer variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getSources(), newAsset.getSources(),
            () -> SetAssetSources.ofVariantIdAndAssetKey(variantId, oldAsset.getKey(),
                newAsset.getSources(), true));
    }

    /**
     * Compares the custom fields and custom types of an {@link Asset} and an {@link AssetDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result. If both the {@link Asset} and the {@link AssetDraft}
     * have identical custom fields and types, then no update action is needed and hence an empty {@link List} is
     * returned.
     *
     * @param variantId   the variantId needed for building the update action.
     * @param oldAsset    the asset which should be updated.
     * @param newAsset    the asset draft where we get the new custom fields and types.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the error callback within the utility, in case of errors.
     * @return A list with the custom field/type update actions or an empty list if the custom fields/types are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildCustomUpdateActions(
        @Nonnull final Integer variantId,
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset,
        @Nonnull final ProductSyncOptions syncOptions) {

        return CustomUpdateActionUtils.buildCustomUpdateActions(
            oldAsset,
            newAsset,
            new AssetCustomActionBuilder(),
            variantId,
            Asset::getId,
            asset -> Asset.resourceTypeId(),
            Asset::getKey,
            syncOptions);
    }

    private ProductVariantAssetUpdateActionUtils() {
    }
}
