package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductVariantAttribute;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
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
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.search.SearchKeywords;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToSet;
import static com.commercetools.sync.commons.utils.CollectionUtils.filterCollection;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateAction;
import static com.commercetools.sync.commons.utils.CommonTypeUpdateActionUtils.buildUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAttributesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantImagesUpdateActions;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantPricesUpdateActions;
import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

// TODO: Update JavaDocs according to changes, tests..
public final class ProductUpdateActionUtils {

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
    public static Optional<UpdateAction<Product>> buildSetDescriptionUpdateAction(@Nonnull final Product oldProduct,
                                                                                  @Nonnull final ProductDraft
                                                                                      newProduct) {
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
        final Set<Reference<Category>> newCategories = newProduct.getCategories();
        final Set<Reference<Category>> oldCategories = oldProduct.getMasterData().getStaged().getCategories();
        return buildUpdateActions(oldCategories, newCategories,
            () -> {
                final List<UpdateAction<Product>> updateActions = new ArrayList<>();
                subtract(newCategories, oldCategories).forEach(category ->
                    updateActions.add(AddToCategory.of(category, true)));
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
    public static List<UpdateAction<Product>> buildSetCategoryOrderHintUpdateActions(@Nonnull final Product oldProduct,
                                                                                     @Nonnull final ProductDraft
                                                                                         newProduct) {
        final CategoryOrderHints newCategoryOrderHints = newProduct.getCategoryOrderHints();
        final CategoryOrderHints oldCategoryOrderHints = oldProduct.getMasterData().getStaged().getCategoryOrderHints();
        return buildUpdateActions(oldCategoryOrderHints, newCategoryOrderHints, () -> {

            final Set<String> newCategoryIds = newProduct.getCategories().stream()
                                                         .map(Reference::getId)
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
    public static List<UpdateAction<Product>> buildRemoveFromCategoryUpdateActions(@Nonnull final Product oldProduct,
                                                                                   @Nonnull final ProductDraft
                                                                                       newProduct) {
        final Set<Reference<Category>> newCategories = newProduct.getCategories();
        final Set<Reference<Category>> oldCategories = oldProduct.getMasterData().getStaged().getCategories();
        return buildUpdateActions(oldCategories, newCategories, () -> {
            final List<UpdateAction<Product>> updateActions = new ArrayList<>();
            subtract(oldCategories, newCategories).forEach(category ->
                updateActions.add(RemoveFromCategory.of(category, true)));
            return updateActions;
        });
    }

    /**
     * Returns a set containing everything in {@code set1} but not in {@code set2}.
     *
     * @param set1 defines the first set.
     * @param set2 defines the second set.
     * @return a set containing everything in {@code set1} but not in {@code set2}.
     */
    @Nonnull
    private static Set<Reference<Category>> subtract(@Nonnull final Set<Reference<Category>> set1,
                                                     @Nonnull final Set<Reference<Category>> set2) {
        final Set<Reference<Category>> difference = new HashSet<>(set1);
        difference.removeAll(set2);
        return difference;
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
    public static Optional<UpdateAction<Product>> buildSetSearchKeywordsUpdateAction(@Nonnull final Product oldProduct,
                                                                                     @Nonnull final ProductDraft
                                                                                         newProduct) {
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
    public static Optional<UpdateAction<Product>> buildSetMetaDescriptionUpdateAction(@Nonnull final Product oldProduct,
                                                                                      @Nonnull final ProductDraft
                                                                                          newProduct) {
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
    public static Optional<UpdateAction<Product>> buildSetMetaKeywordsUpdateAction(@Nonnull final Product oldProduct,
                                                                                   @Nonnull final ProductDraft
                                                                                       newProduct) {
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
    public static Optional<UpdateAction<Product>> buildSetMetaTitleUpdateAction(@Nonnull final Product oldProduct,
                                                                                @Nonnull final ProductDraft
                                                                                    newProduct) {
        final LocalizedString newMetaTitle = newProduct.getMetaTitle();
        final LocalizedString oldMetaTitle = oldProduct.getMasterData().getStaged().getMetaTitle();
        return buildUpdateAction(oldMetaTitle, newMetaTitle, () -> SetMetaTitle.of(newMetaTitle));
    }

    /**
     * Compares the variants of a {@link ProductDraft} and a {@link Product}. TODO: CONTINUE DOCUMENTATION.
     *
     * @param oldProduct TODO
     * @param newProduct TODO
     * @param syncOptions TODO
     * @param attributesMetaData TODO
     * @return TODO
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildVariantsUpdateActions(@Nonnull final Product oldProduct,
                                                                         @Nonnull final ProductDraft
                                                                            newProduct,
                                                                         @Nonnull final ProductSyncOptions
                                                                                 syncOptions,
                                                                         @Nonnull
                                                                             final Map<String, ProductVariantAttribute>
                                                                                 attributesMetaData) {
        final List<UpdateAction<Product>> updateActions = new ArrayList<>();
        final List<ProductVariant> oldProductVariants = oldProduct.getMasterData().getStaged().getAllVariants();
        final List<ProductVariantDraft> newProductVariants = newProduct.getVariants();
        newProductVariants.stream()
                          .filter(Objects::nonNull)
                          .filter(newProductVariant -> Objects.nonNull(newProductVariant.getKey()))//TODO: WARN NO KEYS!
                          .forEach(newProductVariant ->
                              getVariantByKey(oldProductVariants, newProductVariant.getKey())
                                  .ifPresent(oldProductVariant -> {
                                      updateActions.addAll(buildProductVariantAttributesUpdateActions(oldProductVariant,
                                          newProductVariant, syncOptions, attributesMetaData));
                                      updateActions.addAll(buildProductVariantImagesUpdateActions(oldProductVariant,
                                          newProductVariant, syncOptions));
                                      updateActions.addAll(buildProductVariantPricesUpdateActions(oldProductVariant,
                                          newProductVariant, syncOptions));
                                  }));
        return updateActions;
    }

    private static Optional<ProductVariant> getVariantByKey(@Nonnull final List<ProductVariant> productVariants,
                                                            @Nonnull final String key) {
        return productVariants.stream()
                              .filter(productVariant -> Objects.equals(productVariant.getKey(), key))
                              .findFirst();
    }

    /**
     * <b>Note:</b> if you do both add/remove product variants - <b>first always remove the variants</b>,
     * and then - add.
     * The reason of such restriction: if you add first you could have duplication exception on the keys,
     * which expected to be removed first.
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return list of update actions to remove missing variants.
     * @see #buildAddVariantUpdateAction(Product, ProductDraft)
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildRemoveVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                             @Nonnull final ProductDraft newProduct) {
        final List<ProductVariant> oldVariants = oldProduct.getMasterData().getStaged().getVariants();
        final Set<String> newVariants = collectionToSet(newProduct.getVariants(),
                ProductVariantDraft::getKey);

        return filterCollection(oldVariants, oldVariant -> !newVariants.contains(oldVariant.getKey()))
                .map(RemoveVariant::of)
                .collect(toList());
    }

    /**
     * <b>Note:</b> if you do both add/remove product variants - <b>first always remove the variants</b>,
     * and then - add.
     * The reason of such restriction: if you add first you could have duplication exception on the keys,
     * which expected to be removed first.
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return list of update actions to add new variants.
     * @see ProductUpdateActionUtils#buildRemoveVariantUpdateAction(Product, ProductDraft)
     */
    @Nonnull
    public static List<UpdateAction<Product>> buildAddVariantUpdateAction(@Nonnull final Product oldProduct,
                                                                          @Nonnull final ProductDraft newProduct) {
        final List<ProductVariantDraft> newVariants = newProduct.getVariants();
        final Set<String> oldVariantKeys =
                collectionToSet(oldProduct.getMasterData().getStaged().getVariants(), ProductVariant::getKey);

        return filterCollection(newVariants, newVariant -> !oldVariantKeys.contains(newVariant.getKey()))
                .map(ProductUpdateActionUtils::buildAddVariantUpdateActionFromDraft)
                .collect(toList());
    }

    /**
     * Create update action, if {@code newProduct} has {@code #masterVariant#key} different than {@code oldProduct}
     * staged {@code #masterVariant#key}.
     *
     * <p>If update action is created - it is created of
     * {@link ProductVariantDraft newProduct.getMasterVariant().getSku()}
     *
     * @param oldProduct old product with variants
     * @param newProduct new product draft with variants <b>with resolved references prices references</b>
     * @return {@link ChangeMasterVariant} if the keys are different.
     */
    @Nonnull
    public static Optional<UpdateAction<Product>> buildChangeMasterVariantUpdateAction(
            @Nonnull final Product oldProduct,
            @Nonnull final ProductDraft newProduct) {
        final String newKey = newProduct.getMasterVariant().getKey();
        final String oldKey = oldProduct.getMasterData().getStaged().getMasterVariant().getKey();
        return buildUpdateAction(newKey, oldKey,
            // it might be that the new master variant is from new added variants, so CTP variantId is not set yet,
            // thus we can't use ChangeMasterVariant.ofVariantId()
            () -> ChangeMasterVariant.ofSku(newProduct.getMasterVariant().getSku()));
    }

    /**
     * Factory method to create {@link AddVariant} action from {@link ProductVariantDraft} instance.
     *
     * <p>The {@link AddVariant} will include:<ul>
     *     <li>sku</li>
     *     <li>keys</li>
     *     <li>attributes</li>
     *     <li>prices</li>
     *     <li>images</li>
     * </ul>
     *
     * @param draft {@link ProductVariantDraft} which to add.
     * @return new {@link AddVariant} update action with properties from {@code draft}
     */
    @Nonnull
    public static AddVariant buildAddVariantUpdateActionFromDraft(@Nonnull final ProductVariantDraft draft) {
        return AddVariant.of(draft.getAttributes(), draft.getPrices(), draft.getSku())
                         .withKey(draft.getKey())
                         .withImages(draft.getImages());
    }
}
