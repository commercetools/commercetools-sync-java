package com.commercetools.sync.integration.ctpprojectsource.products;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.expansion.ProductExpansionModel;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_NO_KEY_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ProductReferenceResolverIT {
    private static ProductType productTypeSource;
    private static ProductType noKeyProductTypeSource;

    private static Set<ResourceIdentifier<Category>> sourceCategoryResourcesWithIds;
    private static Set<ResourceIdentifier<Category>> sourceCategories;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target and source projects. Then creates custom types for both
     * CTP projects categories.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);

        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_SOURCE_CLIENT);

        createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        sourceCategories = createCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2))
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofIdOrKey(category.getId(), category.getKey(),
                Category.referenceTypeId())).collect(Collectors.toSet());
        sourceCategoryResourcesWithIds =
            sourceCategories.stream()
                            .map(categoryResourceIdentifier ->
                                ResourceIdentifier.<Category>ofId(categoryResourceIdentifier.getId(),
                                    Category.referenceTypeId()))
                            .collect(toSet());

        createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        createProductType(PRODUCT_TYPE_NO_KEY_RESOURCE_PATH, CTP_TARGET_CLIENT);

        productTypeSource = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);
        noKeyProductTypeSource = createProductType(PRODUCT_TYPE_NO_KEY_RESOURCE_PATH, CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes Products and Types from target CTP projects, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {
        deleteAllProducts(CTP_TARGET_CLIENT);
        deleteAllProducts(CTP_SOURCE_CLIENT);

        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                                                        .setErrorCallBack(
                                                                            (errorMessage, exception) -> {
                                                                                errorCallBackMessages
                                                                                    .add(errorMessage);
                                                                                errorCallBackExceptions
                                                                                    .add(exception);
                                                                            })
                                                                        .setWarningCallBack(warningMessage ->
                                                                            warningCallBackMessages
                                                                                .add(warningMessage))
                                                                        .build();
        productSync = new ProductSync(syncOptions);
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);
    }

    @Test
    public void sync_withNewProductWithExistingCategoryAndProductTypeReferences_ShouldCreateProduct() {
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            productTypeSource.toReference(), sourceCategoryResourcesWithIds,
            createRandomCategoryOrderHints(sourceCategories));
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

        final ProductQuery productQuery = ProductQuery.of().withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                                      .withExpansionPaths(ProductExpansionModel::productType)
                                                      .plusExpansionPaths(productProductExpansionModel ->
                                                          productProductExpansionModel.masterData().staged()
                                                                                      .categories());

        final List<Product> products = CTP_SOURCE_CLIENT.execute(productQuery)
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 1, 0, 0));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).hasSize(1);
        assertThat(warningCallBackMessages.get(0)).matches("ProductType with id: '.*' has no key"
            + " set. Keys are required for productType matching.");
    }

    @Test
    public void sync_withNewProductWithNoProductTypeKey_ShouldFailCreatingTheProduct() {
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            noKeyProductTypeSource.toReference(), sourceCategoryResourcesWithIds,
            createRandomCategoryOrderHints(sourceCategories));
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();

        final ProductQuery productQuery = ProductQuery.of().withLimit(SphereClientUtils.QUERY_MAX_LIMIT)
                                                      .withExpansionPaths(ProductExpansionModel::productType)
                                                      .plusExpansionPaths(productProductExpansionModel ->
                                                          productProductExpansionModel.masterData().staged()
                                                                                      .categories());

        final List<Product> products = CTP_SOURCE_CLIENT.execute(productQuery)
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 0, 1));

        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on ProductDraft with"
            + " key:'%s'. Reason: %s: Failed to resolve product type reference on ProductDraft with key:'%s'."
                + " Reason: Reference 'id' field value is blank (null/empty).",
            productDraft.getKey(), ReferenceResolutionException.class.getCanonicalName(), productDraft.getKey()));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
        assertThat(warningCallBackMessages).isEmpty();
    }
}
