package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
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
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class BuildSetMetaDescriptionUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildSetMetaDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
    final UpdateAction<Product> setMetaDescriptionUpdateAction =
        getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription).orElse(null);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
    assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
        .isEqualTo(newDescription);
  }

  @Test
  void buildSetMetaDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
        getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

    assertThat(setMetaDescriptionUpdateAction).isNotNull();
    assertThat(setMetaDescriptionUpdateAction).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getSetMetaDescriptionUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nullable final LocalizedString newProductMetaDescription) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getMetaDescription()).thenReturn(newProductMetaDescription);
    return buildSetMetaDescriptionUpdateAction(oldProduct, newProductDraft);
  }
}
