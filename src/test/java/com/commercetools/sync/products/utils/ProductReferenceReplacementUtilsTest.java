package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncMockUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.commercetools.sync.products.ProductSyncMockUtils.createProductFromJson;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductReferenceReplacementUtilsTest {

    @Test
    public void replaceProductsReferenceIdsWithKeys_WithSomeExpandedReferences_ShouldReplaceReferencesWhereExpanded() {
        final String resourceKey = "key";
        final ProductType productType = getProductTypeMock(resourceKey);
        final Reference<ProductType> productTypeReference =
            Reference.ofResourceTypeIdAndIdAndObj(ProductType.referenceTypeId(), productType.getId(), productType);
        final Reference<ProductType> nonExpandedProductTypeReference = ProductType.referenceOfId(productType.getId());

        final TaxCategory taxCategory = getTaxCategoryMock(resourceKey);
        final Reference<TaxCategory> taxCategoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(TaxCategory.referenceTypeId(), taxCategory.getId(), taxCategory);
        final Reference<TaxCategory> nonExpandedTaxCategoryReference = TaxCategory.referenceOfId(taxCategory.getId());

        final State state = getStateMock(resourceKey);
        final Reference<State> stateReference =
            Reference.ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), state.getId(), state);
        final Reference<State> nonExpandedStateReference = State.referenceOfId(state.getId());

        final Product productWithNonExpandedProductType =
            spy(createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH));

        when(productWithNonExpandedProductType.getProductType())
            .thenReturn(nonExpandedProductTypeReference);
        when(productWithNonExpandedProductType.getTaxCategory()).thenReturn(taxCategoryReference);
        when(productWithNonExpandedProductType.getState()).thenReturn(stateReference);

        final Product productWithNonExpandedTaxCategory =
            spy(createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH));

        when(productWithNonExpandedTaxCategory.getProductType()).thenReturn(productTypeReference);
        when(productWithNonExpandedTaxCategory.getTaxCategory())
            .thenReturn(nonExpandedTaxCategoryReference);
        when(productWithNonExpandedTaxCategory.getState()).thenReturn(stateReference);

        final Product productWithNonExpandedSate =
            spy(createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH));

        when(productWithNonExpandedSate.getProductType()).thenReturn(productTypeReference);
        when(productWithNonExpandedSate.getTaxCategory()).thenReturn(taxCategoryReference);
        when(productWithNonExpandedSate.getState()).thenReturn(nonExpandedStateReference);


        final List<Product> products = Arrays
            .asList(productWithNonExpandedProductType, productWithNonExpandedTaxCategory, productWithNonExpandedSate);


        final List<ProductDraft> productDraftsWithKeysOnReferences = ProductReferenceReplacementUtils
            .replaceProductsReferenceIdsWithKeys(products);

        assertThat(productDraftsWithKeysOnReferences).hasSize(3);

        assertThat(productDraftsWithKeysOnReferences.get(0).getProductType().getId()).isEqualTo(productType.getId());
        assertThat(productDraftsWithKeysOnReferences.get(0).getTaxCategory().getId()).isEqualTo(taxCategory.getKey());
        assertThat(productDraftsWithKeysOnReferences.get(0).getState().getId()).isEqualTo(state.getKey());

        assertThat(productDraftsWithKeysOnReferences.get(1).getProductType().getId()).isEqualTo(productType.getKey());
        assertThat(productDraftsWithKeysOnReferences.get(1).getTaxCategory().getId()).isEqualTo(taxCategory.getId());
        assertThat(productDraftsWithKeysOnReferences.get(1).getState().getId()).isEqualTo(state.getKey());

        assertThat(productDraftsWithKeysOnReferences.get(2).getProductType().getId()).isEqualTo(productType.getKey());
        assertThat(productDraftsWithKeysOnReferences.get(2).getTaxCategory().getId()).isEqualTo(taxCategory.getKey());
        assertThat(productDraftsWithKeysOnReferences.get(2).getState().getId()).isEqualTo(state.getId());

    }

    @Test
    public void replaceProductsReferenceIdsWithKeys_WithNullProducts_ShouldSkipNullProducts() {
        final String resourceKey = "key";
        final ProductType productType = getProductTypeMock(resourceKey);
        final Reference<ProductType> productTypeReference =
            Reference.ofResourceTypeIdAndIdAndObj(ProductType.referenceTypeId(), productType.getId(), productType);
        final Reference<ProductType> nonExpandedProductTypeReference = ProductType.referenceOfId(productType.getId());

        final TaxCategory taxCategory = getTaxCategoryMock(resourceKey);
        final Reference<TaxCategory> taxCategoryReference =
            Reference.ofResourceTypeIdAndIdAndObj(TaxCategory.referenceTypeId(), taxCategory.getId(), taxCategory);
        final Reference<TaxCategory> nonExpandedTaxCategoryReference = TaxCategory.referenceOfId(taxCategory.getId());

        final State state = getStateMock(resourceKey);
        final Reference<State> stateReference =
            Reference.ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), state.getId(), state);

        final Product productWithNonExpandedProductType =
            spy(createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH));

        when(productWithNonExpandedProductType.getProductType())
            .thenReturn(nonExpandedProductTypeReference);
        when(productWithNonExpandedProductType.getTaxCategory()).thenReturn(taxCategoryReference);
        when(productWithNonExpandedProductType.getState()).thenReturn(stateReference);

        final Product productWithNonExpandedTaxCategory =
            spy(createProductFromJson(ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH));

        when(productWithNonExpandedTaxCategory.getProductType()).thenReturn(productTypeReference);
        when(productWithNonExpandedTaxCategory.getTaxCategory())
            .thenReturn(nonExpandedTaxCategoryReference);
        when(productWithNonExpandedTaxCategory.getState()).thenReturn(stateReference);


        final List<Product> products = Arrays
            .asList(productWithNonExpandedProductType, productWithNonExpandedTaxCategory, null);


        final List<ProductDraft> productDraftsWithKeysOnReferences = ProductReferenceReplacementUtils
            .replaceProductsReferenceIdsWithKeys(products);

        assertThat(productDraftsWithKeysOnReferences).hasSize(2);

        assertThat(productDraftsWithKeysOnReferences.get(0).getProductType().getId()).isEqualTo(productType.getId());
        assertThat(productDraftsWithKeysOnReferences.get(0).getTaxCategory().getId()).isEqualTo(taxCategory.getKey());
        assertThat(productDraftsWithKeysOnReferences.get(0).getState().getId()).isEqualTo(state.getKey());

        assertThat(productDraftsWithKeysOnReferences.get(1).getProductType().getId()).isEqualTo(productType.getKey());
        assertThat(productDraftsWithKeysOnReferences.get(1).getTaxCategory().getId()).isEqualTo(taxCategory.getId());
        assertThat(productDraftsWithKeysOnReferences.get(1).getState().getId()).isEqualTo(state.getKey());
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
    public void replaceProductTypeReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String productTypeId = UUID.randomUUID().toString();
        final String productTypeKey = "productTypeKey";

        final ProductType productType = getProductTypeMock(productTypeKey);

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
    public void replaceTaxCategoryReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String taxCategoryId = UUID.randomUUID().toString();
        final String taxCategoryKey = "taxCategoryKey";

        final TaxCategory taxCategory = getTaxCategoryMock(taxCategoryKey);
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
    public void replaceStateReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
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
    public void replaceStateReferenceIdWithKey_WithExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String stateId = UUID.randomUUID().toString();
        final String stateKey = "stateKey";

        final State state = getStateMock(stateKey);
        final Reference<State> stateReference = Reference
            .ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), stateId, state);

        final Product product = mock(Product.class);
        when(product.getState()).thenReturn(stateReference);

        final Reference<State> stateReferenceWithKey = ProductReferenceReplacementUtils
            .replaceProductStateReferenceIdWithKey(product);

        assertThat(stateReferenceWithKey).isNotNull();
        assertThat(stateReferenceWithKey.getId()).isEqualTo(stateKey);
    }

    private static ProductType getProductTypeMock(@Nonnull final String key) {
        final ProductType productType = mock(ProductType.class);
        when(productType.getKey()).thenReturn(key);
        when(productType.getId()).thenReturn(UUID.randomUUID().toString());
        return productType;
    }

    private static TaxCategory getTaxCategoryMock(@Nonnull final String key) {
        final TaxCategory taxCategory = mock(TaxCategory.class);
        when(taxCategory.getKey()).thenReturn(key);
        when(taxCategory.getId()).thenReturn(UUID.randomUUID().toString());
        return taxCategory;
    }

    private static State getStateMock(@Nonnull final String key) {
        final State state = mock(State.class);
        when(state.getKey()).thenReturn(key);
        when(state.getId()).thenReturn(UUID.randomUUID().toString());
        return state;
    }
}
