package com.commercetools.sync.sdk2.products.utils.productupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildChangeSlugUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductChangeSlugAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildChangeSlugUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      createProductFromJson(PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildChangeSlugUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newSlug = LocalizedString.of(Locale.GERMAN, "newSlug");
    final ProductUpdateAction changeSlugUpdateAction =
        getChangeSlugUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newSlug).orElse(null);

    assertThat(changeSlugUpdateAction).isNotNull();
    assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
    assertThat(((ProductChangeSlugAction) changeSlugUpdateAction).getSlug()).isEqualTo(newSlug);
  }

  @Test
  void buildChangeSlugUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString newSlug = LocalizedString.of(Locale.ENGLISH, "english-slug");
    final Optional<ProductUpdateAction> changeSlugUpdateAction =
        getChangeSlugUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newSlug);

    assertThat(changeSlugUpdateAction).isNotNull();
    assertThat(changeSlugUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getChangeSlugUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final LocalizedString newProductDescription) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getSlug()).thenReturn(newProductDescription);
    return buildChangeSlugUpdateAction(oldProduct, newProductDraft);
  }
}
