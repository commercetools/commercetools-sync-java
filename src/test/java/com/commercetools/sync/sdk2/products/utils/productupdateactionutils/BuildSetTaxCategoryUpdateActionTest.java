package com.commercetools.sync.sdk2.products.utils.productupdateactionutils;

import static com.commercetools.sync.sdk2.products.utils.ProductUpdateActionUtils.buildSetTaxCategoryUpdateAction;
import static io.vrap.rmf.base.client.utils.json.JsonUtils.fromJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductSetTaxCategoryActionBuilder;
import com.commercetools.api.models.tax_category.TaxCategoryReference;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import org.junit.jupiter.api.Test;

class BuildSetTaxCategoryUpdateActionTest {

  private ProductProjection oldProduct = mock(ProductProjection.class);

  private ProductDraft newProduct = mock(ProductDraft.class);

  @SuppressWarnings("unchecked")
  private static final TaxCategoryReference oldTaxCategory =
      fromJsonString(
          "{\"typeId\": \"tax-category\",\"id\": \"11111111-1111-1111-1111-111111111111\"}",
          TaxCategoryReference.class);

  @SuppressWarnings("unchecked")
  private static final TaxCategoryResourceIdentifier newSameTaxCategory =
      fromJsonString(
          "{\"typeId\": \"tax-category\",\"id\": \"11111111-1111-1111-1111-111111111111\"}",
          TaxCategoryResourceIdentifier.class);

  @SuppressWarnings("unchecked")
  private static final TaxCategoryResourceIdentifier newChangedTaxCategory =
      fromJsonString(
          "{\"typeId\": \"tax-category\",\"id\": \"22222222-2222-2222-2222-222222222222\"}",
          TaxCategoryResourceIdentifier.class);

  @Test
  void buildSetTaxCategoryUpdateAction_withEmptyOld_containsNewCategory() {
    assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).isEmpty();

    when(newProduct.getTaxCategory()).thenReturn(newSameTaxCategory);
    assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct))
        .contains(ProductSetTaxCategoryActionBuilder.of().taxCategory(newSameTaxCategory).build());
  }

  @Test
  void buildSetTaxCategoryUpdateAction_withEmptyNew_ShouldUnset() {
    assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).isEmpty();

    when(oldProduct.getTaxCategory()).thenReturn(oldTaxCategory);
    assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct))
        .contains(ProductSetTaxCategoryActionBuilder.of().build());
  }

  @Test
  void buildSetTaxCategoryUpdateAction_withEqual_isEmpty() {
    when(oldProduct.getTaxCategory()).thenReturn(oldTaxCategory);
    when(newProduct.getTaxCategory()).thenReturn(newSameTaxCategory);
    assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).isEmpty();
  }

  @Test
  void buildSetTaxCategoryUpdateAction_withDifferent_containsNew() {
    when(oldProduct.getTaxCategory()).thenReturn(oldTaxCategory);
    when(newProduct.getTaxCategory()).thenReturn(newChangedTaxCategory);
    assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct))
        .contains(
            ProductSetTaxCategoryActionBuilder.of().taxCategory(newChangedTaxCategory).build());
  }
}
