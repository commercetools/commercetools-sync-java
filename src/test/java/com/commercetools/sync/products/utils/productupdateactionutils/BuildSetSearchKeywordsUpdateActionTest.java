package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetSearchKeywordsUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.search.SearchKeyword;
import io.sphere.sdk.search.SearchKeywords;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildSetSearchKeywordsUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildSetSearchKeywordsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final SearchKeywords newProductSearchKeywords =
        SearchKeywords.of(
            Locale.ENGLISH,
            Arrays.asList(SearchKeyword.of("searchKeyword1"), SearchKeyword.of("searchKeyword2")));
    final UpdateAction<Product> setSearchKeywordsUpdateAction =
        getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords)
            .orElse(null);

    assertThat(setSearchKeywordsUpdateAction).isNotNull();
    assertThat(setSearchKeywordsUpdateAction.getAction()).isEqualTo("setSearchKeywords");
    assertThat(((SetSearchKeywords) setSearchKeywordsUpdateAction).getSearchKeywords())
        .isEqualTo(newProductSearchKeywords);
  }

  @Test
  void buildSetSearchKeywordsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final SearchKeywords newProductSearchKeywords = SearchKeywords.of();
    final Optional<UpdateAction<Product>> setSearchKeywordsUpdateAction =
        getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords);

    assertThat(setSearchKeywordsUpdateAction).isNotNull();
    assertThat(setSearchKeywordsUpdateAction).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getSetSearchKeywordsUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final SearchKeywords newProductSearchKeywords) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getSearchKeywords()).thenReturn(newProductSearchKeywords);
    return buildSetSearchKeywordsUpdateAction(oldProduct, newProductDraft);
  }
}
