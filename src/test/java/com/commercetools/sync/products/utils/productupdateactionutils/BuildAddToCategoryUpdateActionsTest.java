package com.commercetools.sync.products.utils.productupdateactionutils;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.commercetools.sync.products.ProductSyncMockUtils.CATEGORY_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddToCategoryUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildAddToCategoryUpdateActionsTest {
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_RESOURCE_PATH, Product.class);

    @Test
    void buildAddToCategoryUpdateActions_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final Category category = readObjectFromResource(CATEGORY_KEY_1_RESOURCE_PATH, Category.class);
        final Set<ResourceIdentifier<Category>> newProductCategories = new HashSet<>();
        newProductCategories.add(category.toResourceIdentifier());

        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, newProductCategories);

        assertThat(addToCategoryUpdateAction).hasSize(1);
        assertThat(addToCategoryUpdateAction.get(0).getAction()).isEqualTo("addToCategory");
        assertThat(((AddToCategory) addToCategoryUpdateAction.get(0)).getCategory()).isEqualTo(category.toReference());
    }

    @Test
    void buildAddToCategoryUpdateActions_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final List<UpdateAction<Product>> addToCategoryUpdateAction =
            getAddToCategoryUpdateActions(MOCK_OLD_PUBLISHED_PRODUCT, Collections.emptySet());

        assertThat(addToCategoryUpdateAction).isEmpty();
    }

    private List<UpdateAction<Product>> getAddToCategoryUpdateActions(@Nonnull final Product oldProduct,
                                                                      @Nonnull final Set<ResourceIdentifier<Category>>
                                                                          newProductCategories) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getCategories()).thenReturn(newProductCategories);
        return buildAddToCategoryUpdateActions(oldProduct, newProductDraft);
    }
}
