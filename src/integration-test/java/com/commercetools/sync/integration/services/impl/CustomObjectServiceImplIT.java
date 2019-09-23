package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.commons.models.NonResolvedReferencesCustomObject;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.impl.CustomObjectServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
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
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomObjectServiceImplIT {

    private static final String containerKey = "test-container-key";
    private CustomObjectService customObjectService;
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


        customObjectService = new CustomObjectServiceImpl(productSyncOptions);
    }


    @Test
    void createOrUpdateCustomObject_createNewCustomObject() {
        // preparation
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
                productType.toReference(), null, null, categoryReferencesWithIds,
                createRandomCategoryOrderHints(categoryReferencesWithIds));
        NonResolvedReferencesCustomObject valueObject =
                new NonResolvedReferencesCustomObject("test-product-key", productDraft,
                        null);

        CustomObjectDraft<NonResolvedReferencesCustomObject> customObjectDraft =
                CustomObjectDraft.ofUnversionedUpsert(containerKey, "test-co-key", valueObject,
                        NonResolvedReferencesCustomObject.class);

        // test
        Optional<CustomObject<NonResolvedReferencesCustomObject>> result = customObjectService
                .createOrUpdateCustomObject(customObjectDraft).toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<NonResolvedReferencesCustomObject> savedCustomObject = result.get();
        assertThat(savedCustomObject.getKey()).isEqualTo("test-co-key");
        assertThat(savedCustomObject.getContainer()).isEqualTo(containerKey);
    }

    @Test
    void fetchCustomObject_shouldReturnCorrectCustomObject() {
        // preparation
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
                productType.toReference(), null, null, categoryReferencesWithIds,
                createRandomCategoryOrderHints(categoryReferencesWithIds));
        NonResolvedReferencesCustomObject valueObject =
                new NonResolvedReferencesCustomObject("test-product-key", productDraft,
                        null);

        CustomObjectDraft<NonResolvedReferencesCustomObject> customObjectDraft =
                CustomObjectDraft.ofUnversionedUpsert(containerKey, "test-co-key", valueObject,
                        NonResolvedReferencesCustomObject.class);
        customObjectService
                .createOrUpdateCustomObject(customObjectDraft).toCompletableFuture().join();

        // test
        Optional<CustomObject<NonResolvedReferencesCustomObject>> result = customObjectService
                .fetchCustomObject("test-co-key").toCompletableFuture().join();

        // assertions
        assertTrue(result.isPresent());
        CustomObject<NonResolvedReferencesCustomObject> savedCustomObject = result.get();
        assertThat(savedCustomObject.getContainer()).isEqualTo(containerKey);
    }

}
