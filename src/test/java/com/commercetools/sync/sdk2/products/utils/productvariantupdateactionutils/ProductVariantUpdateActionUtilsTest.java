package com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.ProductSetSkuActionBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import org.junit.jupiter.api.Test;

class ProductVariantUpdateActionUtilsTest {

  @Test
  void buildProductVariantSkuUpdateAction_WithBothNullSkus_ShouldNotBuildAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew)).isEmpty();
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNullNewSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-old");
    when(variantOld.getId()).thenReturn(42L);

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew))
        .contains(ProductSetSkuActionBuilder.of().variantId(42L).staged(true).build());
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNewSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-old");
    when(variantOld.getId()).thenReturn(42L);
    when(variantDraftNew.getSku()).thenReturn("sku-new");

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew))
        .contains(
            ProductSetSkuActionBuilder.of().variantId(42L).sku("sku-new").staged(true).build());
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithSameSku_ShouldNotBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-the-same");
    when(variantDraftNew.getSku()).thenReturn("sku-the-same");

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew)).isEmpty();
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNullOldSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn(null);
    when(variantOld.getId()).thenReturn(42L);
    when(variantDraftNew.getSku()).thenReturn("sku-new");

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew))
        .contains(
            ProductSetSkuActionBuilder.of().variantId(42L).sku("sku-new").staged(true).build());
  }
}
