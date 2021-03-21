package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildSetMetaKeywordsUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildSetMetaKeywordsUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newKeywords = LocalizedString.of(Locale.GERMAN, "newKeywords");
    final UpdateAction<Product> setMetaKeywordsUpdateAction =
        getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newKeywords).orElse(null);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
    assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords())
        .isEqualTo(newKeywords);
  }

  @Test
  void buildSetMetaKeywordsUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> setMetaKeywordsUpdateAction =
        getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setMetaKeywordsUpdateAction).isNotNull();
    assertThat(setMetaKeywordsUpdateAction).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getSetMetaKeywordsUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nullable final LocalizedString newProductMetaKeywords) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getMetaKeywords()).thenReturn(newProductMetaKeywords);
    return buildSetMetaKeywordsUpdateAction(oldProduct, newProductDraft);
  }
}
