package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
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

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetCategoryOrderHintUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSetCategoryOrderHintUpdateActionsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.33");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints, true);

        assertThat(setCategoryOrderHintUpdateActions).hasSize(1);
        assertThat(setCategoryOrderHintUpdateActions.get(0).getAction()).isEqualTo("setCategoryOrderHint");
        assertThat(((SetCategoryOrderHint) setCategoryOrderHintUpdateActions.get(0)).getOrderHint()).isEqualTo("0.33");
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        categoryOrderHintsMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.53");
        categoryOrderHintsMap.put("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.63");
        categoryOrderHintsMap.put("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.73");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);


        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints, true);

        assertThat(setCategoryOrderHintUpdateActions).isEmpty();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        categoryOrderHintsMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.53");
        categoryOrderHintsMap.put("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.63");
        categoryOrderHintsMap.put("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.73");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints, false);

        assertThat(setCategoryOrderHintUpdateActions).hasSize(3);
        setCategoryOrderHintUpdateActions.forEach(productUpdateAction ->
            assertThat(productUpdateAction.getAction()).isEqualTo("setCategoryOrderHint"));
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategoryOrderHints, false);

        assertThat(setCategoryOrderHintUpdateActions).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.33");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategoryOrderHints, true);

        assertThat(setCategoryOrderHintUpdateActions).hasSize(1);
        assertThat(setCategoryOrderHintUpdateActions.get(0).getAction()).isEqualTo("setCategoryOrderHint");
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        categoryOrderHintsMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.53");
        categoryOrderHintsMap.put("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.63");
        categoryOrderHintsMap.put("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.73");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategoryOrderHints, true);

        assertThat(setCategoryOrderHintUpdateActions).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        categoryOrderHintsMap.put("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.53");
        categoryOrderHintsMap.put("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.63");
        categoryOrderHintsMap.put("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.73");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategoryOrderHints, false);

        assertThat(setCategoryOrderHintUpdateActions).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Map<String, String> categoryOrderHintsMap = new HashMap<>();
        categoryOrderHintsMap.put("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f", "0.43");
        final CategoryOrderHints newProductCategoryOrderHints = CategoryOrderHints.of(categoryOrderHintsMap);

        final List<UpdateAction<Product>> setCategoryOrderHintUpdateActions =
            getSetCategoryOrderHintUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategoryOrderHints, false);

        assertThat(setCategoryOrderHintUpdateActions).isEmpty();
    }

    private List<UpdateAction<Product>> getSetCategoryOrderHintUpdateActions(@Nonnull final Product oldProduct,
                                                                      @Nonnull final CategoryOrderHints 
                                                                          newProductCategoryOrderHints,
                                                                      final boolean updateStaged) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getCategoryOrderHints()).thenReturn(newProductCategoryOrderHints);


        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildSetCategoryOrderHintUpdateActions(oldProduct, newProductDraft, productSyncOptions);
    }
}
