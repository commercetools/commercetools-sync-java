package com.commercetools.sync.products.utils.productupdateactionutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.product.ProductChangeNameAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildChangeNameUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      ProductSyncMockUtils.createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildChangeNameUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");
    final ProductUpdateAction changeNameUpdateAction =
        getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName).orElse(null);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
    assertThat(((ProductChangeNameAction) changeNameUpdateAction).getName()).isEqualTo(newName);
  }

  @Test
  void buildChangeNameUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "english name");
    final Optional<ProductUpdateAction> changeNameUpdateAction =
        getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName);

    assertThat(changeNameUpdateAction).isNotNull();
    assertThat(changeNameUpdateAction).isNotPresent();
  }

  private Optional<ProductUpdateAction> getChangeNameUpdateAction(
      @Nonnull final ProductProjection oldProduct, @Nonnull final LocalizedString newProductName) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getName()).thenReturn(newProductName);
    return ProductUpdateActionUtils.buildChangeNameUpdateAction(oldProduct, newProductDraft);
  }
}
