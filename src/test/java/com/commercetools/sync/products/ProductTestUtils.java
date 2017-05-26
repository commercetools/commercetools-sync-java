package com.commercetools.sync.products;

import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductData;

import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductTestUtils {
    public static LocalizedString localizedString(final String string) {
        if (string == null) {
            return null;
        }
        return LocalizedString.of(Locale.ENGLISH, string);
    }

    public static void addName(final Product productMock, final String name) {
        ProductCatalogData catalogData = mock(ProductCatalogData.class);
        ProductData data = mock(ProductData.class);
        when(data.getName()).thenReturn(localizedString(name));
        when(catalogData.getCurrent()).thenReturn(data);
        when(productMock.getMasterData()).thenReturn(catalogData);
    }
}
