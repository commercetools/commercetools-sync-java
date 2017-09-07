package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetSearchKeywords;
import io.sphere.sdk.search.SearchKeyword;
import io.sphere.sdk.search.SearchKeywords;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetSearchKeywordsUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildSetSearchKeywordsUpdateActionTest {
    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private static final Product MOCK_OLD_PUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_PUBLISHED_RESOURCE_PATH,
        Product.class);
    private static final Product MOCK_OLD_UNPUBLISHED_PRODUCT = readObjectFromResource(
        PRODUCT_KEY_1_UNPUBLISHED_RESOURCE_PATH,
        Product.class);

    @Test
    public void publishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of(Locale.ENGLISH,
            Arrays.asList(SearchKeyword.of("searchKeyword1"), SearchKeyword.of("searchKeyword2")));
        final UpdateAction<Product> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords, true)
                .orElse(null);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction.getAction()).isEqualTo("setSearchKeywords");
        assertThat(((SetSearchKeywords) setSearchKeywordsUpdateAction).getSearchKeywords())
            .isEqualTo(newProductSearchKeywords);
    }

    @Test
    public void publishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of();
        final Optional<UpdateAction<Product>> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords, true);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void publishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of(Locale.ENGLISH,
            Arrays.asList(SearchKeyword.of("searchKeyword1"), SearchKeyword.of("searchKeyword2")));
        final UpdateAction<Product> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords, false).orElse(null);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction.getAction()).isEqualTo("setSearchKeywords");
        assertThat(((SetSearchKeywords) setSearchKeywordsUpdateAction).getSearchKeywords())
            .isEqualTo(newProductSearchKeywords);
    }

    @Test
    public void publishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of();
        final Optional<UpdateAction<Product>> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_PUBLISHED_PRODUCT, newProductSearchKeywords, false);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentStagedValuesAndUpdateStaged_ShouldBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of(Locale.ENGLISH,
            Arrays.asList(SearchKeyword.of("searchKeyword1"), SearchKeyword.of("searchKeyword2")));
        final UpdateAction<Product> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductSearchKeywords, true).orElse(null);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction.getAction()).isEqualTo("setSearchKeywords");
        assertThat(((SetSearchKeywords) setSearchKeywordsUpdateAction).getSearchKeywords())
            .isEqualTo(newProductSearchKeywords);
    }

    @Test
    public void unpublishedProduct_WithSameStagedValuesAndUpdateStaged_ShouldNotBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of();
        final Optional<UpdateAction<Product>> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductSearchKeywords, true);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithDifferentCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of(Locale.ENGLISH,
            Arrays.asList(SearchKeyword.of("searchKeyword1"), SearchKeyword.of("searchKeyword2")));
        final Optional<UpdateAction<Product>> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductSearchKeywords, false);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction).isNotPresent();
    }

    @Test
    public void unpublishedProduct_WithSameCurrentValuesAndUpdateCurrent_ShouldNotBuildUpdateAction() {
        final SearchKeywords newProductSearchKeywords = SearchKeywords.of();
        final Optional<UpdateAction<Product>> setSearchKeywordsUpdateAction =
            getSetSearchKeywordsUpdateAction(MOCK_OLD_UNPUBLISHED_PRODUCT, newProductSearchKeywords, false);

        assertThat(setSearchKeywordsUpdateAction).isNotNull();
        assertThat(setSearchKeywordsUpdateAction).isNotPresent();
    }

    private Optional<UpdateAction<Product>> getSetSearchKeywordsUpdateAction(@Nonnull final Product oldProduct,
                                                                          @Nonnull final SearchKeywords
                                                                              newProductSearchKeywords,
                                                                          final boolean updateStaged) {
        final ProductDraft newProductDraft = mock(ProductDraft.class);
        when(newProductDraft.getSearchKeywords()).thenReturn(newProductSearchKeywords );

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_CLIENT)
                                                                               .updateStaged(updateStaged)
                                                                               .build();
        return buildSetSearchKeywordsUpdateAction(oldProduct, newProductDraft, productSyncOptions);
    }
}
