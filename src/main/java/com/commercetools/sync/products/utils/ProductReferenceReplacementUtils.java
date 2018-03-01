package com.commercetools.sync.products.utils;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.replaceReferenceIdWithKey;
import static java.util.stream.Collectors.toList;

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
                final Reference<ProductType> productTypeReferenceWithKey =
                    replaceProductTypeReferenceIdWithKey(product);
                final Reference<TaxCategory> taxCategoryReferenceWithKey =
                    replaceTaxCategoryReferenceIdWithKey(product);
                final Reference<State> stateReferenceWithKey = replaceProductStateReferenceIdWithKey(product);

                final CategoryReferencePair categoryReferencePair = replaceCategoryReferencesIdsWithKeys(product);
                final List<Reference<Category>> categoryReferencesWithKeys =
                    categoryReferencePair.getCategoryReferences();
                final CategoryOrderHints categoryOrderHintsWithKeys = categoryReferencePair.getCategoryOrderHints();

                final List<ProductVariant> allVariants = product.getMasterData().getStaged().getAllVariants();
                final List<ProductVariantDraft> variantDraftsWithKeys =
                    VariantReferenceReplacementUtils.replaceVariantsReferenceIdsWithKeys(allVariants);
                final ProductVariantDraft masterVariantDraftWithKeys = variantDraftsWithKeys.remove(0);

                return ProductDraftBuilder.of(productDraft)
                                          .masterVariant(masterVariantDraftWithKeys)
                                          .variants(variantDraftsWithKeys)
                                          .productType(productTypeReferenceWithKey)
                                          .categories(categoryReferencesWithKeys)
                                          .categoryOrderHints(categoryOrderHintsWithKeys)
                                          .taxCategory(taxCategoryReferenceWithKey)
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

    /**
     * Takes a product that is supposed to have its ProductType reference expanded in order to be able to fetch the key
     * and replace the reference id with the corresponding key and then return a new {@link ProductType}
     * {@link Reference} containing the key in the id field.
     *
     * <p><b>Note:</b> The productType reference should be expanded for the {@code product}, otherwise the reference
     * id will not be replaced with key and will still have the id in place.
     *
     * @param product the product to replace its ProductType reference id with the key.
     *
     * @return a new {@link ProductType} {@link Reference} containing the key in the id field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Reference<ProductType> replaceProductTypeReferenceIdWithKey(@Nonnull final Product product) {
        final Reference<ProductType> productType = product.getProductType();
        return replaceReferenceIdWithKey(productType, () -> ProductType.referenceOfId(productType.getObj().getKey()));
    }

    /**
     * Takes a product that is supposed to have its TaxCategory reference expanded in order to be able to fetch the key
     * and replace the reference id with the corresponding key and then return a new {@link TaxCategory}
     * {@link Reference} containing the key in the id field.
     *
     * <p><b>Note:</b> The TaxCategory reference should be expanded for the {@code product}, otherwise the reference
     * id will not be replaced with the key and will still have the id in place.
     *
     * @param product the product to replace its TaxCategory reference id with the key.
     *
     * @return a new {@link TaxCategory} {@link Reference} containing the key in the id field.
     */
    @Nullable
    @SuppressWarnings("ConstantConditions") // NPE cannot occur due to being checked in replaceReferenceIdWithKey
    static Reference<TaxCategory> replaceTaxCategoryReferenceIdWithKey(@Nonnull final Product product) {
        final Reference<TaxCategory> productTaxCategory = product.getTaxCategory();
        return replaceReferenceIdWithKey(productTaxCategory,
            () -> TaxCategory.referenceOfId(productTaxCategory.getObj().getKey()));
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
        return replaceReferenceIdWithKey(productState, () -> State.referenceOfId(productState.getObj().getKey()));
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
     * @return a {@link CategoryReferencePair} that contains a {@link List} of category references with keys replacing
     *         the ids and a {@link CategoryOrderHints} with keys replacing the ids.
     */
    @Nonnull
    static CategoryReferencePair replaceCategoryReferencesIdsWithKeys(@Nonnull final Product product) {
        final Set<Reference<Category>> categoryReferences = product.getMasterData().getStaged().getCategories();
        final List<Reference<Category>> categoryReferencesWithKeys = new ArrayList<>();

        final CategoryOrderHints categoryOrderHints = product.getMasterData().getStaged().getCategoryOrderHints();
        final Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();

        categoryReferences.forEach(categoryReference ->
            categoryReferencesWithKeys.add(
                replaceReferenceIdWithKey(categoryReference, () -> {
                    final String categoryId = categoryReference.getId();
                    @SuppressWarnings("ConstantConditions") // NPE is checked in replaceReferenceIdWithKey.
                    final String categoryKey = categoryReference.getObj().getKey();

                    if (categoryOrderHints != null) {
                        final String categoryOrderHintValue = categoryOrderHints.get(categoryId);
                        if (categoryOrderHintValue != null) {
                            categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
                        }
                    }
                    return Category.referenceOfId(categoryKey);
                }))
        );

        final CategoryOrderHints categoryOrderHintsWithKeys = categoryOrderHintsMapWithKeys.isEmpty()
            ? categoryOrderHints : CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
        return CategoryReferencePair.of(categoryReferencesWithKeys, categoryOrderHintsWithKeys);
    }

    /**
     * Builds a {@link ProductQuery} for fetching products from a source CTP project with all the needed references
     * expanded for the sync:
     * <ul>
     *     <li>Product Type</li>
     *     <li>Tax Category</li>
     *     <li>Product State</li>
     *     <li>master variant asset custom types</li>
     *     <li>all variant asset custom types</li>
     *     <li>Staged Product Categories</li>
     *     <li>Staged Price Channels</li>
     *     <li>Reference Attributes</li>
     *     <li>Reference Set Attributes</li>
     * </ul>
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
