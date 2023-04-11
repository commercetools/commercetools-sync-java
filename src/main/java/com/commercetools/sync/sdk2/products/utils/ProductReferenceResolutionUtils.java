package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static com.commercetools.sync.sdk2.products.utils.VariantReferenceResolutionUtils.toVariantDraft;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.sync.sdk2.commons.helpers.CategoryReferencePair;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class ProductReferenceResolutionUtils {

  /**
   * Returns an {@link java.util.List}&lt;{@link ProductDraft}&gt; consisting of the results of
   * applying the mapping from the staged version of a {@link ProductProjection} to {@link
   * ProductDraft} with considering reference resolution.
   *
   * <table>
   *   <caption>Mapping of Reference fields for the reference resolution</caption>
   *   <thead>
   *     <tr>
   *       <th>Reference field</th>
   *       <th>from</th>
   *       <th>to</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *       <td>productType</td>
   *       <td>{@link com.commercetools.api.models.product_type.ProductTypeReference}</td>
   *       <td>{@link com.commercetools.api.models.product_type.ProductTypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>categories</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.category.CategoryReference}&gt;</td>
   *        <td>{@link java.util.List}&lt;{@link com.commercetools.api.models.category.CategoryResourceIdentifier}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.channel</td>
   *        <td>{@link com.commercetools.api.models.channel.ChannelReference}</td>
   *        <td>{@link com.commercetools.api.models.channel.ChannelResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.customerGroup *</td>
   *        <td>{@link com.commercetools.api.models.customer_group.CustomerGroupReference}</td>
   *        <td>{@link com.commercetools.api.models.customer_group.CustomerGroupReference}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.assets.custom.type</td>
   *        <td>{@link com.commercetools.api.models.type.TypeReference}</td>
   *        <td>{@link com.commercetools.api.models.type.TypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.attributes on {@link java.util.List}&lt;{@link com.commercetools.api.models.product.Attribute} *</td>
   *        <td>{@link com.commercetools.api.models.product_type.ProductTypeReference} (example for ProductType)</td>
   *        <td>{@link com.commercetools.api.models.product_type.ProductTypeResourceIdentifier} (with key replaced with id field)</td>
   *     </tr>
   *     <tr>
   *        <td>taxCategory</td>
   *        <td>{@link com.commercetools.api.models.tax_category.TaxCategoryReference}</td>
   *        <td>{@link com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>state *</td>
   *        <td>{@link com.commercetools.api.models.state.StateReference}</td>
   *        <td>{@link com.commercetools.api.models.state.StateResourceIdentifier}</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should contain Id in the map(cache) with a key
   * value. Any reference that is not available in the map will have its id in place and not
   * replaced by the key. This reference will be considered as existing resources on the target
   * commercetools project and the library will issues an update/create API request without
   * reference resolution.
   *
   * @param products the productprojection (staged) without expansion of references.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @return a {@link java.util.List} of {@link ProductDraft} built from the supplied {@link
   *     java.util.List} of {@link com.commercetools.api.models.product.Product}.
   */
  @Nonnull
  public static List<ProductDraft> mapToProductDrafts(
      @Nonnull final List<ProductProjection> products,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    return products.stream()
        .filter(Objects::nonNull)
        .map(
            product -> {
              final ProductDraft productDraft = getDraftBuilderFromStagedProduct(product).build();

              final CategoryReferencePair categoryReferencePair =
                  mapToCategoryReferencePair(product, referenceIdToKeyCache);
              final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
                  categoryReferencePair.getCategoryResourceIdentifiers();
              final CategoryOrderHints categoryOrderHintsWithKeys =
                  categoryReferencePair.getCategoryOrderHints();

              final List<ProductVariant> allVariants = product.getAllVariants();
              final List<ProductVariantDraft> variantDraftsWithKeys =
                  VariantReferenceResolutionUtils.mapToProductVariantDrafts(
                      allVariants, referenceIdToKeyCache);
              final ProductVariantDraft masterVariantDraftWithKeys =
                  variantDraftsWithKeys.remove(0);

              return ProductDraftBuilder.of(productDraft)
                  .masterVariant(masterVariantDraftWithKeys)
                  .variants(variantDraftsWithKeys)
                  .productType(
                      getResourceIdentifierWithKey(product.getProductType(), referenceIdToKeyCache))
                  .categories(categoryResourceIdentifiers)
                  .categoryOrderHints(categoryOrderHintsWithKeys)
                  .taxCategory(
                      getResourceIdentifierWithKey(product.getTaxCategory(), referenceIdToKeyCache))
                  .state(getResourceIdentifierWithKey(product.getState(), referenceIdToKeyCache))
                  .build();
            })
        .collect(Collectors.toList());
  }

  /**
   * Given a {@link ProductProjection} this method creates a {@link ProductDraftBuilder} based on
   * the staged projection values of the supplied product.
   *
   * @param product the product to create a {@link ProductDraftBuilder} based on it's staged data.
   * @return a {@link ProductDraftBuilder} based on the staged projection values of the supplied
   *     product.
   */
  @Nonnull
  public static ProductDraftBuilder getDraftBuilderFromStagedProduct(
      @Nonnull final ProductProjection product) {
    final List<ProductVariantDraft> allVariants =
        product.getAllVariants().stream()
            .map(
                productVariant ->
                    ProductVariantDraftBuilder.of(toVariantDraft(productVariant)).build())
            .collect(toList());
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of(toVariantDraft(product.getMasterVariant())).build();

    return ProductDraftBuilder.of()
        .productType(
            productTypeResourceIdentifierBuilder ->
                productTypeResourceIdentifierBuilder.id(product.getProductType().getId()))
        .name(product.getName())
        .slug(product.getSlug())
        .variants(allVariants)
        .masterVariant(masterVariant)
        .metaDescription(product.getMetaDescription())
        .metaKeywords(product.getMetaKeywords())
        .metaTitle(product.getMetaTitle())
        .description(product.getDescription())
        .searchKeywords(product.getSearchKeywords())
        .taxCategory(
            taxCategoryResourceIdentifierBuilder ->
                Optional.ofNullable(product.getTaxCategory())
                    .map(
                        taxCategory -> taxCategoryResourceIdentifierBuilder.id(taxCategory.getId()))
                    .orElse(null))
        .state(
            stateResourceIdentifierBuilder ->
                Optional.ofNullable(product.getState())
                    .map(state -> stateResourceIdentifierBuilder.id(state.getId()))
                    .orElse(null))
        .key(product.getKey())
        .publish(product.getPublished())
        .categories(
            product.getCategories().stream()
                .map(
                    categoryReference ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryReference.getId())
                            .build())
                .collect(toList()))
        .categoryOrderHints(product.getCategoryOrderHints());
  }

  @Nonnull
  static CategoryReferencePair mapToCategoryReferencePair(
      @Nonnull final ProductProjection product,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    final List<CategoryReference> categoryReferences = product.getCategories();
    final List<CategoryResourceIdentifier> categoryResourceIdentifiers = new ArrayList<>();

    final CategoryOrderHints categoryOrderHints = product.getCategoryOrderHints();
    final Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();

    categoryReferences.forEach(
        categoryReference -> {
          if (categoryReference != null) {
            final String categoryId = categoryReference.getId();
            if (referenceIdToKeyCache.containsKey(categoryId)) {
              final String categoryKey = referenceIdToKeyCache.get(categoryId);

              if (categoryOrderHints != null) {
                final String categoryOrderHintValue = categoryOrderHints.values().get(categoryId);
                if (categoryOrderHintValue != null) {
                  categoryOrderHintsMapWithKeys.put(categoryKey, categoryOrderHintValue);
                }
              }
              categoryResourceIdentifiers.add(
                  CategoryResourceIdentifierBuilder.of().key(categoryKey).build());
            } else {
              categoryResourceIdentifiers.add(
                  CategoryResourceIdentifierBuilder.of().id(categoryReference.getId()).build());
            }
          }
        });

    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryOrderHintsMapWithKeys.isEmpty()
            ? categoryOrderHints
            : CategoryOrderHintsBuilder.of().values(categoryOrderHintsMapWithKeys).build();
    return CategoryReferencePair.of(categoryResourceIdentifiers, categoryOrderHintsWithKeys);
  }

  private ProductReferenceResolutionUtils() {}
}
