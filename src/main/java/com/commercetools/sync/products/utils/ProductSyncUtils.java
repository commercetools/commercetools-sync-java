package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.SyncFilter;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.PriceDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.replaceReferenceIdWithKey;
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
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetTaxCategoryUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildTransitionStateUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;
import static java.util.stream.Collectors.toList;

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
        return filterUpdateActions(updateActions, syncOptions.getUpdateActionsCallBack());
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
    public static List<UpdateAction<Product>> buildCoreActions(@Nonnull final Product oldProduct,
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

        final List<UpdateAction<Product>> productCatgoryUpdateActions =
            buildActionsIfPassesFilter(syncFilter, ActionGroup.CATEGORIES, () ->
                buildCategoryActions(oldProduct, newProduct));
        updateActions.addAll(productCatgoryUpdateActions);

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


    /**
     * Takes a list of Products that are supposed to have their product type and category references expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new list of product drafts with their references containing keys instead of the ids. Note that if the
     * references are not expanded for a product, the reference ids will not be replaced with keys and will still have
     * their ids in place.
     *
     * @param products the products to replace their reference ids with keys
     * @return a list of products drafts with keys instead of ids for references.
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    public static List<ProductDraft> replaceProductsReferenceIdsWithKeys(@Nonnull final List<Product> products) {
        return products
            .stream()
            .map(product -> {
                // Resolve productType reference
                final Reference<ProductType> productType = product.getProductType();
                final Reference<ProductType> productTypeReferenceWithKey = replaceReferenceIdWithKey(productType,
                    () -> ProductType.referenceOfId(productType.getObj().getKey()));

                final ProductDraft productDraft = getDraftBuilderFromStagedProduct(product).build();
                final ProductDraft productDraftWithCategoryKeys =
                    replaceProductDraftCategoryReferenceIdsWithKeys(productDraft);

                return ProductDraftBuilder.of(productDraftWithCategoryKeys)
                                          .productType(productTypeReferenceWithKey)
                                          .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Takes a list of product drafts that are supposed to have their category references expanded in order to be able
     * to fetch the keys and replace the reference ids with the corresponding keys and then return a new list of product
     * drafts with their references containing keys instead of the ids. Note that if the references are not expanded
     * for a product, the reference ids will not be replaced with keys and will still have their ids in place.
     *
     * @param productDrafts the product drafts to replace their reference ids with keys
     * @return a list of products drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<ProductDraft> replaceProductDraftsCategoryReferenceIdsWithKeys(@Nonnull final List<ProductDraft>
                                                                                          productDrafts) {
        return productDrafts.stream()
                            .map(productDraft ->
                                productDraft != null
                                    ? replaceProductDraftCategoryReferenceIdsWithKeys(productDraft) : null
                            )
                            .collect(Collectors.toList());
    }

    /**
     * Takes a product draft that is supposed to have its category references expanded
     * in order to be able to fetch the keys and replace the reference ids with the corresponding keys and then return
     * a new product drafts with the references containing keys instead of the ids. Note that the category references
     * should be expanded for the productDraft, otherwise the reference ids will not be replaced with keys and will
     * still have their ids in place.
     *
     * @param productDraft the product drafts to replace its reference ids with keys
     * @return a new products draft with keys instead of ids for references.
     */
    @Nonnull
    public static ProductDraft replaceProductDraftCategoryReferenceIdsWithKeys(@Nonnull final ProductDraft
                                                                                   productDraft) {
        final Set<ResourceIdentifier<Category>> categories = productDraft.getCategories();
        List<Reference<Category>> categoryReferencesWithKeys = new ArrayList<>();
        Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();
        if (categories != null) {
            categories.forEach(categoryResourceIdentifier -> {
                final String categoryKey = categoryResourceIdentifier.getKey();
                final String categoryId = categoryResourceIdentifier.getId();
                if (categoryKey != null) {
                    // Replace category reference id with key.
                    categoryReferencesWithKeys.add(Category.referenceOfId(categoryKey));

                    // Replace categoryOrderHint id with key.
                    final CategoryOrderHints categoryOrderHints = productDraft.getCategoryOrderHints();
                    if (categoryOrderHints != null) {
                        final String categoryOrderHintValue = categoryOrderHints.get(categoryKey);
                        if (categoryOrderHintValue == null) {
                            // to handle case of getting category order hints from another CTP
                            // TODO NEEDS TO BE REFACTORED INTO OWN METHOD.
                            final String categoryOrderHintValueById = categoryOrderHints.get(categoryId);
                            if (categoryOrderHintValueById != null) {
                                categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValueById);
                            }
                        } else {
                            categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
                        }
                    }
                } else {
                    categoryReferencesWithKeys.add(Category.referenceOfId(categoryId));
                }
            });
        }
        final CategoryOrderHints categoryOrderHintsWithKeys = CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
        final ProductDraft productDraftPriceChannelIdsWithKeys =
            replaceProductDraftPriceChannelIdsWithKeys(productDraft);

        return ProductDraftBuilder.of(productDraftPriceChannelIdsWithKeys)
                                  .categories(categoryReferencesWithKeys)
                                  .categoryOrderHints(categoryOrderHintsWithKeys)
                                  .build();
    }

    private static ProductDraft replaceProductDraftPriceChannelIdsWithKeys(@Nonnull final ProductDraft productDraft) {
        final ProductVariantDraft masterVariant = productDraft.getMasterVariant();
        final List<ProductVariantDraft> productDraftVariants = productDraft.getVariants();
        ProductDraftBuilder productDraftBuilder = ProductDraftBuilder.of(productDraft);

        if (masterVariant != null) {
            final ProductVariantDraft masterVariantPriceChannelReferencesWithKeys =
                replaceProductVariantDraftPriceChannelIdsWithKeys(masterVariant);
            productDraftBuilder = productDraftBuilder.masterVariant(masterVariantPriceChannelReferencesWithKeys);
        }

        if (productDraftVariants != null) {
            final List<ProductVariantDraft> variantDraftsWithPriceChannelReferencesWithKeys =
                productDraftVariants.stream()
                                    .filter(Objects::nonNull)
                                    .map(ProductSyncUtils::replaceProductVariantDraftPriceChannelIdsWithKeys)
                                    .collect(toList());
            productDraftBuilder = productDraftBuilder.variants(variantDraftsWithPriceChannelReferencesWithKeys);
        }
        return productDraftBuilder.build();
    }

    @Nonnull
    private static ProductVariantDraft replaceProductVariantDraftPriceChannelIdsWithKeys(
        @Nonnull final ProductVariantDraft productVariantDraft) {
        final List<PriceDraft> variantDraftPrices = productVariantDraft.getPrices();
        final ProductVariantDraftBuilder productVariantDraftBuilder =
            ProductVariantDraftBuilder.of(productVariantDraft);

        if (variantDraftPrices == null) {
            return productVariantDraftBuilder.build();
        }
        final List<PriceDraft> priceDraftsWithChannelKeys =
            variantDraftPrices.stream()
                              .map(ProductSyncUtils::replacePriceDraftChannelIdWithKey)
                              .collect(toList());
        return productVariantDraftBuilder.prices(priceDraftsWithChannelKeys)
                                         .build();
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    private static PriceDraft replacePriceDraftChannelIdWithKey(@Nonnull final PriceDraft priceDraft) {
        final Reference<Channel> priceDraftChannel = priceDraft.getChannel();
        final Reference<Channel> channelReferenceWithKey = replaceReferenceIdWithKey(priceDraftChannel, () ->
            Channel.referenceOfId(priceDraftChannel.getObj().getKey()));

        return PriceDraftBuilder.of(priceDraft)
                                .channel(channelReferenceWithKey)
                                .build();
    }

    /**
     * Given a {@link CategoryOrderHints} instance and a set of {@link Category} {@link ResourceIdentifier}, this method
     * replaces all the categoryOrderHint ids with the {@link Category} keys.
     * TODO: EITHER TO BE MOVED INTO IT UTILS OR REUSED IN #replaceProductDraftCategoryReferenceIdsWithKeys.
     *
     *
     * @param categoryOrderHints the categoryOrderHints that should have its keys replaced with ids.
     * @param categoryResourceIdentifiers the category resource identifiers which contains the ids and keys of the
     *                                    categories.
     * @return a new {@link CategoryOrderHints} instance with keys replacing the category ids.
     */
    @Nonnull
    public static CategoryOrderHints replaceCategoryOrderHintCategoryIdsWithKeys(
        @Nonnull final CategoryOrderHints categoryOrderHints,
        @Nonnull final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers) {
        final Map<String, String> categoryOrderHintKeyMap = new HashMap<>();
        categoryOrderHints.getAsMap()
                          .forEach((categoryId, categoryOrderHintValue) ->
                              categoryResourceIdentifiers.stream()
                                                         .filter(categoryResourceIdentifier ->
                                                             Objects.equals(categoryResourceIdentifier.getId(),
                                                                 categoryId))
                                                         .findFirst()
                                                         .ifPresent(categoryResourceIdentifier ->
                                                             categoryOrderHintKeyMap
                                                                 .put(categoryResourceIdentifier.getKey(),
                                                                     categoryOrderHintValue)));
        return CategoryOrderHints.of(categoryOrderHintKeyMap);
    }

    /**
     * Given a {@link Product} this method creates a {@link ProductDraftBuilder} based on the staged projection
     * values of the supplied product.
     *
     * @param product the product to create a {@link ProductDraftBuilder} based on it's staged data.
     * @return a {@link ProductDraftBuilder} based on the staged projection values of the supplied product.
     */
    @Nonnull
    public static ProductDraftBuilder getDraftBuilderFromStagedProduct(@Nonnull final Product product) {
        final ProductData productData = product.getMasterData().getStaged();
        final List<ProductVariantDraft> allVariants = productData
            .getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder
            .of(product.getMasterData().getStaged().getMasterVariant()).build();

        return ProductDraftBuilder
            .of(product.getProductType(), productData.getName(), productData.getSlug(), allVariants)
            .masterVariant(masterVariant)
            .metaDescription(productData.getMetaDescription())
            .metaKeywords(productData.getMetaKeywords())
            .metaTitle(productData.getMetaTitle())
            .description(productData.getDescription())
            .searchKeywords(productData.getSearchKeywords())
            .taxCategory(product.getTaxCategory())
            .key(product.getKey())
            .publish(product.getMasterData().isPublished())
            .categories(getCategoryResourceIdentifiers(productData))
            .categoryOrderHints(productData.getCategoryOrderHints());
    }

    /**
     * Builds a {@link Set} of Category {@link ResourceIdentifier} given a {@link ProductData} instance which contains
     * references to those categories.
     *
     * @param productData the instance containing the category references.
     * @return a set of {@link Category} {@link ResourceIdentifier}
     */
    @Nonnull
    @SuppressWarnings("ConstantConditions") // NPE (categoryReference.getObj().getKey()) is being checked in the filter.
    private static Set<ResourceIdentifier<Category>> getCategoryResourceIdentifiers(@Nonnull final ProductData
                                                                                        productData) {
        final Set<Reference<Category>> categoryReferences = productData.getCategories();
        if (categoryReferences == null) {
            return Collections.emptySet();
        }
        return categoryReferences.stream()
                                 .filter(Objects::nonNull)
                                 .filter(categoryReference -> Objects.nonNull(categoryReference.getObj()))
                                 .map(categoryReference ->
                                     ResourceIdentifier.<Category>ofIdOrKey(categoryReference.getId(),
                                         categoryReference.getObj().getKey(), Category.referenceTypeId()))
                                 .collect(Collectors.toSet());
    }
}
