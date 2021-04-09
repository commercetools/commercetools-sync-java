package com.commercetools.sync.products.utils;

import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.helpers.CategoryReferencePair;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProductReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void setup() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void mapToProductDrafts_WithNonExpandedReferences_ShouldUseCacheAndReplaceReferences() {

    final List<ProductProjection> products =
        asList(
            readObjectFromResource("product-with-unresolved-references.json", Product.class)
                .toProjection(STAGED));

    referenceIdToKeyCache.add("cda0dbf7-b42e-40bf-8453-241d5b587f93", "productTypeKey");
    referenceIdToKeyCache.add("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "categoryKey1");
    referenceIdToKeyCache.add("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "categoryKey2");
    referenceIdToKeyCache.add("cdcf8bea-48f2-54bc-b3c2-cdc94bf94f2c", "channelKey");
    referenceIdToKeyCache.add("d1229e6f-2b79-441e-b419-180311e52754", "customerGroupKey");
    referenceIdToKeyCache.add("ebbe95fb-2282-4f9a-8747-fbe440e02dc0", "taxCategoryKey");
    referenceIdToKeyCache.add("ste95fb-2282-4f9a-8747-fbe440e02dcs0", "stateKey");
    referenceIdToKeyCache.add("custom_type_id", "typeKey");

    final List<ProductDraft> productDraftsWithKeysOnReferences =
        ProductReferenceResolutionUtils.mapToProductDrafts(products, referenceIdToKeyCache);

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
  void
      mapToCategoryReferencePair_WithReferencesNotInCache_ShouldReturnReferencesWithoutReplacedKeys() {
    final String categoryId = UUID.randomUUID().toString();
    final Set<Reference<Category>> categoryReferences =
        singleton(Category.referenceOfId(categoryId));
    final CategoryOrderHints categoryOrderHints = getCategoryOrderHintsMock(categoryReferences);

    final ProductProjection product = getProductMock(categoryReferences, categoryOrderHints);

    final CategoryReferencePair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();

    assertThat(categoryReferencesWithKeys)
        .extracting(ResourceIdentifier::getId)
        .containsExactlyInAnyOrder(categoryId);
    assertThat(categoryOrderHintsWithKeys).isEqualTo(product.getCategoryOrderHints());
  }

  @Test
  void
      mapToCategoryReferencePair_WithReferencesNotInCacheAndNoCategoryOrderHints_ShouldNotReplaceIds() {
    final String categoryId = UUID.randomUUID().toString();
    final Set<Reference<Category>> categoryReferences =
        singleton(Category.referenceOfId(categoryId));
    final ProductProjection product = getProductMock(categoryReferences, null);

    final CategoryReferencePair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();

    assertThat(categoryReferencesWithKeys)
        .extracting(ResourceIdentifier::getId)
        .containsExactlyInAnyOrder(categoryId);
    assertThat(categoryOrderHintsWithKeys).isEqualTo(product.getCategoryOrderHints());
  }

  @Test
  void mapToCategoryReferencePair_WithNoReferences_ShouldNotReplaceIds() {
    final ProductProjection product = getProductMock(Collections.emptySet(), null);

    final CategoryReferencePair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();
    assertThat(categoryReferencesWithKeys).isEmpty();
    assertThat(categoryOrderHintsWithKeys).isNull();
  }

  @Test
  void mapToCategoryReferencePair_WithNullReferences_ShouldNotReplaceIds() {
    final ProductProjection product = getProductMock(singleton(null), null);

    final CategoryReferencePair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final Set<ResourceIdentifier<Category>> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();
    assertThat(categoryReferencesWithKeys).isEmpty();
    assertThat(categoryOrderHintsWithKeys).isNull();
  }

  @Nonnull
  private static ProductProjection getProductMock(
      @Nonnull final Set<Reference<Category>> references,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    final ProductProjection product = mock(ProductProjection.class);
    mockProductCategories(references, categoryOrderHints, product);
    return product;
  }

  private static void mockProductCategories(
      @Nonnull final Set<Reference<Category>> references,
      @Nullable final CategoryOrderHints categoryOrderHints,
      @Nonnull final ProductProjection product) {
    when(product.getCategories()).thenReturn(references);
    when(product.getCategoryOrderHints()).thenReturn(categoryOrderHints);
  }

  @Nonnull
  private static CategoryOrderHints getCategoryOrderHintsMock(
      @Nonnull final Set<Reference<Category>> references) {
    final Map<String, String> categoryOrderHintMap = new HashMap<>();
    references.forEach(
        categoryReference -> categoryOrderHintMap.put(categoryReference.getId(), "0.1"));
    return CategoryOrderHints.of(categoryOrderHintMap);
  }
}
