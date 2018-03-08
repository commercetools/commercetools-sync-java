package com.commercetools.sync.products.utils.productupdateactionutils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeNameUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildChangeNameUpdateActionTest {
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_RESOURCE_PATH, Product.class);

    @Test
    public void buildChangeNameUpdateAction_WithDifferentStagedValues_ShouldBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");
        final UpdateAction<Product> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(newName);
    }

    @Test
    public void buildChangeNameUpdateAction_WithSameStagedValues_ShouldNotBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "english name");
        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getChangeNameUpdateAction(@Nonnull final Product oldProduct,
                                                                      @Nonnull final LocalizedString newProductName) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getName()).thenReturn(newProductName);
        return buildChangeNameUpdateAction(oldProduct, newProductDraft);
    }
}
