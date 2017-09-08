package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;
import org.junit.Test;

import static com.commercetools.sync.products.utils.ProductDataUtils.masterData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductDataUtilsTest {

    @Test
    public void masterData_test() {
        final Product product = mock(Product.class);
        final ProductCatalogData catalogData = mock(ProductCatalogData.class);
        when(product.getMasterData()).thenReturn(catalogData);

        final ProductData current = mock(ProductData.class);
        final ProductData staged = mock(ProductData.class);
        when(catalogData.getCurrent()).thenReturn(current);
        when(catalogData.getStaged()).thenReturn(staged);

        final ProductSyncOptions syncOptions = mock(ProductSyncOptions.class);
        when(syncOptions.shouldUpdateStaged()).thenReturn(true);
        assertThat(masterData(product, syncOptions)).isSameAs(staged);

        when(syncOptions.shouldUpdateStaged()).thenReturn(false);
        assertThat(masterData(product, syncOptions)).isSameAs(current);
    }

}