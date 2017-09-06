package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
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

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaDescriptionUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSetMetaDescriptionUpdateActionTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
        final UpdateAction<Product> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription, true).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription()).isEqualTo(newDescription);
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null, true);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
        final UpdateAction<Product> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newDescription, false).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription()).isEqualTo(newDescription);
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null, false);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
        final UpdateAction<Product> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newDescription, true).orElse(null);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction.getAction()).isEqualTo("setMetaDescription");
        assertThat(((SetMetaDescription) setMetaDescriptionUpdateAction).getMetaDescription()).isEqualTo(newDescription);
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, null, true);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final LocalizedString newDescription = LocalizedString.of(Locale.GERMAN, "newDescription");
        final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newDescription, false);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaDescriptionUpdateAction =
            getSetMetaDescriptionUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, null, false);

        assertThat(setMetaDescriptionUpdateAction).isNotNull();
        assertThat(setMetaDescriptionUpdateAction).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getSetMetaDescriptionUpdateAction(@Nonnull final Product oldProduct,
                                                                              @Nullable final LocalizedString
                                                                              newProductMetaDescription,
                                                                              final boolean updateStaged) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getMetaDescription()).thenReturn(newProductMetaDescription);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildSetMetaDescriptionUpdateAction(oldProduct, newProductDraft, productSyncOptions);
    }
}
