package com.commercetools.sync.products.utils;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.queries.ProductQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class ProductReferenceResolutionUtilsTest {

  @Test
  void mapToProductDrafts_WithNonExpandedReferences_ShouldUseCacheAndReplaceReferences() {

    final List<Product> products =
        asList(readObjectFromResource("product-with-unresolved-references.json", Product.class));

    Map<String, String> idToKeyMap = new HashMap<>();
    idToKeyMap.put("cda0dbf7-b42e-40bf-8453-241d5b587f93", "productTypeKey");
    idToKeyMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "categoryKey1");
    idToKeyMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "categoryKey2");
    idToKeyMap.put("cdcf8bea-48f2-54bc-b3c2-cdc94bf94f2c", "channelKey");
    idToKeyMap.put("d1229e6f-2b79-441e-b419-180311e52754", "customerGroupKey");
    idToKeyMap.put("ebbe95fb-2282-4f9a-8747-fbe440e02dc0", "taxCategoryKey");
    idToKeyMap.put("ste95fb-2282-4f9a-8747-fbe440e02dcs0", "stateKey");
    idToKeyMap.put("custom_type_id", "typeKey");

    final List<ProductDraft> productDraftsWithKeysOnReferences =
        ProductReferenceResolutionUtils.mapToProductDrafts(products, idToKeyMap);

    // assertions

    final Optional<ProductDraft> productKey1 =
        productDraftsWithKeysOnReferences.stream()
            .filter(productDraft -> "productKeyResolved".equals(productDraft.getKey()))
            .findFirst();

    assertThat(productKey1)
        .hasValueSatisfying(
            productDraft ->
                assertThat(productDraft.getMasterVariant().getPrices())
                    .anySatisfy(
                        priceDraft -> {
                          assertThat(priceDraft.getChannel().getKey()).isEqualTo("channelKey");
                          assertThat(priceDraft.getCustomerGroup().getKey())
                              .isEqualTo("customerGroupKey");
                          assertThat(priceDraft.getCustom().getType().getKey())
                              .isEqualTo("typeKey");
                        }));

    assertThat(productKey1)
        .hasValueSatisfying(
            productDraft -> {
              assertThat(productDraft.getProductType().getKey()).isEqualTo("productTypeKey");
              assertThat(productDraft.getState().getKey()).isEqualTo("stateKey");
              assertThat(productDraft.getTaxCategory().getKey()).isEqualTo("taxCategoryKey");
              assertThat(productDraft.getCategories())
                  .anySatisfy(
                      categoryDraft -> {
                        assertThat(categoryDraft.getKey()).isEqualTo("categoryKey1");
                      });
            });
  }

  @Test
  void buildProductQuery_Always_ShouldReturnQueryWithNoneReferencesExpanded() {
    final ProductQuery productQuery = ProductReferenceResolutionUtils.buildProductQuery();
    assertThat(productQuery.expansionPaths()).isEmpty();
  }

  @Test
  void
      mapToCategoryReferencePair_WithReferencesNotInCache_ShouldReturnReferencesWithoutReplacedKeys() {
    final String categoryId = UUID.randomUUID().toString();
    final Set<Reference<Category>> categoryReferences =
        singleton(Category.referenceOfId(categoryId));
    final CategoryOrderHints categoryOrderHints = getCategoryOrderHintsMock(categoryReferences);

    final Product product = getProductMock(categoryReferences, categoryOrderHints);

    Map<String, String> idToKeyMap = new HashMap<>();
    final CategoryReferencePair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, idToKeyMap);

    assertThat(categoryReferencePair).isNotNull();

    final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();

    assertThat(categoryReferencesWithKeys)
        .extracting(ResourceIdentifier::getId)
        .containsExactlyInAnyOrder(categoryId);
    assertThat(categoryOrderHintsWithKeys)
        .isEqualTo(product.getMasterData().getStaged().getCategoryOrderHints());
  }

  @Test
  void
      mapToCategoryReferencePair_WithReferencesNotInCacheAndNoCategoryOrderHints_ShouldNotReplaceIds() {
    final String categoryId = UUID.randomUUID().toString();
    final Set<Reference<Category>> categoryReferences =
        singleton(Category.referenceOfId(categoryId));
    final Product product = getProductMock(categoryReferences, null);

    Map<String, String> idToKeyMap = new HashMap<>();
    final CategoryReferencePair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, idToKeyMap);

    assertThat(categoryReferencePair).isNotNull();

    final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();

    assertThat(categoryReferencesWithKeys)
        .extracting(ResourceIdentifier::getId)
        .containsExactlyInAnyOrder(categoryId);
    assertThat(categoryOrderHintsWithKeys)
        .isEqualTo(product.getMasterData().getStaged().getCategoryOrderHints());
  }

  @Nonnull
  private static Product getProductMock(
      @Nonnull final Set<Reference<Category>> references,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    final ProductData productData = mock(ProductData.class);
    mockProductDataCategories(references, categoryOrderHints, productData);
    return mockStagedProductData(productData);
  }

  private static void mockProductDataCategories(
      @Nonnull final Set<Reference<Category>> references,
      @Nullable final CategoryOrderHints categoryOrderHints,
      @Nonnull final ProductData productData) {
    when(productData.getCategories()).thenReturn(references);
    when(productData.getCategoryOrderHints()).thenReturn(categoryOrderHints);
  }

  @Nonnull
  private static CategoryOrderHints getCategoryOrderHintsMock(
      @Nonnull final Set<Reference<Category>> references) {
    final Map<String, String> categoryOrderHintMap = new HashMap<>();
    references.forEach(
        categoryReference -> categoryOrderHintMap.put(categoryReference.getId(), "0.1"));
    return CategoryOrderHints.of(categoryOrderHintMap);
  }

  @Nonnull
  private static Product mockStagedProductData(@Nonnull final ProductData productData) {
    final ProductCatalogData productCatalogData = mock(ProductCatalogData.class);
    when(productCatalogData.getStaged()).thenReturn(productData);

    final Product product = mock(Product.class);
    when(product.getMasterData()).thenReturn(productCatalogData);
    return product;
  }
}
