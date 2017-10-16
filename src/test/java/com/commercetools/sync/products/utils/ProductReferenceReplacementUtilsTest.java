package com.commercetools.sync.products.utils;

import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductReferenceReplacementUtilsTest {

    @Test
    public void
        //TODO
    replaceProductsReferenceIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String productTypeId = UUID.randomUUID().toString();
        final Reference<ProductType> productTypeReference = ProductType.referenceOfId(productTypeId);
        final Product product = mock(Product.class);
        when(product.getProductType()).thenReturn(productTypeReference);

        final Reference<ProductType> productTypeReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductTypeReferenceIdWithKey(product);

        assertThat(productTypeReferenceWithKey).isNotNull();
        assertThat(productTypeReferenceWithKey.getId()).isEqualTo(productTypeId);
    }


    @Test
    public void
    replaceProductTypeReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String productTypeId = UUID.randomUUID().toString();
        final Reference<ProductType> productTypeReference = ProductType.referenceOfId(productTypeId);
        final Product product = mock(Product.class);
        when(product.getProductType()).thenReturn(productTypeReference);

        final Reference<ProductType> productTypeReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductTypeReferenceIdWithKey(product);

        assertThat(productTypeReferenceWithKey).isNotNull();
        assertThat(productTypeReferenceWithKey.getId()).isEqualTo(productTypeId);
    }

    @Test
    public void
    replaceProductTypeReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String productTypeId = UUID.randomUUID().toString();
        final String productTypeKey = "productTypeKey";

        final ProductType productType = mock(ProductType.class);
        when(productType.getKey()).thenReturn(productTypeKey);
        when(productType.getId()).thenReturn(productTypeId);

        final Reference<ProductType> productTypeReference = Reference
            .ofResourceTypeIdAndIdAndObj(ProductType.referenceTypeId(), productTypeId, productType);

        final Product product = mock(Product.class);
        when(product.getProductType()).thenReturn(productTypeReference);

        final Reference<ProductType> productTypeReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductTypeReferenceIdWithKey(product);

        assertThat(productTypeReferenceWithKey).isNotNull();
        assertThat(productTypeReferenceWithKey.getId()).isEqualTo(productTypeKey);
    }

    @Test
    public void
    replaceTaxCategoryReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String taxCategoryId = UUID.randomUUID().toString();
        final Reference<TaxCategory> taxCategoryReference = TaxCategory.referenceOfId(taxCategoryId);
        final Product product = mock(Product.class);
        when(product.getTaxCategory()).thenReturn(taxCategoryReference);

        final Reference<TaxCategory> taxCategoryReferenceWithKey = ProductReferenceReplacementUtils
            .replaceTaxCategoryReferenceIdWithKey(product);

        assertThat(taxCategoryReferenceWithKey).isNotNull();
        assertThat(taxCategoryReferenceWithKey.getId()).isEqualTo(taxCategoryId);
    }

    @Test
    public void
    replaceTaxCategoryReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String taxCategoryId = UUID.randomUUID().toString();
        final String taxCategoryKey = "taxCategoryKey";

        final TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn(taxCategoryKey);
        when(taxCategory.getId()).thenReturn(taxCategoryId);

        final Reference<TaxCategory> taxCategoryReference = Reference
            .ofResourceTypeIdAndIdAndObj(TaxCategory.referenceTypeId(), taxCategoryId, taxCategory);

        final Product product = mock(Product.class);
        when(product.getTaxCategory()).thenReturn(taxCategoryReference);

        final Reference<TaxCategory> taxCategoryReferenceWithKey = ProductReferenceReplacementUtils
            .replaceTaxCategoryReferenceIdWithKey(product);

        assertThat(taxCategoryReferenceWithKey).isNotNull();
        assertThat(taxCategoryReferenceWithKey.getId()).isEqualTo(taxCategoryKey);
    }

    @Test
    public void
    replaceStateReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String stateId = UUID.randomUUID().toString();
        final Reference<State> stateReference = State.referenceOfId(stateId);
        final Product product = mock(Product.class);
        when(product.getState()).thenReturn(stateReference);

        final Reference<State> stateReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductStateReferenceIdWithKey(product);

        assertThat(stateReferenceWithKey).isNotNull();
        assertThat(stateReferenceWithKey.getId()).isEqualTo(stateId);
    }

    @Test
    public void
    replaceStateReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String stateId = UUID.randomUUID().toString();
        final String stateKey = "stateKey";

        final State state = mock(State.class);
        when(state.getKey()).thenReturn(stateKey);
        when(state.getId()).thenReturn(stateId);

        final Reference<State> stateReference = Reference
            .ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), stateId, state);

        final Product product = mock(Product.class);
        when(product.getState()).thenReturn(stateReference);

        final Reference<State> stateReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductStateReferenceIdWithKey(product);

        assertThat(stateReferenceWithKey).isNotNull();
        assertThat(stateReferenceWithKey.getId()).isEqualTo(stateKey);
    }
}
