package com.commercetools.sync.products.utils.productvariantupdateactionutils;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantSkuUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.SetSku;
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
    when(variantOld.getId()).thenReturn(42);

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew))
        .contains(SetSku.of(42, null, true));
  }

  @Test
  void buildProductVariantSkuUpdateAction_WithNewSku_ShouldBuildUpdateAction() {
    final ProductVariant variantOld = mock(ProductVariant.class);
    final ProductVariantDraft variantDraftNew = mock(ProductVariantDraft.class);

    when(variantOld.getSku()).thenReturn("sku-old");
    when(variantOld.getId()).thenReturn(42);
    when(variantDraftNew.getSku()).thenReturn("sku-new");

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew))
        .contains(SetSku.of(42, "sku-new", true));
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
    when(variantOld.getId()).thenReturn(42);
    when(variantDraftNew.getSku()).thenReturn("sku-new");

    assertThat(buildProductVariantSkuUpdateAction(variantOld, variantDraftNew))
        .contains(SetSku.of(42, "sku-new", true));
  }
}
