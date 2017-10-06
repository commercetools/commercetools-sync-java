package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.products.AttributeMetaData;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeNameUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeSlugUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildPublishUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveFromCategoryUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetCategoryOrderHintUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetSearchKeywordsUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetTaxCategoryUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildTransitionStateUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;

// TODO: FIX DOCUMENTATION AFTER CHANGE OF REMOVAL OF SYNC OPTIONS FOR ONLY COMPARING STAGED.
public final class ProductSyncUtils {
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
     * @param attributesMetaData TODO
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildActions(@Nonnull final Product oldProduct,
                                                           @Nonnull final ProductDraft newProduct,
                                                           @Nonnull final ProductSyncOptions syncOptions,
                                                           @Nonnull final Map<String, AttributeMetaData>
                                                                   attributesMetaData) {
        final List<UpdateAction<Product>> updateActions =
            buildCoreActions(oldProduct, newProduct, syncOptions, attributesMetaData);
        final List<UpdateAction<Product>> assetUpdateActions =
            buildAssetActions(oldProduct, newProduct, syncOptions);
        updateActions.addAll(assetUpdateActions);
        return filterUpdateActions(updateActions, syncOptions.getUpdateActionsFilter());
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
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link BaseSyncOptions}
     *                    for more info.
     * @param attributesMetaData TODO
     * @return A list of category-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildCoreActions(
        @Nonnull final Product oldProduct,
        @Nonnull final ProductDraft newProduct,
        @Nonnull final ProductSyncOptions syncOptions,
        @Nonnull final Map<String, AttributeMetaData> attributesMetaData) {

        final List<UpdateAction<Product>> updateActions = new ArrayList<>(buildUpdateActionsFromOptionals(Arrays.asList(
            buildChangeNameUpdateAction(oldProduct, newProduct),
            buildSetDescriptionUpdateAction(oldProduct, newProduct),
            buildChangeSlugUpdateAction(oldProduct, newProduct),
            buildSetSearchKeywordsUpdateAction(oldProduct, newProduct),
            buildSetMetaTitleUpdateAction(oldProduct, newProduct),
            buildSetMetaDescriptionUpdateAction(oldProduct, newProduct),
            buildSetMetaKeywordsUpdateAction(oldProduct, newProduct),
            buildSetTaxCategoryUpdateAction(oldProduct, newProduct),
            buildTransitionStateUpdateAction(oldProduct, newProduct)
        )));

        updateActions.addAll(buildAddToCategoryUpdateActions(oldProduct, newProduct));
        updateActions.addAll(buildSetCategoryOrderHintUpdateActions(oldProduct, newProduct));
        updateActions.addAll(buildRemoveFromCategoryUpdateActions(oldProduct, newProduct));
        updateActions.addAll(buildVariantsUpdateActions(oldProduct, newProduct, syncOptions, attributesMetaData));

        // lastly publish/unpublish
        buildPublishUpdateAction(oldProduct, newProduct).ifPresent(updateActions::add);
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
     *      function supplied was null, the same supplied list is returned as is.
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
        @Nonnull final List<Optional<? extends UpdateAction<Product>>> optionalUpdateActions) {
        return optionalUpdateActions.stream()
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());
    }
}
