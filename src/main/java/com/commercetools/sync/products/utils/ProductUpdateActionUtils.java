package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.SyncFilter;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.products.commands.updateactions.SetTaxCategory;
import io.sphere.sdk.products.commands.updateactions.TransitionState;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.search.SearchKeywords;
import io.sphere.sdk.states.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.commons.utils.CollectionUtils.emptyIfNull;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActions;
import static com.commercetools.sync.commons.utils.FilterUtils.executeSupplierIfPassesFilter;
import static com.commercetools.sync.internals.utils.UnorderedCollectionSyncUtils.buildRemoveUpdateActions;
import static com.commercetools.sync.products.ActionGroup.ASSETS;
import static com.commercetools.sync.products.ActionGroup.ATTRIBUTES;
import static com.commercetools.sync.products.ActionGroup.IMAGES;
import static com.commercetools.sync.products.ActionGroup.PRICES;
import static com.commercetools.sync.products.ActionGroup.SKU;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAssetsUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantImagesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantPricesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isBlank;

public final class ProductUpdateActionUtils {
    private static final String BLANK_VARIANT_KEY = "The variant key is blank.";
    private static final String NULL_VARIANT = "The variant is null.";
    static final String BLANK_OLD_MASTER_VARIANT_KEY = "Old master variant key is blank.";
    static final String BLANK_NEW_MASTER_VARIANT_KEY = "New master variant null or has blank key.";
    static final String BLANK_NEW_MASTER_VARIANT_SKU = "New master variant has blank SKU.";

    /**
     * Compares the {@link LocalizedString} names of a {@link ProductDraft} and a {@link Product}. It returns an
     * {@link ChangeName} as a result in an {@link Optional}. If both the {@link Product} and the {@link ProductDraft}
     * have the same name, then no update action is needed and hence an empty {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new name.
     * @return A filled optional with the update action or an empty optional if the names are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildChangeNameUpdateAction(@Nonnull final Product oldProduct,
                                                                              @Nonnull final ProductDraft newProduct) {
        final LocalizedString newName = newProduct.getName();
        final LocalizedString oldName = oldProduct.getMasterData().getStaged().getName();
        return buildUpdateAction(oldName, newName, () -> ChangeName.of(newName, true));
    }

    /**
     * Compares the {@link LocalizedString} descriptions of a {@link ProductDraft} and a {@link Product}. It returns an
     * {@link SetDescription} as a result in an {@link Optional}. If both the {@link Product} and the
     * {@link ProductDraft} have the same description, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new description.
     * @return A filled optional with the update action or an empty optional if the descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetDescriptionUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final LocalizedString newDescription = newProduct.getDescription();
        final LocalizedString oldDescription = oldProduct.getMasterData().getStaged().getDescription();
        return buildUpdateAction(oldDescription, newDescription, () -> SetDescription.of(newDescription, true));
    }

    /**
     * Compares the {@link LocalizedString} slugs of a {@link ProductDraft} and a {@link Product}. It returns a
     * {@link ChangeSlug} update action as a result in an {@link Optional}. If both the {@link Product} and the
     * {@link ProductDraft} have the same slug, then no update action is needed and hence an empty {@link Optional} is
     * returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new slug.
     * @return A filled optional with the update action or an empty optional if the slugs are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildChangeSlugUpdateAction(@Nonnull final Product oldProduct,
                                                                              @Nonnull final ProductDraft newProduct) {
        final LocalizedString newSlug = newProduct.getSlug();
        final LocalizedString oldSlug = oldProduct.getMasterData().getStaged().getSlug();
        return buildUpdateAction(oldSlug, newSlug, () -> ChangeSlug.of(newSlug, true));
    }

    /**
     * Compares the {@link Set} of {@link Category} {@link Reference}s of a {@link ProductDraft} and a {@link Product}.
     * It returns a {@link List} of {@link AddToCategory} update actions as a result, if the old product
     * needs to be added to a category to have the same set of categories as the new product.
     * If both the {@link Product} and the {@link ProductDraft} have the same set of categories, then no update actions
     * are needed and hence an empty {@link List} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new slug.
     * @return A list containing the update actions or an empty list if the category sets are identical.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildAddToCategoryUpdateActions(@Nonnull final Product oldProduct,
                                                                              @Nonnull final ProductDraft newProduct) {
        final Set<ResourceIdentifier<Category>> newCategories = newProduct.getCategories();
        final Set<Reference<Category>> oldCategories = oldProduct.getMasterData().getStaged().getCategories();
        return buildUpdateActions(oldCategories, newCategories,
            () -> {
                final List<UpdateAction<Product>> updateActions = new ArrayList<>();
                final List<ResourceIdentifier<Category>> newCategoriesResourceIdentifiers =
                    filterCollection(newCategories, newCategoryReference ->
                        oldCategories.stream()
                                     .map(Reference::toResourceIdentifier)
                                     .noneMatch(oldResourceIdentifier ->
                                         oldResourceIdentifier.equals(newCategoryReference)))
                        .collect(toList());
                newCategoriesResourceIdentifiers.forEach(categoryResourceIdentifier ->
                    updateActions.add(AddToCategory.of(categoryResourceIdentifier, true)));
                return updateActions;
            });
    }

    /**
     * Compares the {@link CategoryOrderHints} of a {@link ProductDraft} and a {@link Product}. It returns a
     * {@link SetCategoryOrderHint} update action as a result in an {@link List}. If both the {@link Product} and the
     * {@link ProductDraft} have the same categoryOrderHints, then no update actions are needed and hence an empty
     * {@link List} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new categoryOrderHints.
     * @return A list containing the update actions or an empty list if the categoryOrderHints are identical.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildSetCategoryOrderHintUpdateActions(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final CategoryOrderHints newCategoryOrderHints = newProduct.getCategoryOrderHints();
        final CategoryOrderHints oldCategoryOrderHints = oldProduct.getMasterData().getStaged().getCategoryOrderHints();
        return buildUpdateActions(oldCategoryOrderHints, newCategoryOrderHints, () -> {

            final Set<String> newCategoryIds = newProduct.getCategories().stream()
                                                         .map(ResourceIdentifier::getId)
                                                         .collect(toSet());

            final List<UpdateAction<Product>> updateActions = new ArrayList<>();

            final Map<String, String> newMap = nonNull(newCategoryOrderHints) ? newCategoryOrderHints
                .getAsMap() : emptyMap();
            final Map<String, String> oldMap = nonNull(oldCategoryOrderHints) ? oldCategoryOrderHints
                .getAsMap() : emptyMap();

            // remove category hints present in old product if they are absent in draft but only if product
            // is or will be assigned to given category
            oldMap.forEach((categoryId, value) -> {
                if (!newMap.containsKey(categoryId) && newCategoryIds.contains(categoryId)) {
                    updateActions.add(SetCategoryOrderHint.of(categoryId, null, true));
                }
            });

            // add category hints present in draft if they are absent or changed in old product
            newMap.forEach((key, value) -> {
                if (!oldMap.containsKey(key) || !Objects.equals(oldMap.get(key), value)) {
                    updateActions.add(SetCategoryOrderHint.of(key, value, true));
                }
            });

            return updateActions;
        });
    }

    /**
     * Compares the {@link Set} of {@link Category} {@link Reference}s of a {@link ProductDraft} and a {@link Product}.
     * It returns a {@link List} of {@link RemoveFromCategory} update actions as a result, if the old product
     * needs to be removed from a category to have the same set of categories as the new product.
     * If both the {@link Product} and the {@link ProductDraft} have the same set of categories, then no update actions
     * are needed and hence an empty {@link List} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new slug.
     * @return A list containing the update actions or an empty list if the category sets are identical.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildRemoveFromCategoryUpdateActions(
        @Nonnull final Product oldProduct,
        @Nonnull final ProductDraft newProduct) {
        final Set<ResourceIdentifier<Category>> newCategories = newProduct.getCategories();
        final Set<Reference<Category>> oldCategories = oldProduct.getMasterData().getStaged().getCategories();
        return buildUpdateActions(oldCategories, newCategories, () -> {
            final List<UpdateAction<Product>> updateActions = new ArrayList<>();
            filterCollection(oldCategories, oldCategoryReference ->
                !newCategories.contains(oldCategoryReference.toResourceIdentifier()))
                .forEach(categoryReference ->
                    updateActions.add(RemoveFromCategory.of(categoryReference.toResourceIdentifier(), true))
                );
            return updateActions;
        });
    }

    /**
     * Compares the {@link SearchKeywords} of a {@link ProductDraft} and a {@link Product}. It returns a
     * {@link SetSearchKeywords} update action as a result in an {@link Optional}. If both the {@link Product} and the
     * {@link ProductDraft} have the same search keywords, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new search keywords.
     * @return A filled optional with the update action or an empty optional if the search keywords are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetSearchKeywordsUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final SearchKeywords newSearchKeywords = newProduct.getSearchKeywords();
        final SearchKeywords oldSearchKeywords = oldProduct.getMasterData().getStaged().getSearchKeywords();
        return buildUpdateAction(oldSearchKeywords, newSearchKeywords,
            () -> SetSearchKeywords.of(newSearchKeywords, true));
    }

    /**
     * Compares the {@link LocalizedString} meta descriptions of a {@link ProductDraft} and a {@link Product}. It
     * returns a {@link SetMetaDescription} update action as a result in an {@link Optional}. If both the
     * {@link Product} and the {@link ProductDraft} have the same meta description, then no update action is needed and
     * hence an empty {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new meta description.
     * @return A filled optional with the update action or an empty optional if the meta descriptions are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetMetaDescriptionUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final LocalizedString newMetaDescription = newProduct.getMetaDescription();
        final LocalizedString oldMetaDescription = oldProduct.getMasterData().getStaged().getMetaDescription();
        return buildUpdateAction(oldMetaDescription, newMetaDescription,
            () -> SetMetaDescription.of(newMetaDescription));
    }

    /**
     * Compares the {@link LocalizedString} meta keywordss of a {@link ProductDraft} and a {@link Product}. It returns
     * a {@link SetMetaKeywords} update action as a result in an {@link Optional}. If both the {@link Product} and the
     * {@link ProductDraft} have the same meta keywords, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new meta keywords.
     * @return A filled optional with the update action or an empty optional if the meta keywords are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetMetaKeywordsUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final LocalizedString newMetaKeywords = newProduct.getMetaKeywords();
        final LocalizedString oldMetaKeywords = oldProduct.getMasterData().getStaged().getMetaKeywords();
        return buildUpdateAction(oldMetaKeywords, newMetaKeywords, () -> SetMetaKeywords.of(newMetaKeywords));
    }

    /**
     * Compares the {@link LocalizedString} meta titles of a {@link ProductDraft} and a {@link Product}. It returns a
     * {@link SetMetaTitle} update action as a result in an {@link Optional}. If both the {@link Product} and the
     * {@link ProductDraft} have the same meta title, then no update action is needed and hence an empty
     * {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new meta title.
     * @return A filled optional with the update action or an empty optional if the meta titles are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildSetMetaTitleUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final LocalizedString newMetaTitle = newProduct.getMetaTitle();
        final LocalizedString oldMetaTitle = oldProduct.getMasterData().getStaged().getMetaTitle();
        return buildUpdateAction(oldMetaTitle, newMetaTitle, () -> SetMetaTitle.of(newMetaTitle));
    }

    /**
     * Compares the variants (including the master variants) of a {@link ProductDraft} and a {@link Product}. It returns
     * a {@link List} of variant related update actions. For example:
     *
     * <ul>
     *     <li>{@link AddVariant}</li>
     *     <li>{@link RemoveVariant}</li>
     *     <li>{@link ChangeMasterVariant}</li>
     *     <li>{@link io.sphere.sdk.products.commands.updateactions.SetAttribute}</li>
     *     <li>{@link io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants}</li>
     *     <li>{@link io.sphere.sdk.products.commands.updateactions.SetSku}</li>
     *     <li>{@link io.sphere.sdk.products.commands.updateactions.AddExternalImage}</li>
     *     <li>{@link io.sphere.sdk.products.commands.updateactions.RemoveImage}</li>
     *     <li>{@link io.sphere.sdk.products.commands.updateactions.AddPrice}</li>
     *     <li> ... and more variant level update actions.</li>
     * </ul>
     * If both the {@link Product} and the {@link ProductDraft} have identical variants, then no update actions are
     * needed and hence an empty {@link List} is returned.
     *
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new meta title.
     * @param syncOptions the sync options wrapper which contains options related to the sync process supplied by
     *                    the user. For example, custom callbacks to call in case of warnings or errors occurring
     *                    on the build update action process. And other options (See {@link ProductSyncOptions}
     *                    for more info).
     * @param attributesMetaData a map of attribute name -&gt; {@link AttributeMetaData}; which defines attribute
     *                           information: its name and whether it has the constraint "SameForAll" or not.
     * @return A list of product variant-specific update actions.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildVariantsUpdateActions(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct,
            @Nonnull final ProductSyncOptions syncOptions,
            @Nonnull final Map<String, AttributeMetaData> attributesMetaData) {

        if (haveInvalidMasterVariants(oldProduct, newProduct, syncOptions)) {
            return emptyList();
        }

        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();

        final List<ProductVariant> oldProductVariantsWithoutMaster =
            oldProduct.getMasterData().getStaged().getVariants();

        final Map<String, ProductVariant> oldProductVariantsNoMaster =
            collectionToMap(oldProductVariantsWithoutMaster, ProductVariant::getKey);

        final Map<String, ProductVariant> oldProductVariantsWithMaster = new HashMap<>(oldProductVariantsNoMaster);
        oldProductVariantsWithMaster.put(oldMasterVariant.getKey(), oldMasterVariant);

        final List<ProductVariantDraft> newAllProductVariants = new ArrayList<>(newProduct.getVariants());
        newAllProductVariants.add(newProduct.getMasterVariant());

        // Remove missing variants, but keep master variant (MV can't be removed)
        final List<UpdateAction<Product>> updateActions =
            buildRemoveUpdateActions(oldProductVariantsWithoutMaster, newAllProductVariants, ProductVariant::getKey,
                ProductVariantDraft::getKey, variant -> RemoveVariant.ofVariantId(variant.getId(), true));

        emptyIfNull(newAllProductVariants).forEach(newProductVariant -> {
            if (newProductVariant == null) {
                handleBuildVariantsUpdateActionsError(oldProduct, NULL_VARIANT, syncOptions);
            } else {
                final String newProductVariantKey = newProductVariant.getKey();
                if (isBlank(newProductVariantKey)) {
                    handleBuildVariantsUpdateActionsError(oldProduct, BLANK_VARIANT_KEY, syncOptions);
                } else {
                    final ProductVariant matchingOldVariant = oldProductVariantsWithMaster.get(newProductVariantKey);
                    final List<UpdateAction<Product>> updateOrAddVariant = ofNullable(matchingOldVariant)
                        .map(oldVariant -> collectAllVariantUpdateActions(oldProduct, oldVariant, newProductVariant,
                            attributesMetaData, syncOptions))
                        .orElseGet(() -> singletonList(buildAddVariantUpdateActionFromDraft(newProductVariant)));
                    updateActions.addAll(updateOrAddVariant);
                }
            }
        });

        updateActions.addAll(buildChangeMasterVariantUpdateAction(oldProduct, newProduct, syncOptions));
        return updateActions;
    }

    @Nonnull
    private static List<UpdateAction<Product>> collectAllVariantUpdateActions(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductVariant oldProductVariant,
            @Nonnull final ProductVariantDraft newProductVariant,
            @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
            @Nonnull final ProductSyncOptions syncOptions) {
        final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
        final SyncFilter syncFilter = syncOptions.getSyncFilter();

        updateActions.addAll(
            buildActionsIfPassesFilter(syncFilter, ATTRIBUTES, () ->
                buildProductVariantAttributesUpdateActions(oldProduct.getKey(), oldProductVariant,
                    newProductVariant, attributesMetaData, syncOptions)));

        updateActions.addAll(
            buildActionsIfPassesFilter(syncFilter, IMAGES, () ->
                buildProductVariantImagesUpdateActions(oldProductVariant, newProductVariant)));

        updateActions.addAll(
            buildActionsIfPassesFilter(syncFilter, PRICES, () ->
                buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant, syncOptions)));

        updateActions.addAll(
            buildActionsIfPassesFilter(syncFilter, ASSETS, () ->
                buildProductVariantAssetsUpdateActions(oldProductVariant, newProductVariant, syncOptions)));

        buildActionIfPassesFilter(syncFilter, SKU, () ->
            buildProductVariantSkuUpdateAction(oldProductVariant, newProductVariant))
            .ifPresent(updateActions::add);

        return updateActions;
    }

    /**
     * Compares the 'published' field of a {@link ProductDraft} and a {@link Product} and accordingly returns
     * a {@link Publish} or {@link Unpublish} update action as a result in an {@link Optional}. If the new product's
     * 'published' field is null, then the default false value is assumed.
     *
     * <p>If both the {@link Product} and the {@link ProductDraft} have the same 'published' flag value, then no update
     * action is needed and hence an empty {@link Optional} is returned.
     *
     * <p>NOTE: Comparison is done against the staged projection of the old product.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new published field value.
     * @return A filled optional with the update action or an empty optional if the flag values are identical.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildPublishUpdateAction(@Nonnull final Product oldProduct,
                                                                           @Nonnull final ProductDraft newProduct) {
        final Boolean isNewProductPublished = toBoolean(newProduct.isPublish());
        final Boolean isOldProductPublished = toBoolean(oldProduct.getMasterData().isPublished());
        if (Boolean.TRUE.equals(isNewProductPublished)) {
            return buildUpdateAction(isOldProductPublished, isNewProductPublished, Publish::of);
        }
        return buildUpdateAction(isOldProductPublished, isNewProductPublished, Unpublish::of);
    }


    /**
     * Create update action, if {@code newProduct} has {@code #masterVariant#key} different than {@code oldProduct}
     * staged {@code #masterVariant#key}.
     *
     * <p>If update action is created - it is created of
     * {@link ProductVariantDraft newProduct.getMasterVariant().getSku()}
     *
     * <p>If old master variant is missing in the new variants list - add {@link RemoveVariant} action at the end.
     *
     * @param oldProduct  old product with variants
     * @param newProduct  new product draft with variants <b>with resolved references prices references</b>
     * @param syncOptions the sync options wrapper which contains options related to the sync process
     * @return a list of maximum two elements: {@link ChangeMasterVariant} if the keys are different,
     *     optionally followed by {@link RemoveVariant} if the changed variant does not exist in the new variants list.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildChangeMasterVariantUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct,
            @Nonnull final ProductSyncOptions syncOptions) {
        final String newKey = newProduct.getMasterVariant().getKey();
        final String oldKey = oldProduct.getMasterData().getStaged().getMasterVariant().getKey();

        if (haveInvalidMasterVariants(oldProduct, newProduct, syncOptions)) {
            return emptyList();
        }

        return buildUpdateActions(newKey, oldKey,
            // it might be that the new master variant is from new added variants, so CTP variantId is not set yet,
            // thus we can't use ChangeMasterVariant.ofVariantId(),
            // but it could be re-factored as soon as ChangeMasterVariant.ofKey() happens in the SDK
            () -> {

                final String newSku = newProduct.getMasterVariant().getSku();
                if (isBlank(newSku)) {
                    handleBuildVariantsUpdateActionsError(oldProduct, BLANK_NEW_MASTER_VARIANT_SKU, syncOptions);
                    return emptyList();
                }

                final List<UpdateAction<Product>> updateActions = new ArrayList<>(2);
                updateActions.add(ChangeMasterVariant.ofSku(newSku, true));

                // verify whether the old master variant should be removed:
                // if the new variant list doesn't contain the old master variant key.
                // Since we can't remove a variant, if it is master, we have to change MV first, and then remove it
                // (if it does not exist in the new variants list).
                // We don't need to include new master variant to the iteration stream iteration,
                // because this body is called only if newKey != oldKey
                if (newProduct.getVariants().stream()
                    .noneMatch(variant -> Objects.equals(variant.getKey(), oldKey))) {
                    updateActions.add(RemoveVariant.of(oldProduct.getMasterData().getStaged().getMasterVariant()));
                }
                return updateActions;
            });
    }


    /**
     * Compares the {@link io.sphere.sdk.taxcategories.TaxCategory} references of an old {@link Product} and
     * new {@link ProductDraft}. If they are different - return {@link SetTaxCategory} update action.
     *
     * <p>If the old value is set, but the new one is empty - the command will unset the tax category.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft with new {@link io.sphere.sdk.taxcategories.TaxCategory} reference.
     * @return An optional with {@link SetTaxCategory} update action.
     */
    @Nonnull
    public static Optional<SetTaxCategory> buildSetTaxCategoryUpdateAction(
        @Nonnull final Product oldProduct,
        @Nonnull final ProductDraft newProduct) {
        return buildUpdateAction(oldProduct.getTaxCategory(), newProduct.getTaxCategory(),
            () -> SetTaxCategory.of(newProduct.getTaxCategory()));
    }

    /**
     * Compares the {@link State} references of an old {@link Product} and
     * new {@link ProductDraft}. If they are different - return {@link TransitionState} update action.
     *
     * <p>If the old value is set, but the new one is empty - return empty object, because unset transition state is
     * not possible.
     *
     * <p><b>Note:</b> the transition state action is called with <i>force == true</i>, i.e. the platform won't verify
     * transition
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft with new {@link State} reference.
     * @return An optional with {@link TransitionState} update action.
     */
    @Nonnull
    public static Optional<TransitionState> buildTransitionStateUpdateAction(
        @Nonnull final Product oldProduct,
        @Nonnull final ProductDraft newProduct) {
        return ofNullable(newProduct.getState() != null && !Objects.equals(oldProduct.getState(), newProduct.getState())
            ? TransitionState.of(newProduct.getState(), true)
            : null);
    }

    /**
     * Factory method to create {@link AddVariant} action from {@link ProductVariantDraft} instance.
     *
     * <p>The {@link AddVariant} will include:<ul>
     * <li>sku</li>
     * <li>keys</li>
     * <li>attributes</li>
     * <li>prices</li>
     * <li>images</li>
     * </ul>
     *
     * @param draft {@link ProductVariantDraft} which to add.
     * @return new {@link AddVariant} update action with properties from {@code draft}
     */
    @Nonnull
    static AddVariant buildAddVariantUpdateActionFromDraft(@Nonnull final ProductVariantDraft draft) {
        return AddVariant.of(draft.getAttributes(), draft.getPrices(), draft.getSku(), true)
                         .withKey(draft.getKey())
                         .withImages(draft.getImages());
    }

    /**
     * Util that checks if the supplied {@link ActionGroup} passes the {@link SyncFilter}, then it returns the result
     * of the {@code updateActionSupplier} execution, otherwise it returns an empty {@link Optional}.
     *
     * @param syncFilter           the sync filter to check the {@code actionGroup} against.
     * @param actionGroup          the action group to check against the filter.
     * @param updateActionSupplier the supplier to execute if the {@code actionGroup} passes the filter.
     * @param <T>                  the type of the content of the returned {@link Optional}.
     * @return the result of the {@code updateActionSupplier} execution if the {@code actionGroup} passes the
     *         {@code syncFilter}, otherwise it returns an empty {@link Optional}.
     */
    @Nonnull
    static <T> Optional<T> buildActionIfPassesFilter(
        @Nonnull final SyncFilter syncFilter,
        @Nonnull final ActionGroup actionGroup,
        @Nonnull final Supplier<Optional<T>> updateActionSupplier) {
        return executeSupplierIfPassesFilter(syncFilter, actionGroup, updateActionSupplier, Optional::empty);
    }

    /**
     * Util that checks if the supplied {@link ActionGroup} passes the {@link SyncFilter}, then it returns the result
     * of the {@code updateActionSupplier} execution, otherwise it returns an empty {@link List}.
     *
     * @param syncFilter the sync filter to check the {@code actionGroup} against.
     * @param actionGroup the action group to check against the filter.
     * @param updateActionSupplier the supplier to execute if the {@code actionGroup} passes the filter.
     * @param <T> the type of the content of the returned {@link List}.
     * @return the result of the {@code updateActionSupplier} execution if the {@code actionGroup} passes the
     *         {@code syncFilter}, otherwise it returns an empty {@link List}.
     */
    @Nonnull
    static <T> List<T> buildActionsIfPassesFilter(
        @Nonnull final SyncFilter syncFilter,
        @Nonnull final ActionGroup actionGroup,
        @Nonnull final Supplier<List<T>> updateActionSupplier) {
        return executeSupplierIfPassesFilter(syncFilter, actionGroup, updateActionSupplier, Collections::emptyList);
    }

    /**
     * Validate both old and new product have master variant with significant key.
     *
     * <p>If at least on of the master variants key not found - the error is reported to {@code syncOptions} and
     * <b>true</b> is returned.
     *
     * @param oldProduct  old product to verify
     * @param newProduct  new product to verify
     * @param syncOptions {@link BaseSyncOptions#applyErrorCallback(String, Throwable) applyErrorCallback} holder
     * @return <b>true</b> if at least one of the products have invalid (null/blank) master variant or key.
     */
    private static boolean haveInvalidMasterVariants(@Nonnull final Product oldProduct,
                                                     @Nonnull final ProductDraft newProduct,
                                                     @Nonnull final ProductSyncOptions syncOptions) {
        boolean hasError = false;

        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        if (isBlank(oldMasterVariant.getKey())) {
            handleBuildVariantsUpdateActionsError(oldProduct, BLANK_OLD_MASTER_VARIANT_KEY, syncOptions);
            hasError = true;
        }

        final ProductVariantDraft newMasterVariant = newProduct.getMasterVariant();
        if (newMasterVariant == null || isBlank(newMasterVariant.getKey())) {
            handleBuildVariantsUpdateActionsError(oldProduct, BLANK_NEW_MASTER_VARIANT_KEY, syncOptions);
            hasError = true;
        }

        return hasError;
    }

    /**
     * Apply error message to the {@code syncOptions}, reporting the product key and {@code reason}
     *
     * @param product     product which has sync error
     * @param reason      reason to specify in the error message.
     * @param syncOptions {@link BaseSyncOptions#applyErrorCallback(String, Throwable) applyErrorCallback} holder
     */
    private static void handleBuildVariantsUpdateActionsError(@Nonnull final Product product,
                                                              @Nonnull final String reason,
                                                              @Nonnull final ProductSyncOptions syncOptions) {
        syncOptions.applyErrorCallback(format("Failed to build variants update actions on the product with key '%s'. "
                + "Reason: %s",
            product.getKey(), reason));
    }

    private ProductUpdateActionUtils() {
    }
}
