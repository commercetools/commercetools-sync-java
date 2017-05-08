package com.commercetools.sync.categories.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeParentUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildChangeOrderHintUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.commons.utils.CustomUpdateActionUtils.buildCustomUpdateActions;

public final class CategorySyncUtils {

    /**
     * TODO: SEE GITHUB ISSUE#12
     * Should transform Category TO CategoryDraft using new generated factory methods mentioned in GH issue #12,
     *
     * <p>Change to {@code public} once implemented.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by the
     *                    user. For example, custom callbacks to call in case of warnings or errors occurring on the
     *                    build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    private static List<UpdateAction<Category>> buildActions(@Nonnull final Category oldCategory,
                                                             @Nonnull final Category newCategory,
                                                             @Nonnull final CategorySyncOptions syncOptions) {
        final List<UpdateAction<Category>> updateActions = buildCoreActions(oldCategory, newCategory, syncOptions);
        final List<UpdateAction<Category>> assetUpdateActions =
          buildAssetActions(oldCategory, newCategory, syncOptions);
        updateActions.addAll(assetUpdateActions);
        return filterUpdateActions(updateActions, syncOptions.getUpdateActionsFilter());
    }

    /**
     * TODO: SEE GITHUB ISSUE#12
     * Should transform Category TO CategoryDraft using new generated factory methods mentioned in GH issue #12,
     * then call {@link CategorySyncUtils#buildCoreActions(Category, Category, CategorySyncOptions)}.
     *
     * <p>Change to {@code public} once implemented.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions} for
     *                    more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    private static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category oldCategory,
                                                                 @Nonnull final Category newCategory,
                                                                 @Nonnull final CategorySyncOptions syncOptions) {
        return new ArrayList<>();
    }

    /**
     * TODO: SEE GITHUB ISSUE#3 and #12
     * Should transform Category TO CategoryDraft using new generated factory methods mentioned in GH issue #12,
     * then call {@link CategorySyncUtils#buildAssetActions(Category, Category, CategorySyncOptions)}.
     * Change to {@code public} once implemented.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions} for
     *                    more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    private static List<UpdateAction<Category>> buildAssetActions(@Nonnull final Category oldCategory,
                                                                  @Nonnull final Category newCategory,
                                                                  @Nonnull final CategorySyncOptions syncOptions) {
        return new ArrayList<>();
    }

    /**
     * Applies a given filter function, if not null, {@code updateActionsFilter} on {@link List} of {@link UpdateAction}
     * elements.
     *
     * @param updateActions       the list of update actions to apply the filter on.
     * @param updateActionsFilter the filter functions to apply on the list of update actions
     * @return a new resultant list from applying the filter function, if not null, on the supplied list. If the filter
     *      function supplied was null, the same supplied list is returned as is.
     */
    @Nonnull
    private static List<UpdateAction<Category>> filterUpdateActions(
      @Nonnull final List<UpdateAction<Category>> updateActions,
      @Nullable final Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> updateActionsFilter) {
        return updateActionsFilter != null ? updateActionsFilter.apply(updateActions) : updateActions;
    }

    /**
     * Compares the Name, Slug, Description, Parent, OrderHint, MetaTitle, MetaDescription, MetaKeywords and Custom
     * fields/ type fields and assets of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
     * {@link UpdateAction&lt;Category&gt;} as a result. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link List} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @param typeService responsible for fetching the key of the old resource type from it's cache.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildActions(@Nonnull final Category oldCategory,
                                                            @Nonnull final CategoryDraft newCategory,
                                                            @Nonnull final CategorySyncOptions syncOptions,
                                                            @Nonnull final TypeService typeService) {
        final List<UpdateAction<Category>> updateActions =
          buildCoreActions(oldCategory, newCategory, syncOptions, typeService);
        final List<UpdateAction<Category>> assetUpdateActions =
          buildAssetActions(oldCategory, newCategory, syncOptions);
        updateActions.addAll(assetUpdateActions);
        return filterUpdateActions(updateActions, syncOptions.getUpdateActionsFilter());
    }

    /**
     * Compares the Name, Slug, Description, Parent, OrderHint, MetaTitle, MetaDescription, MetaKeywords and Custom
     * fields/ type fields of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
     * {@link UpdateAction&lt;Category&gt;} as a result. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link List} is returned.
     *
     * @param oldCategory the category which should be updated.
     * @param newCategory the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @param typeService responsible for fetching the key of the old resource type from it's cache.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Category>> buildCoreActions(@Nonnull final Category oldCategory,
                                                                @Nonnull final CategoryDraft newCategory,
                                                                @Nonnull final CategorySyncOptions syncOptions,
                                                                @Nonnull final TypeService typeService) {
        final List<UpdateAction<Category>> updateActions = buildUpdateActionsFromOptionals(Arrays.asList(
          buildChangeNameUpdateAction(oldCategory, newCategory),
          buildChangeSlugUpdateAction(oldCategory, newCategory),
          buildSetDescriptionUpdateAction(oldCategory, newCategory, syncOptions),
          buildChangeParentUpdateAction(oldCategory, newCategory, syncOptions),
          buildChangeOrderHintUpdateAction(oldCategory, newCategory, syncOptions),
          buildSetMetaTitleUpdateAction(oldCategory, newCategory),
          buildSetMetaDescriptionUpdateAction(oldCategory, newCategory),
          buildSetMetaKeywordsUpdateAction(oldCategory, newCategory)
        ));
        final List<UpdateAction<Category>> categoryCustomUpdateActions =
          buildCustomUpdateActions(oldCategory, newCategory, syncOptions, typeService);
        updateActions.addAll(categoryCustomUpdateActions);
        return updateActions;
    }

    /**
     * Given a list of category {@link UpdateAction} elements, where each is wrapped in an {@link Optional}; this method
     * filters out the optionals which are only present and returns a new list of of category {@link UpdateAction}
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
}
