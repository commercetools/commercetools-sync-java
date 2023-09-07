package com.commercetools.sync.products.utils.productupdateactionutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetCategoryOrderHintAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.utils.ProductUpdateActionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class BuildSetCategoryOrderHintUpdateActionsTest {
  private static final ProductProjection MOCK_OLD_PUBLISHED_PRODUCT =
      ProductSyncMockUtils.createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH);

  @Test
  void buildSetCategoryOrderHintUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
    final CategoryOrderHints newProductCategoryOrderHints =
        CategoryOrderHintsBuilder.of()
            .addValue("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.33")
            .build();

    final List<ProductUpdateAction> setCategoryOrderHintUpdateActions =
        getSetCategoryOrderHintUpdateActions(
            MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints);

    assertThat(setCategoryOrderHintUpdateActions).hasSize(1);
    assertThat(setCategoryOrderHintUpdateActions.get(0).getAction())
        .isEqualTo("setCategoryOrderHint");
    assertThat(
            ((ProductSetCategoryOrderHintAction) setCategoryOrderHintUpdateActions.get(0))
                .getOrderHint())
        .isEqualTo("0.33");
  }

  @Test
  void buildSetCategoryOrderHintUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
    final Map<String, String> categoryOrderHintsMap = new HashMap<>();
    categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
    categoryOrderHintsMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.53");
    categoryOrderHintsMap.put("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.63");
    categoryOrderHintsMap.put("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.73");
    final CategoryOrderHints newProductCategoryOrderHints =
        CategoryOrderHintsBuilder.of().values(categoryOrderHintsMap).build();

    final List<ProductUpdateAction> setCategoryOrderHintUpdateActions =
        getSetCategoryOrderHintUpdateActions(
            MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints);

    assertThat(setCategoryOrderHintUpdateActions).isEmpty();
  }

  private List<ProductUpdateAction> getSetCategoryOrderHintUpdateActions(
      @Nonnull final ProductProjection oldProduct,
      @Nonnull final CategoryOrderHints newProductCategoryOrderHints) {
    final ProductDraft newProductDraft = mock(ProductDraft.class);
    when(newProductDraft.getCategoryOrderHints()).thenReturn(newProductCategoryOrderHints);
    return ProductUpdateActionUtils.buildSetCategoryOrderHintUpdateActions(
        oldProduct, newProductDraft);
  }
}
