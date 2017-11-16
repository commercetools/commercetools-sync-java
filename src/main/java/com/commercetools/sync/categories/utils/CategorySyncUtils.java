package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.BaseSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeOrderHintUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeParentUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;

public final class CategorySyncUtils {

    /**
     * Compares the Name, Slug, Description, Parent, OrderHint, MetaTitle, MetaDescription, MetaKeywords and Custom
     * fields/ type fields of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link List} is returned. Then it applies a specified filter function in the {@link CategorySyncOptions}
     * instance on the resultant list and returns this result.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category oldCategory,
                                                            @Nonnull final CategoryDraft newCategory,
                                                            @Nonnull final CategorySyncOptions syncOptions) {
        final List<UpdateAction<Category>> updateActions =
            buildCoreActions(oldCategory, newCategory, syncOptions);
        final List<UpdateAction<Category>> assetUpdateActions =
            buildAssetActions(oldCategory, newCategory, syncOptions);
        updateActions.addAll(assetUpdateActions);
        return syncOptions.applyBeforeUpdateCallBack(updateActions, newCategory, oldCategory);
    }

    /**
     * Compares the Name, Slug, externalID, Description, Parent, OrderHint, MetaTitle, MetaDescription, MetaKeywords
     * and Custom fields/ type fields of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
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
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category oldCategory,
                                                                @Nonnull final CategoryDraft newCategory,
                                                                @Nonnull final CategorySyncOptions syncOptions) {
        final List<UpdateAction<Category>> updateActions = buildUpdateActionsFromOptionals(Arrays.asList(
            buildChangeNameUpdateAction(oldCategory, newCategory),
            buildChangeSlugUpdateAction(oldCategory, newCategory),
            buildSetExternalIdUpdateAction(oldCategory, newCategory),
            buildSetDescriptionUpdateAction(oldCategory, newCategory),
            buildChangeParentUpdateAction(oldCategory, newCategory, syncOptions),
            buildChangeOrderHintUpdateAction(oldCategory, newCategory, syncOptions),
            buildSetMetaTitleUpdateAction(oldCategory, newCategory),
            buildSetMetaDescriptionUpdateAction(oldCategory, newCategory),
            buildSetMetaKeywordsUpdateAction(oldCategory, newCategory)
        ));
        final List<UpdateAction<Category>> categoryCustomUpdateActions =
            buildCustomUpdateActions(oldCategory, newCategory, syncOptions);
        updateActions.addAll(categoryCustomUpdateActions);
        return updateActions;
    }

    /**
     * TODO: SEE GITHUB ISSUE#3
     * Change to {@code public} once implemented.
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
    private static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category oldCategory,
                                                                  @Nonnull final CategoryDraft newCategory,
                                                                  @Nonnull final CategorySyncOptions syncOptions) {

        return new ArrayList<>();
    }

    /**
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
