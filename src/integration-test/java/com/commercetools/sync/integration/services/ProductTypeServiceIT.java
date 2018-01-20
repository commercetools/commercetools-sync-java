package com.commercetools.sync.integration.services;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class ProductTypeServiceIT {
    private ProductTypeService productTypeService;
    private static final String OLD_PRODUCT_TYPE_KEY = "old_product_type_key";
    private static final String OLD_PRODUCT_TYPE_NAME = "old_product_type_name";
    private static final Locale OLD_PRODUCT_TYPE_LOCALE = Locale.ENGLISH;

    /**
     * Deletes product types from the target CTP project, then it populates the project with test data.
     */
    @Before
    public void setup() {
        deleteProductTypes(CTP_TARGET_CLIENT);
        createProductType(OLD_PRODUCT_TYPE_KEY, OLD_PRODUCT_TYPE_LOCALE, OLD_PRODUCT_TYPE_NAME, CTP_TARGET_CLIENT);
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                               .build();
        productTypeService = new ProductTypeServiceImpl(productSyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterClass
    public static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
    }

    @Test
    public void fetchCachedProductTypeId_WithNonExistingProductType_ShouldNotFetchAProductType() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId("non-existing-type-key")
                                                          .toCompletableFuture()
                                                          .join();
        assertThat(productTypeId).isEmpty();
    }

    @Test
    public void fetchCachedProductTypeId_WithExistingProductType_ShouldFetchProductTypeAndCache() {
        final Optional<String> productTypeId = productTypeService.fetchCachedProductTypeId(OLD_PRODUCT_TYPE_KEY)
                                                          .toCompletableFuture()
                                                          .join();
        assertThat(productTypeId).isNotEmpty();
    }

    @Test
    public void fetchCachedProductTypeId_OnSecondTime_ShouldNotFindProductTypeInCache() {
        // Fetch any product type to populate cache
        productTypeService.fetchCachedProductTypeId("anyTypeKey").toCompletableFuture().join();

        // Create new type
        final String newProductTypeKey = "new_type_key";
        final ProductTypeDraft draft = ProductTypeDraftBuilder
            .of(newProductTypeKey, "typeName", "typeDescription", new ArrayList<>()).build();
        CTP_TARGET_CLIENT.execute(ProductTypeCreateCommand.of(draft)).toCompletableFuture().join();

        final Optional<String> newProductTypeId =
            productTypeService.fetchCachedProductTypeId(newProductTypeKey).toCompletableFuture().join();

        assertThat(newProductTypeId).isEmpty();
    }
}
