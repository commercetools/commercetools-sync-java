package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.commons.models.ProductWithUnResolvedProductReferences;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.LazyResolutionService;
import com.commercetools.sync.services.impl.LazyResolutionServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.DeleteCommand;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.deleteAllCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.deleteProductTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LazyResolutionServiceImplIT {

    private static final String testContainerKey = "commercetools-sync-java.LazyResolutionService";
    private LazyResolutionService lazyResolutionService;
    private static ProductType productType;
    private static List<Reference<Category>> categoryReferencesWithIds;

    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    @BeforeAll
    static void setup() {
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
                OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        final List<Category> categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null,
                2));
        categoryReferencesWithIds = getReferencesWithIds(categories);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }


    @AfterAll
    static void tearDown() {
        deleteProductTypes(CTP_TARGET_CLIENT);
        deleteAllCategories(CTP_TARGET_CLIENT);
        deleteCustomObjects(CTP_TARGET_CLIENT);
    }

    private static void deleteCustomObjects(final SphereClient sphereClient) {
        CustomObjectQuery<ProductWithUnResolvedProductReferences> customObjectQuery = CustomObjectQuery
                .of(ProductWithUnResolvedProductReferences.class).byContainer(testContainerKey);
        List<CustomObject<ProductWithUnResolvedProductReferences>> existingCOs = sphereClient
                .execute(customObjectQuery).toCompletableFuture().join().getResults();

        existingCOs.forEach(obj -> {
            DeleteCommand<CustomObject<ProductWithUnResolvedProductReferences>> deleteCommand = CustomObjectDeleteCommand
                    .of(obj, ProductWithUnResolvedProductReferences.class);
            sphereClient.execute(deleteCommand).toCompletableFuture().join();
        });
    }

    @BeforeEach
    void setupTest() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (errorMessage, exception) -> {
                    errorCallBackMessages
                            .add(errorMessage);
                    errorCallBackExceptions
                            .add(exception);
                })
            .warningCallback(warningMessage ->
                    warningCallBackMessages
                            .add(warningMessage))
            .build();


        lazyResolutionService = new LazyResolutionServiceImpl(productSyncOptions);
    }


    @Test
    void save_shouldCreateNewCustomObject() {
        // preparation
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
                productType.toReference(), null, null, categoryReferencesWithIds,
                createRandomCategoryOrderHints(categoryReferencesWithIds));
        ProductWithUnResolvedProductReferences valueObject =
                new ProductWithUnResolvedProductReferences(productDraft,
                        null);

        // test
        Optional<CustomObject<ProductWithUnResolvedProductReferences>> result = lazyResolutionService
                .save(valueObject).toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<ProductWithUnResolvedProductReferences> createdCustomObject = result.get();
        assertThat(createdCustomObject.getKey()).isEqualTo("productKey1");
        assertThat(createdCustomObject.getContainer()).isEqualTo(testContainerKey);
    }

    @Test
    void fetch_shouldReturnCorrectCustomObject() {
        // preparation
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH,
                productType.toReference(), null, null, categoryReferencesWithIds,
                createRandomCategoryOrderHints(categoryReferencesWithIds));
        ProductWithUnResolvedProductReferences valueObject =
                new ProductWithUnResolvedProductReferences(productDraft,
                        null);
        lazyResolutionService
                .save(valueObject).toCompletableFuture().join();

        // test
        Optional<CustomObject<ProductWithUnResolvedProductReferences>> result = lazyResolutionService
                .fetch("productKey2").toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<ProductWithUnResolvedProductReferences> savedCustomObject = result.get();
        assertThat(savedCustomObject.getContainer()).isEqualTo(testContainerKey);
    }


    @Test
    void delete_shouldRemoveAndReturnCorrectCustomObject() {
        // preparation
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH,
                productType.toReference(), null, null, categoryReferencesWithIds,
                createRandomCategoryOrderHints(categoryReferencesWithIds));
        ProductWithUnResolvedProductReferences valueObject =
                new ProductWithUnResolvedProductReferences(productDraft,
                        null);

        lazyResolutionService
                .save(valueObject).toCompletableFuture().join();

        // test
        Optional<CustomObject<ProductWithUnResolvedProductReferences>> result = lazyResolutionService
                .delete("test-product-key-3").toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<ProductWithUnResolvedProductReferences> deletedCustomObject = result.get();
        assertThat(deletedCustomObject.getContainer()).isEqualTo(testContainerKey);

        Optional<CustomObject<ProductWithUnResolvedProductReferences>> nonExistingObj = lazyResolutionService
                .fetch("productKey2").toCompletableFuture().join();
        assertFalse(nonExistingObj.isPresent());
    }

}
