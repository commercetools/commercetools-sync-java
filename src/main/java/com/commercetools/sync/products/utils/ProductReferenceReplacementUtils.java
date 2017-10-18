package com.commercetools.sync.products.utils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.expansion.ExpansionPath;
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
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
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
     * Takes a list of Products that are supposed to have their product type, tax category, state and  category
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
                final ProductDraft productDraftWithCategoryKeys =
                    replaceProductDraftCategoryReferenceIdsWithKeys(productDraft);

                final Reference<ProductType> productTypeReferenceWithKey =
                    replaceProductTypeReferenceIdWithKey(product);
                final Reference<TaxCategory> taxCategoryReferenceWithKey =
                    replaceTaxCategoryReferenceIdWithKey(product);
                final Reference<State> stateReferenceWithKey =
                    replaceProductStateReferenceIdWithKey(product);

                return ProductDraftBuilder.of(productDraftWithCategoryKeys)
                                          .productType(productTypeReferenceWithKey)
                                          .taxCategory(taxCategoryReferenceWithKey)
                                          .state(stateReferenceWithKey)
                                          .build();
            })
            .collect(Collectors.toList());
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
     * Builds a {@link ProductQuery} for fetching products from a source CTP project with all the needed references
     * expanded for the sync:
     * <ul>
     *     <li>Product Type</li>
     *     <li>Tax Category</li>
     *     <li>Product State</li>
     *     <li>Staged Product Categories</li>
     *     <li>Staged Price Channels</li>
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
                           .plusExpansionPaths(productExpansionModel ->
                               productExpansionModel.masterData().staged().categories())
                           .plusExpansionPaths(channelExpansionModel ->
                               channelExpansionModel.masterData().staged().allVariants().prices().channel());
    }

    /**
     * ============================== Tested/document so far.. ========================================================
     * ================================================================================================================
     */

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
                                    .map(ProductReferenceReplacementUtils::
                                        replaceProductVariantDraftPriceChannelIdsWithKeys)
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
                              .map(ProductReferenceReplacementUtils::replacePriceDraftChannelIdWithKey)
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
            .state(product.getState())
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
