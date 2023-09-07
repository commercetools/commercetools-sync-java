package com.commercetools.sync.products.utils.productupdateactionutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetSearchKeywordsAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.SearchKeywordBuilder;
import com.commercetools.api.models.product.SearchKeywords;
import com.commercetools.api.models.product.SearchKeywordsBuilder;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildSetSearchKeywordsUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      ProductSyncMockUtils.createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildSetSearchKeywordsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final SearchKeywords newProductSearchKeywords =
        SearchKeywordsBuilder.of()
            .addValue(
                Locale.ENGLISH.getLanguage(),
                Arrays.asList(
                    SearchKeywordBuilder.of().text("searchKeyword1").build(),
                    SearchKeywordBuilder.of().text("searchKeyword2").build()))
            .build();
    final ProductUpdateAction setSearchKeywordsUpdateAction =
        getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords)
            .orElse(null);

    assertThat(setSearchKeywordsUpdateAction).isNotNull();
    assertThat(setSearchKeywordsUpdateAction.getAction()).isEqualTo("setSearchKeywords");
    assertThat(((ProductSetSearchKeywordsAction) setSearchKeywordsUpdateAction).getSearchKeywords())
        .isEqualTo(newProductSearchKeywords);
  }

  @Test
  void buildSetSearchKeywordsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final SearchKeywords newProductSearchKeywords = SearchKeywords.of();
    final Optional<ProductUpdateAction> setSearchKeywordsUpdateAction =
        getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords);

    assertThat(setSearchKeywordsUpdateAction).isNotNull();
    assertThat(setSearchKeywordsUpdateAction).isNotPresent();
  }

  @Test
  void
      buildSetSearchKeywordsUpdateAction_WithNewSearchKeywordsNull_ShouldReturnEmptyUpdateAction() {
    final Optional<ProductUpdateAction> setSearchKeywordsUpdateAction =
        getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setSearchKeywordsUpdateAction).isNotNull();
    assertThat(setSearchKeywordsUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getSetSearchKeywordsUpdateAction(
      @Nonnull final ProductProjection oldProduct, final SearchKeywords newProductSearchKeywords) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getSearchKeywords()).thenReturn(newProductSearchKeywords);
    return ProductUpdateActionUtils.buildSetSearchKeywordsUpdateAction(oldProduct, newProductDraft);
  }
}
