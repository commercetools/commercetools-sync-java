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
import io.sphere.sdk.models.LocalizedString;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;
import static java.util.Arrays.asList;

public final class CategoryAssetUpdateActionUtils {

    /**
     * Compares all the fields of an {@link Asset} and an {@link AssetDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If both the {@link Asset} and the {@link AssetDraft}
     * have identical fields, then no update action is needed and hence an empty {@link List} is returned.
     *
     * @param oldAsset    the asset which should be updated.
     * @param newAsset    the asset draft where we get the new fields.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the error callback within the utility, in case of errors.
     * @return A list with the update actions or an empty list if the asset fields are identical.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Asset oldAsset,
                                                            @Nonnull final AssetDraft newAsset,
                                                            @Nonnull final CategorySyncOptions syncOptions) {

        final List<UpdateAction<Category>> updateActions = filterEmptyOptionals(
            asList(
                buildChangeAssetNameUpdateAction(oldAsset, newAsset),
                buildSetAssetDescriptionUpdateAction(oldAsset, newAsset),
                buildSetAssetTagsUpdateAction(oldAsset, newAsset),
                buildSetAssetSourcesUpdateAction(oldAsset, newAsset)
            ));

        updateActions.addAll(buildCustomUpdateActions(oldAsset, newAsset, syncOptions));
        return updateActions;
    }


    /**
     * Compares the {@link LocalizedString} names of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same name, then no update action is needed and hence an empty {@link Optional}
     * is returned.
     *
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildChangeAssetNameUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getName(), newAsset.getName(),
            () -> ChangeAssetName.ofKey(oldAsset.getKey(), newAsset.getName()));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same description, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetDescriptionUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getDescription(), newAsset.getDescription(),
            () -> SetAssetDescription.ofKey(oldAsset.getKey(), newAsset.getDescription()));
    }

    /**
     * Compares the tags of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same tags, then no update action is needed and hence an empty {@link Optional} is
     * returned.
     *
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new tags.
     * @return A filled optional with the update action or an empty optional if the tags are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetTagsUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getTags(), newAsset.getTags(),
            () -> SetAssetTags.ofKey(oldAsset.getKey(), newAsset.getTags()));
    }

    /**
     * Compares the sources of an {@link Asset} and an {@link AssetDraft} and returns an
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result in an {@link Optional}. If both the {@link Asset} and
     * the {@link AssetDraft} have the same sources, then no update action is needed and hence an empty {@link Optional}
     * is returned.
     *
     * @param oldAsset the asset which should be updated.
     * @param newAsset the asset draft where we get the new sources.
     * @return A filled optional with the update action or an empty optional if the sources are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Category>> buildSetAssetSourcesUpdateAction(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset) {
        return buildUpdateAction(oldAsset.getSources(), newAsset.getSources(),
            () -> SetAssetSources.ofKey(oldAsset.getKey(), newAsset.getSources()));
    }

    /**
     * Compares the custom fields and custom types of an {@link Asset} and an {@link AssetDraft} and returns a list of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If both the {@link Asset} and the {@link AssetDraft}
     * have identical custom fields and types, then no update action is needed and hence an empty {@link List} is
     * returned.
     *
     * @param oldAsset    the asset which should be updated.
     * @param newAsset    the asset draft where we get the new custom fields and types.
     * @param syncOptions responsible for supplying the sync options to the sync utility method. It is used for
     *                    triggering the error callback within the utility, in case of errors.
     * @return A list with the custom field/type update actions or an empty list if the custom fields/types are
     *         identical.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCustomUpdateActions(
        @Nonnull final Asset oldAsset,
        @Nonnull final AssetDraft newAsset,
        @Nonnull final CategorySyncOptions syncOptions) {

        return CustomUpdateActionUtils.buildCustomUpdateActions(
            oldAsset,
            newAsset,
            new AssetCustomActionBuilder(),
            -1,
            Asset::getId,
            asset -> Asset.resourceTypeId(),
            Asset::getKey,
            syncOptions);
    }

    private CategoryAssetUpdateActionUtils() {
    }
}
