package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.helpers.CategoryCustomActionBuilder;
import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildAssetsUpdateActions;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeOrderHintUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeParentUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildPrimaryResourceCustomUpdateActions;
import static com.commercetools.sync.commons.utils.OptionalUtils.filterEmptyOptionals;

public final class CategorySyncUtils {
    private static final CategoryCustomActionBuilder categoryCustomActionBuilder =
        new CategoryCustomActionBuilder();

    /**
     * Compares all the fields of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link List} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(
        @Nonnull final Category oldCategory,
        @Nonnull final CategoryDraft newCategory,
        @Nonnull final CategorySyncOptions syncOptions) {

        final List<UpdateAction<Category>> updateActions = filterEmptyOptionals(
            buildChangeNameUpdateAction(oldCategory, newCategory),
            buildChangeSlugUpdateAction(oldCategory, newCategory),
            buildSetExternalIdUpdateAction(oldCategory, newCategory),
            buildSetDescriptionUpdateAction(oldCategory, newCategory),
            buildChangeParentUpdateAction(oldCategory, newCategory, syncOptions),
            buildChangeOrderHintUpdateAction(oldCategory, newCategory, syncOptions),
            buildSetMetaTitleUpdateAction(oldCategory, newCategory),
            buildSetMetaDescriptionUpdateAction(oldCategory, newCategory),
            buildSetMetaKeywordsUpdateAction(oldCategory, newCategory)
        );

        final List<UpdateAction<Category>> categoryCustomUpdateActions =
            buildPrimaryResourceCustomUpdateActions(oldCategory, newCategory, categoryCustomActionBuilder, syncOptions);

        final List<UpdateAction<Category>> assetsUpdateActions =
            buildAssetsUpdateActions(oldCategory, newCategory, syncOptions);

        updateActions.addAll(categoryCustomUpdateActions);
        updateActions.addAll(assetsUpdateActions);

        return updateActions;
    }

    private CategorySyncUtils() {
    }
}
