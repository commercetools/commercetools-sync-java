package com.commercetools.sync.sdk2.products.utils.productupdateactionutils;

import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.readObjectFromResource;
import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.category.CategoryResourceIdentifierBuilder;
import com.commercetools.api.models.product.ProductAddToCategoryAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildAddToCategoryUpdateActionsTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      createProductFromJson(PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildAddToCategoryUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
    final List<CategoryResourceIdentifier> newProductCategories = new ArrayList<>();
    newProductCategories.add(category.toResourceIdentifier());

    final List<ProductUpdateAction> addToCategoryUpdateAction =
        getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

    assertThat(addToCategoryUpdateAction).hasSize(1);
    assertThat(addToCategoryUpdateAction.get(0).getAction()).isEqualTo("addToCategory");
    assertThat(((ProductAddToCategoryAction) addToCategoryUpdateAction.get(0)).getCategory())
        .isEqualTo(newProductCategories.get(0));
  }

  @Test
  void buildAddToCategoryUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final List<CategoryResourceIdentifier> newProductCategories = new ArrayList<>();
    MOCK_OLD_PUBLISHED_PRODUCT.getCategories().stream()
        .forEach(
            categoryReference ->
                newProductCategories.add(
                    CategoryResourceIdentifierBuilder.of().id(categoryReference.getId()).build()));

    final List<ProductUpdateAction> addToCategoryUpdateAction =
        getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

    assertThat(addToCategoryUpdateAction).isEmpty();
  }

  private List<ProductUpdateAction> getAddToCategoryUpdateActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final List<CategoryResourceIdentifier> newProductCategories) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getCategories()).thenReturn(newProductCategories);
    return buildAddToCategoryUpdateActions(oldProduct, newProductDraft);
  }
}
