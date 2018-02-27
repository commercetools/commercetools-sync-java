package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.commons.utils.CustomUpdateActionUtils;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.categories.commands.updateactions.SetAssetDescription;
import io.sphere.sdk.categories.commands.updateactions.SetAssetSources;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static java.util.Arrays.asList;

//TODO: CONSIDER CHANGE NAME.
// TODO: Add tests.
public final class CategoryAssetUpdateActionUtils {

    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Asset oldAsset,
                                                            @Nonnull final AssetDraft newAsset,
                                                            @Nonnull final CategorySyncOptions syncOptions) {

        final List<UpdateAction<Category>> updateActions = buildUpdateActionsFromOptionals(asList(
            buildChangeAssetNameUpdateAction(oldAsset, newAsset),
            buildSetAssetDescriptionUpdateAction(oldAsset, newAsset),
            buildSetAssetTagsUpdateAction(oldAsset, newAsset),
            buildSetAssetSourcesUpdateAction(oldAsset, newAsset)
        ));

        updateActions.addAll(buildCustomUpdateActions(oldAsset, newAsset, syncOptions));
        return updateActions;
    }

    /**
     * TODO: THIS WILL BE REMOVED AS SOON AS GITHUB ISSUE#255 is resolved.
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
    private static List<UpdateAction<Category>> buildUpdateActionsFromOptionals(
        @Nonnull final List<Optional<UpdateAction<Category>>> optionalUpdateActions) {
        return optionalUpdateActions.stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeAssetNameUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getName(), newAsset.getName(),
            () -> ChangeAssetName.ofKey(oldAsset.getKey(), newAsset.getName()));
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetDescriptionUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getDescription(), newAsset.getDescription(),
            () -> SetAssetDescription.ofKey(oldAsset.getKey(), newAsset.getDescription()));
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetTagsUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getTags(), newAsset.getTags(),
            () -> SetAssetTags.ofKey(oldAsset.getKey(), newAsset.getTags()));
    }

    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetSourcesUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getSources(), newAsset.getSources(),
            () -> SetAssetSources.ofKey(oldAsset.getKey(), newAsset.getSources()));
    }

    @Nonnull
    public static List<UpdateAction<Category>> buildCustomUpdateActions(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset,
        @Nonnull final CategorySyncOptions syncOptions) {
        return CustomUpdateActionUtils.buildCustomUpdateActions(oldAsset, newAsset,
            new AssetCustomActionBuilder(), -1, Asset::getId, asset -> Asset.resourceTypeId(), Asset::getKey,
            syncOptions);
    }

    private CategoryAssetUpdateActionUtils() {
    }
}
