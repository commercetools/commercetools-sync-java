package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeSlugUpdateAction;
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
import io.sphere.sdk.products.commands.updateactions.ChangeSlug;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildChangeSlugUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildChangeSlugUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newSlug = LocalizedString.of(Locale.GERMAN, "newSlug");
    final UpdateAction<Product> changeSlugUpdateAction =
        getChangeSlugUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newSlug).orElse(null);

    assertThat(changeSlugUpdateAction).isNotNull();
    assertThat(changeSlugUpdateAction.getAction()).isEqualTo("changeSlug");
    assertThat(((ChangeSlug) changeSlugUpdateAction).getSlug()).isEqualTo(newSlug);
  }

  @Test
  void buildChangeSlugUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString newSlug = LocalizedString.of(Locale.ENGLISH, "english-slug");
    final Optional<UpdateAction<Product>> changeSlugUpdateAction =
        getChangeSlugUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newSlug);

    assertThat(changeSlugUpdateAction).isNotNull();
    assertThat(changeSlugUpdateAction).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getChangeSlugUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final LocalizedString newProductDescription) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getSlug()).thenReturn(newProductDescription);
    return buildChangeSlugUpdateAction(oldProduct, newProductDraft);
  }
}
