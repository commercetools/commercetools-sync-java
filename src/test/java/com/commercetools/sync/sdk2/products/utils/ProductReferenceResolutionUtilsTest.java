package com.commercetools.sync.sdk2.products.utils;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductFromJson;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.common.ResourceIdentifier;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.sync.sdk2.commons.helpers.CategoryResourceIdentifierPair;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        asList(createProductFromJson("product-with-unresolved-references.json"));

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
    final List<CategoryReference> categoryReferences =
        singletonList(CategoryReferenceBuilder.of().id(categoryId).build());
    final CategoryOrderHints categoryOrderHints = getCategoryOrderHintsMock(categoryReferences);

    final ProductProjection product = getProductMock(categoryReferences, categoryOrderHints);

    final CategoryResourceIdentifierPair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final List<CategoryResourceIdentifier> categoryReferencesWithKeys =
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
    final List<CategoryReference> categoryReferences =
        singletonList(CategoryReferenceBuilder.of().id(categoryId).build());
    final ProductProjection product = getProductMock(categoryReferences, null);

    final CategoryResourceIdentifierPair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final List<CategoryResourceIdentifier> categoryReferencesWithKeys =
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
    final ProductProjection product = getProductMock(Collections.emptyList(), null);

    final CategoryResourceIdentifierPair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final List<CategoryResourceIdentifier> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();
    assertThat(categoryReferencesWithKeys).isEmpty();
    assertThat(categoryOrderHintsWithKeys).isNull();
  }

  @Test
  void mapToCategoryReferencePair_WithNullReferences_ShouldNotReplaceIds() {
    final ProductProjection product = getProductMock(singletonList(null), null);

    final CategoryResourceIdentifierPair categoryReferencePair =
        ProductReferenceResolutionUtils.mapToCategoryReferencePair(product, referenceIdToKeyCache);

    assertThat(categoryReferencePair).isNotNull();

    final List<CategoryResourceIdentifier> categoryReferencesWithKeys =
        categoryReferencePair.getCategoryResourceIdentifiers();
    final CategoryOrderHints categoryOrderHintsWithKeys =
        categoryReferencePair.getCategoryOrderHints();
    assertThat(categoryReferencesWithKeys).isEmpty();
    assertThat(categoryOrderHintsWithKeys).isNull();
  }

  @Nonnull
  private static ProductProjection getProductMock(
      @Nonnull final List<CategoryReference> references,
      @Nullable final CategoryOrderHints categoryOrderHints) {
    final ProductProjection product = mock(ProductProjection.class);
    mockProductCategories(references, categoryOrderHints, product);
    return product;
  }

  private static void mockProductCategories(
      @Nonnull final List<CategoryReference> references,
      @Nullable final CategoryOrderHints categoryOrderHints,
      @Nonnull final ProductProjection product) {
    when(product.getCategories()).thenReturn(references);
    when(product.getCategoryOrderHints()).thenReturn(categoryOrderHints);
  }

  @Nonnull
  private static CategoryOrderHints getCategoryOrderHintsMock(
      @Nonnull final List<CategoryReference> references) {
    final Map<String, String> categoryOrderHintMap = new HashMap<>();
    references.forEach(
        categoryReference -> categoryOrderHintMap.put(categoryReference.getId(), "0.1"));
    return CategoryOrderHintsBuilder.of().values(categoryOrderHintMap).build();
  }
}
