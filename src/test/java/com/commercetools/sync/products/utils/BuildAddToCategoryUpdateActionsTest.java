package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildAddToCategoryUpdateActionsTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(category.toReference());
        
        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories, true);

        assertThat(addToCategoryUpdateAction).hasSize(1);
        assertThat(addToCategoryUpdateAction.get(0).getAction()).isEqualTo("addToCategory");
        assertThat(((AddToCategory) addToCategoryUpdateAction.get(0)).getCategory()).isEqualTo(category.toReference());
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptySet(), true);

        assertThat(addToCategoryUpdateAction).isEmpty();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(category.toReference());

        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories, false);

        assertThat(addToCategoryUpdateAction).hasSize(1);
        assertThat(addToCategoryUpdateAction.get(0).getAction()).isEqualTo("addToCategory");
        assertThat(((AddToCategory) addToCategoryUpdateAction.get(0)).getCategory()).isEqualTo(category.toReference());
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptySet(), false);

        assertThat(addToCategoryUpdateAction).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(category.toReference());

        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategories, true);

        assertThat(addToCategoryUpdateAction).hasSize(1);
        assertThat(addToCategoryUpdateAction.get(0).getAction()).isEqualTo("addToCategory");
        assertThat(((AddToCategory) addToCategoryUpdateAction.get(0)).getCategory()).isEqualTo(category.toReference());
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, Collections.emptySet(), true);

        assertThat(addToCategoryUpdateAction).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
        final Set<Reference<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(category.toReference());

        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductCategories, false);

        assertThat(addToCategoryUpdateAction).isEmpty();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_UNPUBLISHED_PRODUCT, Collections.emptySet(), false);

        assertThat(addToCategoryUpdateAction).isEmpty();
    }

    private List<UpdateAction<Product>> getAddToCategoryUpdateActions(@Nonnull final Product oldProduct,
                                                                    @Nonnull final Set<Reference<Category>> 
                                                                        newProductCategories,
                                                                    final boolean updateStaged) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getCategories()).thenReturn(newProductCategories);
        

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildAddToCategoryUpdateActions(oldProduct, newProductDraft, productSyncOptions);
    }
}
