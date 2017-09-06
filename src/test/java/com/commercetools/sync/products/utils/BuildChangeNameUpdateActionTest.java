package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeNameUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildChangeNameUpdateActionTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");
        final UpdateAction<Product> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName, true).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(newName);
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "Rehrücken ohne Knochen");
        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName, true);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");
        final UpdateAction<Product> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName, false).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(newName);
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "Rehrücken ohne Knochen");
        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newName, false);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");
        final UpdateAction<Product> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newName, true).orElse(null);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction.getAction()).isEqualTo("changeName");
        assertThat(((ChangeName) changeNameUpdateAction).getName()).isEqualTo(newName);
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "Rehrücken ohne Knochen");
        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newName, true);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.GERMAN, "newName");
        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newName, false);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final LocalizedString newName = LocalizedString.of(Locale.ENGLISH, "Rehrücken ohne Knochen");
        final Optional<UpdateAction<Product>> changeNameUpdateAction =
            getChangeNameUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newName, false);

        assertThat(changeNameUpdateAction).isNotNull();
        assertThat(changeNameUpdateAction).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getChangeNameUpdateAction(@Nonnull final Product oldProduct,
                                                                      @Nonnull final LocalizedString newProductName,
                                                                      final boolean updateStaged) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getName()).thenReturn(newProductName);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildChangeNameUpdateAction(oldProduct, newProductDraft, productSyncOptions);
    }
}
