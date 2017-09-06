package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveFromCategoryUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildRemoveFromCategoryUpdateActionsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptySet(), true);

        assertThat(removeFromCategoryUpdateActions).hasSize(4);
        removeFromCategoryUpdateActions.forEach(updateAction -> assertThat(updateAction.getAction()).isEqualTo("removeFromCategory"));
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
        newProductCategories.add(Category.referenceOfId("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
        newProductCategories.add(Category.referenceOfId("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
        newProductCategories.add(Category.referenceOfId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories, true);

        assertThat(removeFromCategoryUpdateActions).isEmpty();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptySet(), false);

        assertThat(removeFromCategoryUpdateActions).hasSize(1);
        assertThat(removeFromCategoryUpdateActions.get(0).getAction()).isEqualTo("removeFromCategory");
        assertThat(((RemoveFromCategory)removeFromCategoryUpdateActions.get(0)).getCategory())
            .isEqualTo(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories, false);

        assertThat(removeFromCategoryUpdateActions).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final List<UpdateAction<Product>> removeFromCategoryUpdateActions =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, Collections.emptySet(), true);

        assertThat(removeFromCategoryUpdateActions).hasSize(4);
        removeFromCategoryUpdateActions.forEach(updateAction -> assertThat(updateAction.getAction()).isEqualTo("removeFromCategory"));
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
        newProductCategories.add(Category.referenceOfId("2dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
        newProductCategories.add(Category.referenceOfId("3dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));
        newProductCategories.add(Category.referenceOfId("4dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final List<UpdateAction<Product>> removeFromCategoryUpdateAction =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategories, true);

        assertThat(removeFromCategoryUpdateAction).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final List<UpdateAction<Product>> removeFromCategoryUpdateAction =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, Collections.emptySet(), false);

        assertThat(removeFromCategoryUpdateAction).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(Category.referenceOfId("1dfc8bea-84f2-45bc-b3c2-cdc94bf96f1f"));

        final List<UpdateAction<Product>> removeFromCategoryUpdateAction =
            getRemoveFromCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategories, false);

        assertThat(removeFromCategoryUpdateAction).isEmpty();
    }

    private List<UpdateAction<Product>> getRemoveFromCategoryUpdateActions(@Nonnull final Product oldProduct,
                                                                      @Nonnull final Set<Reference<Category>>
                                                                          newProductCategories,
                                                                      final boolean updateStaged) {

        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getCategories()).thenReturn(newProductCategories);


        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildRemoveFromCategoryUpdateActions(oldProduct, newProductDraft, productSyncOptions);
    }
}
