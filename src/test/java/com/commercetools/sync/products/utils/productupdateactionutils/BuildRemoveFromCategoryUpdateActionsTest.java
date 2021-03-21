package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveFromCategoryUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildRemoveFromCategoryUpdateActionsTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildRemoveFromCategoryUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
        getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptySet());

    assertThat(removeFromCategoryUpdateActions).hasSize(4);
    removeFromCategoryUpdateActions.forEach(
        updateAction -> assertThat(updateAction.getAction()).isEqualTo("removeFromCategory"));
  }

  @Test
  void buildRemoveFromCategoryUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Set<ResourceIdentifier<Category>> newProductCategories = new HashSet<>();
    newProductCategories.add(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
    newProductCategories.add(Category.referenceOfId("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
    newProductCategories.add(Category.referenceOfId("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
    newProductCategories.add(Category.referenceOfId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

    final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
        getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

    assertThat(removeFromCategoryUpdateActions).isEmpty();
  }

  private List<UpdateAction<Product>> getRemoveFromCategoryUpdateActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final Set<ResourceIdentifier<Category>> newProductCategories) {

    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getCategories()).thenReturn(newProductCategories);
    return buildRemoveFromCategoryUpdateActions(oldProduct, newProductDraft);
  }
}
