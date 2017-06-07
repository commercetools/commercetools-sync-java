package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;
import org.junit.Test;

import static com.commercetools.sync.products.helpers.ProductSyncUtils.masterData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductSyncUtilsTest {

    @Test
    public void masterData_test() {
        ProductSyncOptions syncOptions = mock(ProductSyncOptions.class);
        Product product = mock(Product.class);
        ProductCatalogData catalogData = mock(ProductCatalogData.class);
        when(product.getMasterData()).thenReturn(catalogData);
        ProductData current = mock(ProductData.class);
        ProductData staged = mock(ProductData.class);
        when(catalogData.getCurrent()).thenReturn(current);
        when(catalogData.getStaged()).thenReturn(staged);

        when(syncOptions.isCompareStaged()).thenReturn(true);
        assertThat(masterData(product, syncOptions)).isSameAs(staged);
        when(syncOptions.isCompareStaged()).thenReturn(false);
        assertThat(masterData(product, syncOptions)).isSameAs(current);
    }

}