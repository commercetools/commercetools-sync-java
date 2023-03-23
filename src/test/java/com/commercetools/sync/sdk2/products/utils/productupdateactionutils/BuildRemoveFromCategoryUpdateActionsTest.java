package com.commercetools.sync.sdk2.products.utils.productupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.utils.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildRemoveFromCategoryUpdateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildRemoveFromCategoryUpdateActionsTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      createProductFromJson(PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildRemoveFromCategoryUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final List<ProductUpdateAction> removeFromCategoryUpdateActions =
        getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptyList());

    assertThat(removeFromCategoryUpdateActions).hasSize(4);
    removeFromCategoryUpdateActions.forEach(
        updateAction -> assertThat(updateAction.getAction()).isEqualTo("removeFromCategory"));
  }

  @Test
  void buildRemoveFromCategoryUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final List<CategoryResourceIdentifier> newProductCategories = new ArrayList<>();
    MOCK_OLD_PUBLISHED_PRODUCT.getCategories().stream()
        .forEach(
            categoryReference -> {
              newProductCategories.add(
                  CategoryResourceIdentifierBuilder.of().id(categoryReference.getId()).build());
            });

    final List<ProductUpdateAction> removeFromCategoryUpdateActions =
        getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

    assertThat(removeFromCategoryUpdateActions).isEmpty();
  }

  private List<ProductUpdateAction> getRemoveFromCategoryUpdateActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final List<CategoryResourceIdentifier> newProductCategories) {

    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getCategories()).thenReturn(newProductCategories);
    return buildRemoveFromCategoryUpdateActions(oldProduct, newProductDraft);
  }
}
