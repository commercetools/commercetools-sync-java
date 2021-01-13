package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaTitleUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetMetaTitle;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildSetMetaTitleUpdateActionTest {
  private static final Product MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class);

  @Test
  void buildSetMetaTitleUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newTitle = LocalizedString.of(Locale.GERMAN, "newTitle");
    final UpdateAction<Product> setMetaTitleUpdateAction =
        getSetMetaTitleUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newTitle).orElse(null);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction.getAction()).isEqualTo("setMetaTitle");
    assertThat(((SetMetaTitle) setMetaTitleUpdateAction).getMetaTitle()).isEqualTo(newTitle);
  }

  @Test
  void buildSetMetaTitleUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> setMetaTitleUpdateAction =
        getSetMetaTitleUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setMetaTitleUpdateAction).isNotNull();
    assertThat(setMetaTitleUpdateAction).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getSetMetaTitleUpdateAction(
      @Nonnull final Product oldProduct, @Nullable final LocalizedString newProductMetaTitle) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getMetaTitle()).thenReturn(newProductMetaTitle);
    return buildSetMetaTitleUpdateAction(oldProduct, newProductDraft);
  }
}
