package com.commercetools.sync.products.utils.productupdateactionutils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.Unpublish;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildPublishOrUnpublishUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildPublishOrUnpublishUpdateActionTest {
    @Test
    void buildPublishUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final UpdateAction<Product> action =
            getPublishOrUnpublishUpdateAction(true, false).orElse(null);

        assertThat(action).isNotNull();
        assertThat(action).isEqualTo(Unpublish.of());
    }

    @Test
    void buildPublishUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> action =
            getPublishOrUnpublishUpdateAction(false, false);

        assertThat(action).isNotNull();
        assertThat(action).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getPublishOrUnpublishUpdateAction(
        final boolean isOldProductPublished, final boolean isNewProductPublished) {

        final ProductCatalogData productCatalogData = mock(ProductCatalogData.class);
        when(productCatalogData.isPublished()).thenReturn(isOldProductPublished);
        final Product oldProduct = mock(Product.class);
        when(oldProduct.getMasterData()).thenReturn(productCatalogData);

        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.isPublish()).thenReturn(isNewProductPublished);
        return buildPublishOrUnpublishUpdateAction(oldProduct, newProductDraft);
    }


}
