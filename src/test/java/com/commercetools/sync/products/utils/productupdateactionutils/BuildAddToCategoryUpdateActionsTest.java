package com.commercetools.sync.products.utils.productupdateactionutils;

import static com.commercetools.sync.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
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
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildAddToCategoryUpdateActionsTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      readObjectFromResource(PRODUCT_KEY_1_RESOURCE_PATH, Product.class)
          .toProjection(ProductProjectionType.STAGED);

  @Test
  void buildAddToCategoryUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
    final Set<ResourceIdentifier<Category>> newProductCategories = new HashSet<>();
    newProductCategories.add(category.toResourceIdentifier());

    final List<UpdateAction<Product>> addToCategoryUpdateAction =
        getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

    assertThat(addToCategoryUpdateAction).hasSize(1);
    assertThat(addToCategoryUpdateAction.get(0).getAction()).isEqualTo("addToCategory");
    assertThat(((AddToCategory) addToCategoryUpdateAction.get(0)).getCategory())
        .isEqualTo(category.toReference());
  }

  @Test
  void buildAddToCategoryUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Set<ResourceIdentifier<Category>> newProductCategories = new HashSet<>();
    MOCK_OLD_PUBLISHED_PRODUCT.getCategories().stream()
        .forEach(
            categoryReference -> {
              newProductCategories.add(
                  ResourceIdentifier.ofId(
                      categoryReference.getId(), categoryReference.getTypeId()));
            });

    final List<UpdateAction<Product>> addToCategoryUpdateAction =
        getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

    assertThat(addToCategoryUpdateAction).isEmpty();
  }

  private List<UpdateAction<Product>> getAddToCategoryUpdateActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final Set<ResourceIdentifier<Category>> newProductCategories) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getCategories()).thenReturn(newProductCategories);
    return buildAddToCategoryUpdateActions(oldProduct, newProductDraft);
  }
}
