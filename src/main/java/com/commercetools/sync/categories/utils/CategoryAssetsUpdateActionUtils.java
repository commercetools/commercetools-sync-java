package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildChangeAssetNameUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildCustomUpdateActions;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildSetAssetDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildSetAssetSourcesUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryAssetUpdateActionUtils.buildSetAssetTagsUpdateAction;
import static java.util.Arrays.asList;

//TODO: CONSIDER CHANGE NAME.
// TODO: Add tests.
public final class CategoryAssetsUpdateActionUtils {

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
    private static List<UpdateAction<Category>> buildUpdateActionsFromOptionals(
        @Nonnull final List<Optional<UpdateAction<Category>>> optionalUpdateActions) {
        return optionalUpdateActions.stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());
    }
}
