package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static com.commercetools.sync.sdk2.products.utils.AssetUtils.createAssetDraft;
import static com.commercetools.sync.sdk2.products.utils.PriceUtils.createPriceDraft;
import static java.util.stream.Collectors.toList;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifier;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.helpers.CategoryResourceIdentifierPair;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class ProductReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link ProductDraft}&gt; consisting of the results of applying the
   * mapping from the staged version of a {@link ProductProjection} to {@link ProductDraft} with
   * considering reference resolution.
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
   *       <td>{@link ProductTypeReference}</td>
   *       <td>{@link ProductTypeResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>categories</td>
   *        <td>{@link List}&lt;{@link CategoryReference}&gt;</td>
   *        <td>{@link List}&lt;{@link CategoryResourceIdentifier}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.channel</td>
   *        <td>{@link com.commercetools.api.models.channel.ChannelReference}</td>
   *        <td>{@link com.commercetools.api.models.channel.ChannelResourceIdentifier}</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.customerGroup *</td>
   *        <td>{@link com.commercetools.api.models.customer_group.CustomerGroupReference}</td>
   *        <td>{@link com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier}</td>
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
   *        <td>variants.attributes on {@link List}&lt;{@link Attribute} *</td>
   *        <td>{@link ProductTypeReference} (example for ProductType)</td>
   *        <td>{@link ProductTypeReference} (with key replaced with id field)</td>
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
   * @return a {@link List} of {@link ProductDraft} built from the supplied {@link List} of {@link
   *     Product}.
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

              final CategoryResourceIdentifierPair categoryResourceIdentifierPair =
                  mapToCategoryReferencePair(product, referenceIdToKeyCache);
              final List<CategoryResourceIdentifier> categoryResourceIdentifiers =
                  categoryResourceIdentifierPair.getCategoryResourceIdentifiers();
              final CategoryOrderHints categoryOrderHintsWithKeys =
                  categoryResourceIdentifierPair.getCategoryOrderHints();

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
                      (ProductTypeResourceIdentifier)
                          getResourceIdentifierWithKey(
                              product.getProductType(), referenceIdToKeyCache))
                  .categories(categoryResourceIdentifiers)
                  .categoryOrderHints(categoryOrderHintsWithKeys)
                  .taxCategory(
                      (TaxCategoryResourceIdentifier)
                          getResourceIdentifierWithKey(
                              product.getTaxCategory(), referenceIdToKeyCache))
                  .state(
                      (StateResourceIdentifier)
                          getResourceIdentifierWithKey(product.getState(), referenceIdToKeyCache))
                  .build();
            })
        .collect(Collectors.toList());
  }

  /**
   * Given a {@link Product} this method creates a {@link ProductDraftBuilder} based on the staged
   * projection values of the supplied product.
   *
   * @param product the product to create a {@link ProductDraftBuilder} based on it's staged data.
   * @return a {@link ProductDraftBuilder} based on the staged projection values of the supplied
   *     product.
   */
  @Nonnull
  public static ProductDraftBuilder getDraftBuilderFromStagedProduct(
      @Nonnull final ProductProjection product) {
    final List<ProductVariantDraft> allVariants =
        product.getVariants().stream()
            .map(productVariant -> getProductVariantDraft(productVariant))
            .collect(toList());
    final ProductVariantDraft masterVariant = getProductVariantDraft(product.getMasterVariant());

    return ProductDraftBuilder.of()
        .productType(
            ProductTypeResourceIdentifierBuilder.of().id(product.getProductType().getId()).build())
        .name(product.getName())
        .slug(product.getSlug())
        .masterVariant(masterVariant)
        .variants(allVariants)
        .metaDescription(product.getMetaDescription())
        .metaKeywords(product.getMetaKeywords())
        .metaTitle(product.getMetaTitle())
        .description(product.getDescription())
        .searchKeywords(product.getSearchKeywords())
        .taxCategory(
            TaxCategoryResourceIdentifierBuilder.of().id(product.getTaxCategory().getId()).build())
        .key(product.getKey())
        .categories(
            product.getCategories().stream()
                .map(
                    categoryReference ->
                        CategoryResourceIdentifierBuilder.of()
                            .id(categoryReference.getId())
                            .build())
                .collect(toList()))
        .categoryOrderHints(product.getCategoryOrderHints())
        .publish(product.getPublished());
  }

  static ProductVariantDraft getProductVariantDraft(@Nonnull final ProductVariant productVariant) {
    List<AssetDraft> assetDrafts = createAssetDraft(productVariant.getAssets());
    List<PriceDraft> priceDrafts = createPriceDraft(productVariant.getPrices());
    List<Attribute> attributes = createAttributes(productVariant.getAttributes());
    return ProductVariantDraftBuilder.of()
        .assets(assetDrafts)
        .attributes(attributes)
        .images(productVariant.getImages())
        .prices(priceDrafts)
        .sku(productVariant.getSku())
        .key(productVariant.getKey())
        .build();
  }

  public static List<Attribute> createAttributes(List<Attribute> attributes) {
    return attributes.stream()
        .map(attribute -> AttributeBuilder.of(attribute).build())
        .collect(Collectors.toList());
  }

  @Nonnull
  static CategoryResourceIdentifierPair mapToCategoryReferencePair(
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
    return CategoryResourceIdentifierPair.of(
        categoryResourceIdentifiers, categoryOrderHintsWithKeys);
  }

  private ProductReferenceResolutionUtils() {}
}
