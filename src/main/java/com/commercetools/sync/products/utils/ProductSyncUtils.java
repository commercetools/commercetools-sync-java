package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveFromCategoryUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetCategoryOrderHintsUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetSearchKeywordsUpdateAction;

public class ProductSyncUtils {
    /**
     * Compares the Name, Slug, Description, Parent, OrderHint, MetaTitle, MetaDescription, MetaKeywords and Custom
     * fields/ type fields and assets of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link List} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildActions(@Nonnull final Product oldProduct,
                                                           @Nonnull final ProductDraft newProduct,
                                                           @Nonnull final ProductSyncOptions syncOptions) {
        final List<UpdateAction<Product>> updateActions =
            buildCoreActions(oldProduct, newProduct, syncOptions);
        final List<UpdateAction<Product>> assetUpdateActions =
            buildAssetActions(oldProduct, newProduct, syncOptions);
        updateActions.addAll(assetUpdateActions);
        return filterUpdateActions(updateActions, syncOptions.getActionsFilter());
    }

    /**
     * Compares the Name, Slug, externalID, Description, Parent, OrderHint, MetaTitle, MetaDescription, MetaKeywords
     * and Custom fields/ type fields of a {@link Category} and a {@link CategoryDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Category}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link Category} and the {@link CategoryDraft} have the same parents, an empty
     * {@link List} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildCoreActions(@Nonnull final Product oldProduct,
                                                               @Nonnull final ProductDraft newProduct,
                                                               @Nonnull final ProductSyncOptions syncOptions) {
        final List<UpdateAction<Product>> updateActions = buildUpdateActionsFromOptionals(Arrays.asList(
            buildChangeNameUpdateAction(oldProduct, newProduct, syncOptions),
            buildSetDescriptionUpdateAction(oldProduct, newProduct, syncOptions),
            buildChangeSlugUpdateAction(oldProduct, newProduct, syncOptions),
            buildSetSearchKeywordsUpdateAction(oldProduct, newProduct, syncOptions),
            buildSetMetaTitleUpdateAction(oldProduct, newProduct, syncOptions),
            buildSetMetaDescriptionUpdateAction(oldProduct, newProduct, syncOptions),
            buildSetMetaKeywordsUpdateAction(oldProduct, newProduct, syncOptions)
        ));

        updateActions.addAll(buildAddToCategoryUpdateActions(oldProduct, newProduct, syncOptions));
        updateActions.addAll(buildSetCategoryOrderHintsUpdateAction(oldProduct, newProduct, syncOptions));
        updateActions.addAll(buildRemoveFromCategoryUpdateActions(oldProduct, newProduct, syncOptions));

        return updateActions;
    }

    /**
     * TODO: SEE GITHUB ISSUE#3
     * Change to {@code public} once implemented.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @return A list of category-specific update actions.
     */
    @Nonnull
    private static List<UpdateAction<Product>> buildAssetActions(@Nonnull final Product oldProduct,
                                                                 @Nonnull final ProductDraft newProduct,
                                                                 @Nonnull final ProductSyncOptions syncOptions) {

        return new ArrayList<>();
    }

    /**
     * Applies a given filter function, if not null, {@code updateActionsFilter} on {@link List} of {@link UpdateAction}
     * elements.
     *
     * @param updateActions       the list of update actions to apply the filter on.
     * @param updateActionsFilter the filter functions to apply on the list of update actions
     * @return a new resultant list from applying the filter function, if not null, on the supplied list. If the filter
     * function supplied was null, the same supplied list is returned as is.
     */
    @Nonnull
    private static List<UpdateAction<Product>> filterUpdateActions(
        @Nonnull final List<UpdateAction<Product>> updateActions,
        @Nullable final Function<List<UpdateAction<Product>>, List<UpdateAction<Product>>> updateActionsFilter) {
        return updateActionsFilter != null ? updateActionsFilter.apply(updateActions) : updateActions;
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
    private static List<UpdateAction<Product>> buildUpdateActionsFromOptionals(
        @Nonnull final List<Optional<UpdateAction<Product>>> optionalUpdateActions) {
        return optionalUpdateActions.stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());
    }
}
