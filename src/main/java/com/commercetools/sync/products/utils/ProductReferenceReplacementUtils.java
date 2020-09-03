package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.getReferenceWithKeyReplaced;
import static java.util.stream.Collectors.toList;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class ProductReferenceReplacementUtils {

    /**
     * Takes a list of Products that are supposed to have their product type, tax category, state, variants and category
     * references expanded in order to be able to fetch the keys and replace the reference ids with the corresponding
     * keys and then return a new list of product drafts with their references containing keys instead of the ids.
     *
     * <p><b>Note:</b>If the references are not expanded for a product, the reference ids will not be replaced with keys
     * and will still have their ids in place.
     *
     * @param products the products to replace their reference ids with keys
     * @return a list of products drafts with keys instead of ids for references.
     */
    @Nonnull
    public static List<ProductDraft> replaceProductsReferenceIdsWithKeys(@Nonnull final List<Product> products) {
        return products
            .stream()
            .filter(Objects::nonNull)
            .map(product -> {
                final ProductDraft productDraft = getDraftBuilderFromStagedProduct(product).build();

                final Reference<State> stateReferenceWithKey = replaceProductStateReferenceIdWithKey(product);

                final CategoryReferencePair categoryReferencePair = mapToCategoryReferencePair(product);
                final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
                    categoryReferencePair.getCategoryResourceIdentifiers();
                final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

                final List<ProductVariant> allVariants = product.getMasterData().getStaged().getAllVariants();
                final List<ProductVariantDraft> variantDraftsWithKeys =
                    VariantReferenceReplacementUtils.replaceVariantsReferenceIdsWithKeys(allVariants);
                final ProductVariantDraft masterVariantDraftWithKeys = variantDraftsWithKeys.remove(0);

                return ProductDraftBuilder.of(productDraft)
                                          .masterVariant(masterVariantDraftWithKeys)
                                          .variants(variantDraftsWithKeys)
                                          .productType(mapToProductTypeResourceIdentifier(product.getProductType()))
                                          .categories(categoryResourceIdentifiers)
                                          .categoryOrderHints(categoryOrderHintsWithKeys)
                                          .taxCategory(mapToTaxCategoryResourceIdentifier(product.getTaxCategory()))
                                          .state(stateReferenceWithKey)
                                          .build();
            })
            .collect(Collectors.toList());
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
            .state(product.getState())
            .key(product.getKey())
            .publish(product.getMasterData().isPublished())
            .categories(new ArrayList<>(productData.getCategories()))
            .categoryOrderHints(productData.getCategoryOrderHints());
    }

    @Nullable // todo (ahmetoz) could be refactored to avoid duplication
    private static ResourceIdentifier<ProductType> mapToProductTypeResourceIdentifier(
        @Nullable final Reference<ProductType> productTypeReference) {

        if (productTypeReference != null) {
            if (productTypeReference.getObj() != null) {
                return ResourceIdentifier.ofKey(productTypeReference.getObj().getKey());
            }
            return ResourceIdentifier.ofId(productTypeReference.getId());
        }
        return null;
    }

    @Nullable
    private static ResourceIdentifier<TaxCategory> mapToTaxCategoryResourceIdentifier(
        @Nullable final Reference<TaxCategory> taxCategoryReference) {

        if (taxCategoryReference != null) {
            if (taxCategoryReference.getObj() != null) {
                return ResourceIdentifier.ofKey(taxCategoryReference.getObj().getKey());
            }
            return ResourceIdentifier.ofId(taxCategoryReference.getId());
        }
        return null;
    }

    /**
     * Takes a product that is supposed to have its State reference expanded in order to be able to fetch the key
     * and replace the reference id with the corresponding key and then return a new {@link State} {@link Reference}
     * containing the key in the id field.
     *
     * <p><b>Note:</b> The State reference should be expanded for the {@code product}, otherwise the reference
     * id will not be replaced with the key and will still have the id in place.
     *
     * @param product the product to replace its State reference id with the key.
     *
     * @return a new {@link State} {@link Reference} containing the key in the id field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Reference<State> replaceProductStateReferenceIdWithKey(@Nonnull final Product product) {
        final Reference<State> productState = product.getState();
        return getReferenceWithKeyReplaced(productState,
            () -> State.referenceOfId(productState.getObj().getKey()));
    }

    /**
     * Takes a product that is supposed to have its category references expanded in order to be able to fetch the keys
     * and replace the reference ids with the corresponding keys for both the product's category references and
     * the categoryOrderHints ids. This method returns as a result a {@link CategoryReferencePair} that contains a
     * {@link List} of category references with keys replacing the ids and a {@link CategoryOrderHints} with keys
     * replacing the ids.
     *
     * <p>If the product's categoryOrderHints is null, then the resulting categoryOrderHints will be also null.
     *
     * <p>If the product's category references are not expanded the ids will not be replaced in both the category
     * references and the categoryOrderHints and will be returned as is.
     *
     * @param product the product to replace its category references and CategoryOrderHints ids with keys.
     * @return a {@link CategoryReferencePair} that contains a {@link List} of category resource identifiers with keys
     *         replacing the ids and a {@link CategoryOrderHints} with keys replacing the ids.
     */
    @Nonnull
    static CategoryReferencePair mapToCategoryReferencePair(@Nonnull final Product product) {
        final Set<Reference<Category>> categoryReferences = product.getMasterData().getStaged().getCategories();
        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = new HashSet<>();

        final CategoryOrderHints categoryOrderHints = product.getMasterData().getStaged().getCategoryOrderHints();
        final Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();

        categoryReferences.forEach(categoryReference -> {
            if (categoryReference != null) {
                if (categoryReference.getObj() != null) {
                    final String categoryId = categoryReference.getId();
                    final String categoryKey = categoryReference.getObj().getKey();

                    if (categoryOrderHints != null) {
                        final String categoryOrderHintValue = categoryOrderHints.get(categoryId);
                        if (categoryOrderHintValue != null) {
                            categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
                        }
                    }
                    categoryResourceIdentifiers.add(ResourceIdentifier.ofKey(categoryKey));
                } else {
                    categoryResourceIdentifiers.add(ResourceIdentifier.ofId(categoryReference.getId()));
                }
            }
        });

        final CategoryOrderHints categoryOrderHintsWithKeys = categoryOrderHintsMapWithKeys.isEmpty()
            ? categoryOrderHints : CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
        return CategoryReferencePair.of(categoryResourceIdentifiers, categoryOrderHintsWithKeys);
    }

    /**
     * Builds a {@link ProductQuery} for fetching products from a source CTP project with all the needed references
     * expanded for the sync:
     * <ul>
     *     <li>Product Type</li>
     *     <li>Tax Category</li>
     *     <li>Product State</li>
     *     <li>Staged Assets' Custom Types</li>
     *     <li>Staged Product Categories</li>
     *     <li>Staged Prices' Channels</li>
     *     <li>Staged Prices' Custom Types</li>
     *     <li>Reference Attributes</li>
     *     <li>Reference Set Attributes</li>
     * </ul>
     *
     * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
     * a source commercetools project. Otherwise, it is more efficient to build the query without expansions, if they
     * are not needed, to avoid unnecessarily bigger payloads fetched from the source project.
     *
     * @return the query for fetching products from the source CTP project with all the aforementioned references
     *         expanded.
     */
    @Nonnull
    public static ProductQuery buildProductQuery() {
        return ProductQuery.of()
                           .withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE)
                           .withExpansionPaths(ProductExpansionModel::productType)
                           .plusExpansionPaths(ProductExpansionModel::taxCategory)
                           .plusExpansionPaths(ExpansionPath.of("state"))
                           .plusExpansionPaths(expansionModel ->
                               expansionModel.masterData().staged().categories())
                           .plusExpansionPaths(expansionModel ->
                               expansionModel.masterData().staged().allVariants().prices().channel())
                           .plusExpansionPaths(
                               ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"))
                            .plusExpansionPaths(
                               ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"))
                           .plusExpansionPaths(expansionModel ->
                               expansionModel.masterData().staged().allVariants().attributes().value())
                           .plusExpansionPaths(expansionModel ->
                               expansionModel.masterData().staged().allVariants().attributes().valueSet())
                           .plusExpansionPaths(
                               ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"))
                           .plusExpansionPaths(
                               ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));
    }

    private ProductReferenceReplacementUtils() {
    }
}
