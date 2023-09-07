package com.commercetools.sync.products.utils.productupdateactionutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetMetaDescriptionAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildSetMetaDescriptionUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      ProductSyncMockUtils.createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildSetMetaDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
    final ProductUpdateAction setMetaDescriptionUpdateAction =
        getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription).orElse(null);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
    assertThat(
            ((ProductSetMetaDescriptionAction) setMetaDescriptionUpdateAction).getMetaDescription())
        .isEqualTo(newDescription);
  }

  @Test
  void buildSetMetaDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Optional<ProductUpdateAction> setMetaDescriptionUpdateAction =
        getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getSetMetaDescriptionUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nullable final LocalizedString newProductMetaDescription) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getMetaDescription()).thenReturn(newProductMetaDescription);
    return ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction(
        oldProduct, newProductDraft);
  }
}
