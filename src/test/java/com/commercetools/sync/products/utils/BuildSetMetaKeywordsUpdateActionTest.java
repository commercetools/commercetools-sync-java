package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetMetaKeywords;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetMetaKeywordsUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSetMetaKeywordsUpdateActionTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final LocalizedString newKeywords = LocalizedString.of(Locale.GERMAN, "newKeywords");
        final UpdateAction<Product> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newKeywords, true).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords()).isEqualTo(newKeywords);
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null, true);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final LocalizedString newKeywords = LocalizedString.of(Locale.GERMAN, "newKeywords");
        final UpdateAction<Product> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newKeywords, false).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords()).isEqualTo(newKeywords);
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, null, false);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final LocalizedString newKeywords = LocalizedString.of(Locale.GERMAN, "newKeywords");
        final UpdateAction<Product> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newKeywords, true).orElse(null);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction.getAction()).isEqualTo("setMetaKeywords");
        assertThat(((SetMetaKeywords) setMetaKeywordsUpdateAction).getMetaKeywords()).isEqualTo(newKeywords);
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, null, true);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final LocalizedString newKeywords = LocalizedString.of(Locale.GERMAN, "newKeywords");
        final Optional<UpdateAction<Product>> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newKeywords, false);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final Optional<UpdateAction<Product>> setMetaKeywordsUpdateAction =
            getSetMetaKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, null, false);

        assertThat(setMetaKeywordsUpdateAction).isNotNull();
        assertThat(setMetaKeywordsUpdateAction).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getSetMetaKeywordsUpdateAction(@Nonnull final Product oldProduct,
                                                                              @Nullable final LocalizedString
                                                                                  newProductMetaKeywords,
                                                                              final boolean updateStaged) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getMetaKeywords()).thenReturn(newProductMetaKeywords);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildSetMetaKeywordsUpdateAction(oldProduct, newProductDraft, productSyncOptions);
    }
}
