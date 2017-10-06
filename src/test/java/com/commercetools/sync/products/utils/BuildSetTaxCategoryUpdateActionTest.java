package com.commercetools.sync.products.utils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.SetTaxCategory;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildSetTaxCategoryUpdateAction;
import static io.sphere.sdk.json.SphereJsonUtils.readObject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuildSetTaxCategoryUpdateActionTest {

    @Mock
    private Product oldProduct;

    @Mock
    private ProductDraft newProduct;

    @SuppressWarnings("unchecked")
    private static final Reference<TaxCategory> oldTaxCategory = readObject(
        "{\"typeId\": \"tax-category\",\"id\": \"11111111-1111-1111-1111-111111111111\"}", Reference.class);

    @SuppressWarnings("unchecked")
    private static final Reference<TaxCategory> newSameTaxCategory = readObject(
        "{\"typeId\": \"tax-category\",\"id\": \"11111111-1111-1111-1111-111111111111\"}", Reference.class);

    @SuppressWarnings("unchecked")
    private static final Reference<TaxCategory> newChangedTaxCategory = readObject(
        "{\"typeId\": \"tax-category\",\"id\": \"22222222-2222-2222-2222-222222222222\"}", Reference.class);

    @Test
    public void buildSetTaxCategoryUpdateAction_withEmptyOld() throws Exception {
        assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).isEmpty();

        when(newProduct.getTaxCategory()).thenReturn(newSameTaxCategory);
        assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct))
            .contains(SetTaxCategory.of(newSameTaxCategory));
    }

    @Test
    public void buildSetTaxCategoryUpdateAction_withEmptyNewShouldUnset() throws Exception {
        assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).isEmpty();

        when(oldProduct.getTaxCategory()).thenReturn(oldTaxCategory);
        assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).contains(SetTaxCategory.of(null));
    }

    @Test
    public void buildSetTaxCategoryUpdateAction_withEqual() throws Exception {
        when(oldProduct.getTaxCategory()).thenReturn(oldTaxCategory);
        when(newProduct.getTaxCategory()).thenReturn(newSameTaxCategory);
        assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct)).isEmpty();
    }

    @Test
    public void buildSetTaxCategoryUpdateAction_withDifferent() throws Exception {
        when(oldProduct.getTaxCategory()).thenReturn(oldTaxCategory);
        when(newProduct.getTaxCategory()).thenReturn(newChangedTaxCategory);
        assertThat(buildSetTaxCategoryUpdateAction(oldProduct, newProduct))
            .contains(SetTaxCategory.of(newChangedTaxCategory));
    }

}