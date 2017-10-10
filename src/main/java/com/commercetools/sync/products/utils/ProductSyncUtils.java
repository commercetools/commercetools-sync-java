package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.SyncFilter;
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

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildActionIfPassesFilter;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildActionsIfPassesFilter;
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
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;

public final class ProductSyncUtils {

    /**
     * Compares the name, description, slug, search keywords, metaTitle, metaDescription, metaKeywords, categories,
     * variants (comparing all variants see
     * {@link ProductUpdateActionUtils#buildVariantsUpdateActions(Product, ProductDraft, ProductSyncOptions, Map)}),
     * and publish state of a {@link Product} and a {@link ProductDraft}, given that each of these fields pass the
     * specified {@link SyncFilter}. It returns a {@link List} of {@link UpdateAction}&lt;{@link Product}&gt; as a
     * result. If no update action is needed, for example in case where both the {@link Product} and the
     * {@link ProductDraft} have the same names, an empty {@link List} is returned. Then it applies a specified filter
     * function in the {@link ProductSyncOptions} instance on the resultant list and returns this result.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link ProductSyncOptions}
     *                    for more info.
     * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines attribute
     *                           information: its name, whether a value is required or not and whether it has the
     *                           constraint "SameForAll" or not.
     * @return A list of product-specific update actions.
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
        return filterUpdateActions(updateActions, syncOptions.getUpdateActionsCallBack());
    }

    /**
     * Compares the name, description, slug, search keywords, metaTitle, metaDescription, metaKeywords, categories,
     * variants (comparing all variants see
     * {@link ProductUpdateActionUtils#buildVariantsUpdateActions(Product, ProductDraft, ProductSyncOptions, Map)})
     * and publish state of a {@link Product} and a {@link ProductDraft}, given that each of these fields pass the
     * specified {@link SyncFilter}. It returns a {@link List} of {@link UpdateAction}&lt;{@link Product}&gt; as a
     * result. If no update action is needed, for example in case where both the {@link Product} and the
     * {@link ProductDraft} have the same names, an empty {@link List} is returned.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link ProductSyncOptions}
     *                    for more info.
     * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines attribute
     *                           information: its name, whether a value is required or not and whether it has the
     *                           constraint "SameForAll" or not.
     * @return A list of product-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildCoreActions(@Nonnull final Product oldProduct,
                                                               @Nonnull final ProductDraft newProduct,
                                                               @Nonnull final ProductSyncOptions syncOptions,
                                                               @Nonnull final Map<String, AttributeMetaData>
                                                                       attributesMetaData) {
        final SyncFilter syncFilter = syncOptions.getSyncFilter();
        final List<UpdateAction<Product>> updateActions = buildUpdateActionsFromOptionals(
            Arrays.asList(
                buildActionIfPassesFilter(syncFilter, ActionGroup.NAME, () ->
                    buildChangeNameUpdateAction(oldProduct, newProduct)),

                buildActionIfPassesFilter(syncFilter, ActionGroup.DESCRIPTION, () ->
                    buildSetDescriptionUpdateAction(oldProduct, newProduct)),

                buildActionIfPassesFilter(syncFilter, ActionGroup.SLUG, () ->
                        buildChangeSlugUpdateAction(oldProduct, newProduct)),

                buildActionIfPassesFilter(syncFilter, ActionGroup.SEARCHKEYWORDS, () ->
                    buildSetSearchKeywordsUpdateAction(oldProduct, newProduct)),

                buildActionIfPassesFilter(syncFilter, ActionGroup.METATITLE, () ->
                    buildSetMetaTitleUpdateAction(oldProduct, newProduct)),

                buildActionIfPassesFilter(syncFilter, ActionGroup.METADESCRIPTION, () ->
                    buildSetMetaDescriptionUpdateAction(oldProduct, newProduct)),

                buildActionIfPassesFilter(syncFilter, ActionGroup.METAKEYWORDS, () ->
                    buildSetMetaKeywordsUpdateAction(oldProduct, newProduct))
            ));

        final List<UpdateAction<Product>> productCategoryUpdateActions =
            buildActionsIfPassesFilter(syncFilter, ActionGroup.CATEGORIES, () ->
                buildCategoryActions(oldProduct, newProduct));
        updateActions.addAll(productCategoryUpdateActions);

        final List<UpdateAction<Product>> variantUpdateActions =
            buildVariantsUpdateActions(oldProduct, newProduct, syncOptions, attributesMetaData);
        updateActions.addAll(variantUpdateActions);

        // lastly publish/unpublish product
        buildPublishUpdateAction(oldProduct, newProduct).ifPresent(updateActions::add);
        return updateActions;
    }

    /**
     * Compares the categories of a {@link Product} and a {@link ProductDraft}. It returns a {@link List} of
     * {@link UpdateAction}&lt;{@link Product}&gt; as a result. If no update action is needed, for example in
     * case where both the {@link Product} and the {@link ProductDraft} have the identical categories, an empty
     * {@link List} is returned.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new data.
     * @return A list of product category-related update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildCategoryActions(@Nonnull final Product oldProduct,
                                                                   @Nonnull final ProductDraft newProduct) {
        final List<UpdateAction<Product>> updateActions = new ArrayList<>();
        updateActions.addAll(buildAddToCategoryUpdateActions(oldProduct, newProduct));
        updateActions.addAll(buildSetCategoryOrderHintUpdateActions(oldProduct, newProduct));
        updateActions.addAll(buildRemoveFromCategoryUpdateActions(oldProduct, newProduct));
        return updateActions;
    }

    /**
     * TODO: SEE GITHUB ISSUE#3
     * Change to {@code public} once implemented.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new data.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied
     *                    by the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link ProductSyncOptions}
     *                    for more info.
     * @return A list of product assets-specific update actions.
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
     * Given a list of product {@link UpdateAction} elements, where each is wrapped in an {@link Optional}; this method
     * filters out the optionals which are only present and returns a new list of product {@link UpdateAction}
     * elements.
     *
     * @param optionalUpdateActions list of product {@link UpdateAction} elements, where each is wrapped
     *                              in an {@link Optional}.
     * @return a List of product update actions from the optionals that were present in
     *         the {@code optionalUpdateActions} list parameter.
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
