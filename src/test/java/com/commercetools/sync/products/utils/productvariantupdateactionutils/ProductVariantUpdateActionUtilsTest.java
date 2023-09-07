package com.commercetools.sync.products.utils.productvariantupdateactionutils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.ProductSetSkuActionBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductVariantUpdateActionUtilsTest {

  @Test
  void buildProductVariantSkuUpdateAction_WithBothNullSkus_ShouldNotBuildAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    Assertions.assertThat(
            ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction(
                variantOld, variantDraftNew))
        .isEmpty();
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNullNewSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-old");
    when(variantOld.getId()).thenReturn(42L);

    Assertions.assertThat(
            ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction(
                variantOld, variantDraftNew))
        .contains(ProductSetSkuActionBuilder.of().variantId(42L).staged(true).build());
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNewSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-old");
    when(variantOld.getId()).thenReturn(42L);
    when(variantDraftNew.getSku()).thenReturn("sku-new");

    Assertions.assertThat(
            ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction(
                variantOld, variantDraftNew))
        .contains(
            ProductSetSkuActionBuilder.of().variantId(42L).sku("sku-new").staged(true).build());
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithSameSku_ShouldNotBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-the-same");
    when(variantDraftNew.getSku()).thenReturn("sku-the-same");

    Assertions.assertThat(
            ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction(
                variantOld, variantDraftNew))
        .isEmpty();
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNullOldSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn(null);
    when(variantOld.getId()).thenReturn(42L);
    when(variantDraftNew.getSku()).thenReturn("sku-new");

    Assertions.assertThat(
            ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction(
                variantOld, variantDraftNew))
        .contains(
            ProductSetSkuActionBuilder.of().variantId(42L).sku("sku-new").staged(true).build());
  }
}
