package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.utils.StreamUtils;
import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.SyncFilter;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.*;

import static com.commercetools.sync.commons.utils.StreamUtils.asList;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.*;

public final class ProductSyncUtils {
    /**
     * Compares all the fields (including the variants see
     * {@link ProductUpdateActionUtils#buildVariantsUpdateActions(Product, ProductDraft, ProductSyncOptions, Map)})
     * of a {@link Product} and a {@link ProductDraft}, given that each of these fields pass the
     * specified {@link SyncFilter}. It returns a {@link List} of {@link UpdateAction}&lt;{@link Product}&gt; as a
     * result. If no update action is needed, for example in case where both the {@link Product} and the
     * {@link ProductDraft} have the same names, an empty {@link List} is returned. Then it applies a specified filter
     * function in the {@link ProductSyncOptions} instance on the resultant list and returns this result.
     *
     * @param oldProduct         the product which should be updated.
     * @param newProduct         the product draft where we get the new data.
     * @param syncOptions        the sync options wrapper which contains options related to the sync process supplied by
     *                           the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                           on the build update action process. And other options (See {@link ProductSyncOptions}
     *                           for more info.
     * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines each attribute's
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
        final SyncFilter syncFilter = syncOptions.getSyncFilter();

        final List<UpdateAction<Product>> updateActions = new ArrayList<>(buildUpdateActionsFromOptionals(Arrays.asList(
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
                buildSetMetaKeywordsUpdateAction(oldProduct, newProduct)),

            buildActionIfPassesFilter(syncFilter, ActionGroup.TAXCATEGORY, () ->
                buildSetTaxCategoryUpdateAction(oldProduct, newProduct)),

            buildActionIfPassesFilter(syncFilter, ActionGroup.STATE, () ->
                buildTransitionStateUpdateAction(oldProduct, newProduct))
        )));

        final List<UpdateAction<Product>> productCategoryUpdateActions =
            buildActionsIfPassesFilter(syncFilter, ActionGroup.CATEGORIES, () ->
                buildCategoryActions(oldProduct, newProduct));
        updateActions.addAll(productCategoryUpdateActions);

        updateActions.addAll(buildVariantsUpdateActions(oldProduct, newProduct, syncOptions, attributesMetaData));

        // lastly publish/unpublish product
        buildPublishUpdateAction(oldProduct, newProduct).ifPresent(updateActions::add);

        return syncOptions.applyBeforeUpdateCallBack(updateActions, newProduct, oldProduct);
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
        @Nonnull final List<Optional<? extends UpdateAction<Product>>> optionalUpdateActions) {
        return asList(optionalUpdateActions.stream());
    }

    private ProductSyncUtils() {
    }
}
