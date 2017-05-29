package com.commercetools.sync.it.products;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static com.commercetools.sync.it.products.ProductIntegrationTestUtils.deleteProducts;
import static com.commercetools.sync.it.products.ProductIntegrationTestUtils.populateSourceProject;
import static com.commercetools.sync.it.products.ProductIntegrationTestUtils.populateTargetProject;

/**
 * Empty.
 */
public class ProductSyncItTest {

    /**
     * Empty.
     */
    @Before
    public void setup() {
        deleteProducts();
        populateSourceProject();
        populateTargetProject();
    }

    /**
     * Empty.
     */
    @AfterClass
    public static void delete() {
        // deleteProducts();
    }

    @Test
    public void sync_WithNewProduct_ShouldCreateProduct() {
    }

}
