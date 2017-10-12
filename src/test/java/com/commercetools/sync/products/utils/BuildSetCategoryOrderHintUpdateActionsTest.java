package com.commercetools.sync.products.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetCategoryOrderHintUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSetCategoryOrderHintUpdateActionsTest {
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_RESOURCE_PATH, Product.class);

    @Test
    public void buildSetCategoryOrderHintUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.33");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints);

        assertThat(setCategoryOrderHintUpdateActions).hasSize(1);
        assertThat(setCategoryOrderHintUpdateActions.get(0).getAction()).isEqualTo("setCategoryOrderHint");
        assertThat(((SetCategoryOrderHint) setCategoryOrderHintUpdateActions.get(0)).getOrderHint()).isEqualTo("0.33");
    }

    @Test
    public void buildSetCategoryOrderHintUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        categoryOrderHintsMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.53");
        categoryOrderHintsMap.put("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.63");
        categoryOrderHintsMap.put("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.73");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);


        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints);

        assertThat(setCategoryOrderHintUpdateActions).isEmpty();
    }

    private List<UpdateAction<Product>> getSetCategoryOrderHintUpdateActions(@Nonnull final Product oldProduct,
                                                                             @Nonnull final CategoryOrderHints
                                                                                 newProductCategoryOrderHints) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getCategoryOrderHints()).thenReturn(newProductCategoryOrderHints);
        return buildSetCategoryOrderHintUpdateActions(oldProduct, newProductDraft);
    }
}
