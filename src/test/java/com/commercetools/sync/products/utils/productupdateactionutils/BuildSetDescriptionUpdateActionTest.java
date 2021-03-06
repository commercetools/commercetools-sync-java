package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetDescriptionUpdateAction;
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
import io.sphere.sdk.products.commands.updateactions.SetDescription;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildSetDescriptionUpdateActionTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildSetDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
    final UpdateAction<Product> setDescriptionUpdateAction =
        getSetDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription).orElse(null);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction.getAction()).isEqualTo("setDescription");
    assertThat(((SetDescription) setDescriptionUpdateAction).getDescription())
        .isEqualTo(newDescription);
  }

  @Test
  void buildSetDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final LocalizedString newDescription =
        LocalizedString.of(Locale.ENGLISH, "english description.");
    final Optional<UpdateAction<Product>> setDescriptionUpdateAction =
        getSetDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription);

    assertThat(setDescriptionUpdateAction).isNotNull();
    assertThat(setDescriptionUpdateAction).isNotPresent();
  }

  private Optional<UpdateAction<Product>> getSetDescriptionUpdateAction(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final LocalizedString newProductDescription) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getDescription()).thenReturn(newProductDescription);
    return buildSetDescriptionUpdateAction(oldProduct, newProductDraft);
  }
}
