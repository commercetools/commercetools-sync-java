package com.commercetools.sync.products.utils;

import static com.commercetools.sync.commons.utils.SyncUtils.getResourceIdentifierWithKey;
import static java.util.stream.Collectors.toList;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.customergroups.CustomerGroup;
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
import io.sphere.sdk.products.attributes.Attribute;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryExecutionUtils;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Util class which provides utilities that can be used when syncing resources from a source
 * commercetools project to a target one.
 */
public final class ProductReferenceResolutionUtils {

  /**
   * Returns an {@link List}&lt;{@link ProductDraft}&gt; consisting of the results of applying the
   * mapping from {@link Product} to {@link ProductDraft} with considering reference resolution.
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
   *       <td>{@link Reference}&lt;{@link ProductType}&gt;</td>
   *       <td>{@link ResourceIdentifier}&lt;{@link ProductType}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>categories</td>
   *        <td>{@link Set}&lt;{@link Reference}&lt;{@link Category}&gt;&gt;</td>
   *        <td>{@link Set}&lt;{@link ResourceIdentifier}&lt;{@link Category}&gt;&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.channel</td>
   *        <td>{@link Reference}&lt;{@link Channel}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Channel}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.customerGroup *</td>
   *        <td>{@link Reference}&lt;{@link CustomerGroup}&gt;</td>
   *        <td>{@link Reference}&lt;{@link CustomerGroup}&gt; (with key replaced with id field)</td>
   *     </tr>
   *     <tr>
   *        <td>variants.prices.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.assets.custom.type</td>
   *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>variants.attributes on {@link List}&lt;{@link Attribute} *</td>
   *        <td>{@link Reference}&lt;{@link ProductType}&gt; (example for ProductType)</td>
   *        <td>{@link Reference}&lt;{@link ProductType}&gt; (with key replaced with id field)</td>
   *     </tr>
   *     <tr>
   *        <td>taxCategory</td>
   *        <td>{@link Reference}&lt;{@link TaxCategory}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link TaxCategory}&gt;</td>
   *     </tr>
   *     <tr>
   *        <td>state *</td>
   *        <td>{@link Reference}&lt;{@link State}&gt;</td>
   *        <td>{@link ResourceIdentifier}&lt;{@link State}&gt;</td>
   *     </tr>
   *   </tbody>
   * </table>
   *
   * <p><b>Note:</b> The aforementioned references should be expanded with a key. Any reference that
   * is not expanded will have its id in place and not replaced by the key will be considered as
   * existing resources on the target commercetools project and the library will issues an
   * update/create API request without reference resolution.
   *
   * @param products the products with expanded references.
   * @return a {@link List} of {@link ProductDraft} built from the supplied {@link List} of {@link
   *     Product}.
   */
  @Nonnull
  public static List<ProductDraft> mapToProductDrafts(@Nonnull final List<Product> products) {
    return products.stream()
        .filter(Objects::nonNull)
        .map(
            product -> {
              final ProductDraft productDraft = getDraftBuilderFromStagedProduct(product).build();

              final CategoryReferencePair categoryReferencePair =
                  mapToCategoryReferencePair(product);
              final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers =
                  categoryReferencePair.getCategoryResourceIdentifiers();
              final CategoryOrderHints categoryOrderHintsWithKeys =
                  categoryReferencePair.getCategoryOrderHints();

              final List<ProductVariant> allVariants =
                  product.getMasterData().getStaged().getAllVariants();
              final List<ProductVariantDraft> variantDraftsWithKeys =
                  VariantReferenceResolutionUtils.mapToProductVariantDrafts(allVariants);
              final ProductVariantDraft masterVariantDraftWithKeys =
                  variantDraftsWithKeys.remove(0);

              return ProductDraftBuilder.of(productDraft)
                  .masterVariant(masterVariantDraftWithKeys)
                  .variants(variantDraftsWithKeys)
                  .productType(getResourceIdentifierWithKey(product.getProductType()))
                  .categories(categoryResourceIdentifiers)
                  .categoryOrderHints(categoryOrderHintsWithKeys)
                  .taxCategory(getResourceIdentifierWithKey(product.getTaxCategory()))
                  .state(getResourceIdentifierWithKey(product.getState()))
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
      @Nonnull final Product product) {
    final ProductData productData = product.getMasterData().getStaged();
    final List<ProductVariantDraft> allVariants =
        productData.getAllVariants().stream()
            .map(productVariant -> ProductVariantDraftBuilder.of(productVariant).build())
            .collect(toList());
    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of(product.getMasterData().getStaged().getMasterVariant())
            .build();

    return ProductDraftBuilder.of(
            product.getProductType(), productData.getName(), productData.getSlug(), allVariants)
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

  @Nonnull
  static CategoryReferencePair mapToCategoryReferencePair(@Nonnull final Product product) {
    final Set<Reference<Category>> categoryReferences =
        product.getMasterData().getStaged().getCategories();
    final Set<ResourceIdentifier<Category>> categoryResourceIdentifiers = new HashSet<>();

    final CategoryOrderHints categoryOrderHints =
        product.getMasterData().getStaged().getCategoryOrderHints();
    final Map<String, String> categoryOrderHintsMapWithKeys = new HashMap<>();

    categoryReferences.forEach(
        categoryReference -> {
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

    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryOrderHintsMapWithKeys.isEmpty()
            ? categoryOrderHints
            : CategoryOrderHints.of(categoryOrderHintsMapWithKeys);
    return CategoryReferencePair.of(categoryResourceIdentifiers, categoryOrderHintsWithKeys);
  }

  /**
   * Builds a {@link ProductQuery} for fetching products from a source CTP project with all the
   * needed references expanded for the sync:
   *
   * <ul>
   *   <li>Product Type
   *   <li>Tax Category
   *   <li>Product State
   *   <li>Staged Assets' Custom Types
   *   <li>Staged Product Categories
   *   <li>Staged Prices' Channels
   *   <li>Staged Prices' Customer Groups
   *   <li>Staged Prices' Custom Types
   *   <li>Reference Attributes
   *   <li>Reference Set Attributes
   * </ul>
   *
   * <p>Note: Please only use this util if you desire to sync all the aforementioned references from
   * a source commercetools project. Otherwise, it is more efficient to build the query without
   * expansions, if they are not needed, to avoid unnecessarily bigger payloads fetched from the
   * source project.
   *
   * @return the query for fetching products from the source CTP project with all the aforementioned
   *     references expanded.
   */
  @Nonnull
  public static ProductQuery buildProductQuery() {
    return ProductQuery.of()
        .withLimit(QueryExecutionUtils.DEFAULT_PAGE_SIZE)
        .withExpansionPaths(ProductExpansionModel::productType)
        .plusExpansionPaths(ProductExpansionModel::taxCategory)
        .plusExpansionPaths(ExpansionPath.of("state"))
        .plusExpansionPaths(expansionModel -> expansionModel.masterData().staged().categories())
        .plusExpansionPaths(
            expansionModel -> expansionModel.masterData().staged().allVariants().prices().channel())
        .plusExpansionPaths(
            expansionModel ->
                expansionModel.masterData().staged().allVariants().prices().customerGroup())
        .plusExpansionPaths(
            ExpansionPath.of("masterData.staged.masterVariant.prices[*].custom.type"))
        .plusExpansionPaths(ExpansionPath.of("masterData.staged.variants[*].prices[*].custom.type"))
        .plusExpansionPaths(
            expansionModel ->
                expansionModel.masterData().staged().allVariants().attributes().value())
        .plusExpansionPaths(
            expansionModel ->
                expansionModel.masterData().staged().allVariants().attributes().valueSet())
        .plusExpansionPaths(
            ExpansionPath.of("masterData.staged.masterVariant.assets[*].custom.type"))
        .plusExpansionPaths(
            ExpansionPath.of("masterData.staged.variants[*].assets[*].custom.type"));
  }

  private ProductReferenceResolutionUtils() {}
}
