package com.commercetools.sync.sdk2.products.utils.productupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetMetaTitleAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildSetMetaTitleUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      createProductFromJson(PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildSetMetaTitleUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newTitle = LocalizedString.of(Locale.GERMAN, "newTitle");
    final ProductUpdateAction setMetaTitleUpdateAction =
        getSetMetaTitleUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newTitle).orElse(null);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
    assertThat(((ProductSetMetaTitleAction) setMetaTitleUpdateAction).getMetaTitle())
        .isEqualTo(newTitle);
  }

  @Test
  void buildSetMetaTitleUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> setMetaTitleUpdateAction =
        getSetMetaTitleUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getSetMetaTitleUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nullable final LocalizedString newProductMetaTitle) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getMetaTitle()).thenReturn(newProductMetaTitle);
    return buildSetMetaTitleUpdateAction(oldProduct, newProductDraft);
  }
}
