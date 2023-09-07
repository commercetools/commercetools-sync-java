package com.commercetools.sync.products.utils.productupdateactionutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetMetaKeywordsAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildSetMetaKeywordsUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      ProductSyncMockUtils.createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildSetMetaKeywordsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newKeywords = LocalizedString.of(Locale.GERMAN, "newKeywords");
    final ProductUpdateAction setMetaKeywordsUpdateAction =
        getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newKeywords).orElse(null);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
    assertThat(((ProductSetMetaKeywordsAction) setMetaKeywordsUpdateAction).getMetaKeywords())
        .isEqualTo(newKeywords);
  }

  @Test
  void buildSetMetaKeywordsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> setMetaKeywordsUpdateAction =
        getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getSetMetaKeywordsUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nullable final LocalizedString newProductMetaKeywords) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getMetaKeywords()).thenReturn(newProductMetaKeywords);
    return ProductUpdateActionUtils.buildSetMetaKeywordsUpdateAction(oldProduct, newProductDraft);
  }
}
