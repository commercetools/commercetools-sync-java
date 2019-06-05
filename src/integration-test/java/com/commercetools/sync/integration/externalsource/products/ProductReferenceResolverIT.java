package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProductReferenceResolverIT {

    private static ProductType productType;
    private static List<Category> categories;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    @BeforeAll
    static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    @BeforeEach
    void setupPerTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptions getProductSyncOptions() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };

        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .build();
    }

    @AfterAll
    static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_withNewProductWithInvalidCategoryReferences_ShouldFailCreatingTheProduct() {
        // Create a list of category references that contains one valid and one invalid reference.
        final List<Reference<Category>> invalidCategoryReferences = new ArrayList<>();
        invalidCategoryReferences.add(Category.referenceOfId(categories.get(0).getId()));
        invalidCategoryReferences.add(Category.referenceOfId(null));

        // Create a product with the invalid category references. (i.e. not ready for reference resolution).
        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, referenceOfId(productType.getKey()), null,
                null, invalidCategoryReferences, createRandomCategoryOrderHints(invalidCategoryReferences));

        final ProductSync productSync = new ProductSync(getProductSyncOptions());
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackExceptions).hasSize(1);
        final Throwable exception = errorCallBackExceptions.get(0);
        assertThat(exception).isExactlyInstanceOf(ReferenceResolutionException.class)
                             .hasMessageContaining("Failed to resolve reference 'category' on ProductDraft with "
                                 + "key:'productKey1'")
                             .hasMessageContaining(format("Reason: %s", BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0))
            .contains("Failed to resolve references on ProductDraft with key:'productKey1'");
        assertThat(warningCallBackMessages).isEmpty();
    }
}
