package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.products.ActionGroup;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.SyncFilter;
import com.commercetools.sync.products.UpdateFilterType;
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
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import io.sphere.sdk.search.SearchKeywords;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantImagesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantPricesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateActions;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isBlank;

// TODO: Update JavaDocs according to changes, tests..
public final class ProductUpdateActionUtils {
    private static final String FAILED_TO_BUILD_VARIANTS_ATTRIBUTES_UPDATE_ACTIONS = "Failed to build "
            + "setAttribute/setAttributeInAllVariants update actions for the attributes of a ProductVariantDraft on the"
            + " product with key '%s'. Reason: %s";
    private static final String BLANK_VARIANT_KEY = "The variant key is blank.";
    private static final String NULL_VARIANT = "The variant is null.";

    /**
     * Compares the {@link LocalizedString} names of a {@link ProductDraft} and a {@link Product}. The name of the
     * product is either fetched from it's current or staged projection based on the whether the {@code updateStaged}
     * flag configured in the {@code syncOptions} supplied as a parameter to the method. If the {@code updateStaged} is
     * set to {@code true}, then the staged projection of the product is used for comparison. If the
     * {@code updateStaged} is set to {@code false}, then the current projection of the product is used for comparison.
     *
     * <p>Then it returns an {@link ChangeName} as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same name, then no update action is needed and
     * hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new name.
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
     * Compares the {@link LocalizedString} descriptions of a {@link ProductDraft} and a {@link Product}. The
     * description of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns an {@link SetDescription} as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same description, then no update action is
     * needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new description.
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
     * Compares the {@link LocalizedString} slugs of a {@link ProductDraft} and a {@link Product}. The
     * slug of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link ChangeSlug} update action as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same slug, then no update action is
     * needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new slug.
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
     * The categories of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link List} of {@link AddToCategory} update actions as a result, if the old product
     * needs to be added to a category to have the same set of categories as the new product.
     * If both the {@link Product} and the {@link ProductDraft} have the same set of categories, then no update action
     * is  needed and hence an empty {@link List} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new slug.
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
     * Compares the {@link CategoryOrderHints} of a {@link ProductDraft} and a {@link Product}. The categoryOrderHints
     * of the  product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link SetCategoryOrderHint} update action as a result in an {@link List}.
     * If both the {@link Product} and the {@link ProductDraft} have the same categoryOrderHints, then no update action
     * is needed and hence an empty {@link List} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new categoryOrderHints.
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
     * The categories of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link List} of {@link RemoveFromCategory} update actions as a result, if the old product
     * needs to be removed from a category to have the same set of categories as the new product.
     * If both the {@link Product} and the {@link ProductDraft} have the same set of categories, then no update action
     * is  needed and hence an empty {@link List} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new slug.
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
     * Compares the {@link SearchKeywords} of a {@link ProductDraft} and a {@link Product}. The search keywords of the
     * product is either fetched from it's current or staged projection based on the whether the {@code updateStaged}
     * flag configured in the {@code syncOptions} supplied as a parameter to the method. If the {@code updateStaged} is
     * set to {@code true}, then the staged projection of the product is used for comparison. If the
     * {@code updateStaged} is set to {@code false}, then the current projection of the product is used for comparison.
     *
     * <p>Then it returns a {@link SetSearchKeywords} update action as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same search keywords, then no update action is
     * needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new search keywords.
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
     * Compares the {@link LocalizedString} meta descriptions of a {@link ProductDraft} and a {@link Product}. The
     * meta description of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link ChangeSlug} update action as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same meta description, then no update action is
     * needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new meta description.
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
     * Compares the {@link LocalizedString} meta keywordss of a {@link ProductDraft} and a {@link Product}. The
     * meta keywords of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link ChangeSlug} update action as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same meta keywords, then no update action is
     * needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new meta keywords.
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
     * Compares the {@link LocalizedString} meta titles of a {@link ProductDraft} and a {@link Product}. The
     * meta title of the product is either fetched from it's current or staged projection based on the whether the
     * {@code updateStaged} flag configured in the {@code syncOptions} supplied as a parameter to the method. If the
     * {@code updateStaged} is set to {@code true}, then the staged projection of the product is used for comparison. If
     * the {@code updateStaged} is set to {@code false}, then the current projection of the product is used for
     * comparison.
     *
     * <p>Then it returns a {@link ChangeSlug} update action as a result in an {@link Optional}.
     * If both the {@link Product} and the {@link ProductDraft} have the same meta title, then no update action is
     * needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the category which should be updated.
     * @param newProduct the category draft where we get the new meta title.
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
     * Compares the variants of a {@link ProductDraft} and a {@link Product}. TODO: CONTINUE DOCUMENTATION.
     *
     * @param oldProduct         TODO
     * @param newProduct         TODO
     * @param syncOptions        TODO
     * @param attributesMetaData TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildVariantsUpdateActions(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct,
            @Nonnull final ProductSyncOptions syncOptions,
            @Nonnull final Map<String, AttributeMetaData> attributesMetaData) {

        final List<UpdateAction<Product>> updateActions = new ArrayList<>();

        final Map<String, ProductVariant> oldProductVariantsNoMaster =
            collectionToMap(oldProduct.getMasterData().getStaged().getVariants(), ProductVariant::getKey);

        final Map<String, ProductVariant> oldProductVariantsWithMaster = new HashMap<>(oldProductVariantsNoMaster);
        ProductVariant masterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        oldProductVariantsWithMaster.put(masterVariant.getKey(), masterVariant);

        final List<ProductVariantDraft> newAllProductVariants = new ArrayList<>(newProduct.getVariants());
        newAllProductVariants.add(newProduct.getMasterVariant());

        // 1. Remove missing variants, but keep master variant (MV can't be removed)
        updateActions.addAll(buildRemoveVariantUpdateActions(oldProductVariantsNoMaster, newAllProductVariants));

        for (ProductVariantDraft newProductVariant : newAllProductVariants) {
            if (newProductVariant == null) {
                final String errorMessage = format(FAILED_TO_BUILD_VARIANTS_ATTRIBUTES_UPDATE_ACTIONS,
                    oldProduct.getKey(), NULL_VARIANT);
                syncOptions.applyErrorCallback(errorMessage, new BuildUpdateActionException(errorMessage));
                continue;
            }

            final String newProductVariantKey = newProductVariant.getKey();
            if (isBlank(newProductVariantKey)) {
                final String errorMessage = format(FAILED_TO_BUILD_VARIANTS_ATTRIBUTES_UPDATE_ACTIONS,
                    oldProduct.getKey(), BLANK_VARIANT_KEY);
                syncOptions.applyErrorCallback(errorMessage, new BuildUpdateActionException(errorMessage));
                continue;
            }

            // 2.1 if both old/new variants lists have an item with the same key - create update actions for the variant
            // 2.2 otherwise - add missing variant
            List<UpdateAction<Product>> updateOrAddVariant =
                ofNullable(oldProductVariantsWithMaster.get(newProductVariantKey))
                    .map(oldProductVariant -> collectAllVariantUpdateActions(oldProduct, oldProductVariant,
                        newProductVariant, attributesMetaData, syncOptions))
                    .orElseGet(() -> singletonList(buildAddVariantUpdateActionFromDraft(newProductVariant)));

            updateActions.addAll(updateOrAddVariant);
        }

        // 3. change master variant and remove previous one, if necessary
        updateActions.addAll(buildChangeMasterVariantUpdateAction(oldProduct, newProduct));

        return updateActions;
    }

    private static List<UpdateAction<Product>> collectAllVariantUpdateActions(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductVariant oldProductVariant,
            @Nonnull final ProductVariantDraft newProductVariant,
            @Nonnull final Map<String, AttributeMetaData> attributesMetaData,
            @Nonnull final ProductSyncOptions syncOptions) {
        final ArrayList<UpdateAction<Product>> updateActions = new ArrayList<>();
        final SyncFilter syncFilter = syncOptions.getSyncFilter();
        updateActions.addAll(
                buildActionsIfPassesFilter(syncFilter, ActionGroup.ATTRIBUTES, () ->
                        buildProductVariantAttributesUpdateActions(oldProduct.getKey(), oldProductVariant,
                                newProductVariant, attributesMetaData, syncOptions)));
        updateActions.addAll(
                buildActionsIfPassesFilter(syncFilter, ActionGroup.IMAGES, () ->
                        buildProductVariantImagesUpdateActions(oldProductVariant, newProductVariant)));
        updateActions.addAll(
                buildActionsIfPassesFilter(syncFilter, ActionGroup.PRICES, () ->
                        buildProductVariantPricesUpdateActions(oldProductVariant, newProductVariant)));
        updateActions.addAll(buildProductVariantSkuUpdateActions(oldProductVariant, newProductVariant));
        return updateActions;
    }

    /**
     * <b>Note:</b> if you do both add/remove product variants - <b>first always remove the variants</b>,
     * and then - add.
     * If you add first, the update action could fail due to having a duplicate {@code key} or {@code sku} with variants
     * which were expected to be removed anyways. So issuing remove update action first will fix such issue.
     *
     * @param oldProductVariants [variantKey-variant] map of old variants. Master variant must be included.
     * @param newProductVariants list of new product variants list <b>with resolved references prices references</b>.
     *                            Master variant must be included.
     * @return list of update actions to remove missing variants.
     */
    @Nonnull
    public static List<RemoveVariant> buildRemoveVariantUpdateActions(
            @Nonnull final Map<String, ProductVariant> oldProductVariants,
            @Nonnull final List<ProductVariantDraft> newProductVariants) {

        // copy the map and remove from the copy duplicate items
        Map<String, ProductVariant> productsToRemove = new HashMap<>(oldProductVariants);

        for (ProductVariantDraft newVariant : newProductVariants) {
            if (productsToRemove.containsKey(newVariant.getKey())) {
                productsToRemove.remove(newVariant.getKey());
            }
        } // now productsToRemove contains only items which don't exist in newProductVariants

        return productsToRemove.values().stream()
            .map(RemoveVariant::of)
            .collect(toList());
    }

    /**
     * Compares the 'published' field of a {@link ProductDraft} and a {@link Product} and accordingly returns
     * a {@link Publish} or {@link Unpublish} update action as a result in an {@link Optional}. If the new product's
     * 'published' field is null, then the default false value is assumed.
     *
     * <p>If both the {@link Product} and the {@link ProductDraft} have the same 'published' flag value, then no update
     * action is needed and hence an empty {@link Optional} is returned.
     *
     * @param oldProduct the product which should be updated.
     * @param newProduct the product draft where we get the new meta description.
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
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return a list of maximum two elements: {@link ChangeMasterVariant} if the keys are different,
     *     optionally followed by {@link RemoveVariant} if the changed variant does not exist in the new variants list.
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildChangeMasterVariantUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final String newKey = newProduct.getMasterVariant().getKey();
        final String oldKey = oldProduct.getMasterData().getStaged().getMasterVariant().getKey();
        return buildUpdateActions(newKey, oldKey,
            // it might be that the new master variant is from new added variants, so CTP variantId is not set yet,
            // thus we can't use ChangeMasterVariant.ofVariantId(),
            // but it could be re-factored as soon as ChangeMasterVariant.ofKey() happens in the SDK
            () -> {
                final List<UpdateAction<Product>> updateActions = new ArrayList<>(2);
                updateActions.add(ChangeMasterVariant.ofSku(newProduct.getMasterVariant().getSku(), true));

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
        return AddVariant.of(draft.getAttributes(), draft.getPrices(), draft.getSku())
            .withKey(draft.getKey())
            .withImages(draft.getImages());
    }

    static Optional<UpdateAction<Product>> buildActionIfPassesFilter(
            @Nullable final SyncFilter syncFilter,
            @Nonnull final ActionGroup filter,
            @Nonnull final Supplier<Optional<UpdateAction<Product>>> updateActionSupplier) {
        return buildSupplierResultIfPassesFilter(syncFilter, filter, updateActionSupplier, empty());
    }

    static List<UpdateAction<Product>> buildActionsIfPassesFilter(
            @Nullable final SyncFilter syncFilter,
            @Nonnull final ActionGroup filter,
            @Nonnull final Supplier<List<UpdateAction<Product>>> updateActionSupplier) {
        return buildSupplierResultIfPassesFilter(syncFilter, filter, updateActionSupplier, emptyList());
    }

    private static <T> T buildSupplierResultIfPassesFilter(
            @Nullable final SyncFilter syncFilter, @Nonnull final ActionGroup filter,
            @Nonnull final Supplier<T> updateActionSupplier, @Nonnull final T emptyResult) {
        if (syncFilter == null) {
            // If there is no filter, attempt to build an update action.
            return updateActionSupplier.get();
        }
        final UpdateFilterType filterType = syncFilter.getFilterType();
        if (filterType.equals(UpdateFilterType.BLACKLIST)) {
            final List<ActionGroup> blackList = syncFilter.getFilters();
            return !blackList.contains(filter) ? updateActionSupplier.get() : emptyResult;
        }

        final List<ActionGroup> whiteList = syncFilter.getFilters();
        return whiteList.contains(filter) ? updateActionSupplier.get() : emptyResult;
    }
}
