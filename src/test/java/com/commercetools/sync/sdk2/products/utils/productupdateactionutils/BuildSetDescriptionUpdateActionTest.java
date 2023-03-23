package com.commercetools.sync.sdk2.products.utils.productupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetDescriptionUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetDescriptionAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildSetDescriptionUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      createProductFromJson(PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildSetDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
    final ProductUpdateAction setDescriptionUpdateAction =
        getSetDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription).orElse(null);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
    assertThat(((ProductSetDescriptionAction) setDescriptionUpdateAction).getDescription())
        .isEqualTo(newDescription);
  }

  @Test
  void buildSetDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString newDescription =
        LocalizedString.of(Locale.ENGLISH, "english description.");
    final Optional<ProductUpdateAction> setDescriptionUpdateAction =
        getSetDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getSetDescriptionUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final LocalizedString newProductDescription) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getDescription()).thenReturn(newProductDescription);
    return buildSetDescriptionUpdateAction(oldProduct, newProductDraft);
  }
}
