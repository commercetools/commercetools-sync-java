package com.commercetools.sync.products.utils.productupdateactionutils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetMetaDescription;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSetMetaDescriptionUpdateActionTest {
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_RESOURCE_PATH, Product.class);

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
        final UpdateAction<Product> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription())
            .isEqualTo(newDescription);
    }

    @Test
    public void buildSetMetaDescriptionUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getSetMetaDescriptionUpdateAction(@Nonnull final Product oldProduct,
                                                                              @Nullable final LocalizedString
                                                                                  newProductMetaDescription) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getMetaDescription()).thenReturn(newProductMetaDescription);
        return buildSetMetaDescriptionUpdateAction(oldProduct, newProductDraft);
    }
}
