package com.commercetools.sync.products.utils;

import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import org.junit.Test;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductVariantUpdateActionUtilsTest {

    @Test
    public void buildProductVariantSkuUpdateActions_normalCase() throws Exception {
        final ProductVariant variantOld = mock(ProductVariant.class);
        final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

        assertThat(buildProductVariantSkuUpdateActions(variantOld, variantDraftNew)).isEmpty();

        when(variantOld.getSku()).thenReturn("sku-old");
        when(variantOld.getId()).thenReturn(42);
        assertThat(buildProductVariantSkuUpdateActions(variantOld, variantDraftNew))
                .containsExactly(SetSku.of(42, null, true));

        when(variantDraftNew.getSku()).thenReturn("sku-new");
        assertThat(buildProductVariantSkuUpdateActions(variantOld, variantDraftNew))
                .containsExactly(SetSku.of(42, "sku-new", true));

        when(variantOld.getSku()).thenReturn("sku-the-same");
        when(variantDraftNew.getSku()).thenReturn("sku-the-same");
        assertThat(buildProductVariantSkuUpdateActions(variantOld, variantDraftNew)).isEmpty();

        when(variantOld.getSku()).thenReturn(null);
        when(variantOld.getId()).thenReturn(42);
        when(variantDraftNew.getSku()).thenReturn("sku-new");
        assertThat(buildProductVariantSkuUpdateActions(variantOld, variantDraftNew))
                .containsExactly(SetSku.of(42, "sku-new", true));
    }
}