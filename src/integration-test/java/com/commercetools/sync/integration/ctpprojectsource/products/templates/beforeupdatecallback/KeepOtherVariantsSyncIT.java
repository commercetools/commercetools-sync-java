package com.commercetools.sync.integration.ctpprojectsource.products.templates.beforeupdatecallback;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.products.templates.beforeupdatecallback.KeepOtherVariantsSync;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryPredicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_NO_VARS_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_WITH_VARS_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class KeepOtherVariantsSyncIT {

    private static ProductType productType;
    private Product oldProduct;
    private ProductSyncOptions syncOptions;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;


    /**
     * Delete all product related test data from target project. Then creates a productType for the products of the
     * target CTP project.
     */
    @BeforeAll
    static void setupAllTests() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * 1. Clears all sync collections used for test assertions.
     * 2. Deletes all products from target CTP project
     * 3. Creates an instance for {@link ProductSyncOptions} that will be used in the test.
     * 4. Creates a product in the target CTP project with 1 variant other than the master
     * variant.
     */
    @BeforeEach
    void setupPerTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = getProductSyncOptions();
        final ProductDraft productDraft =
            createProductDraftBuilder(PRODUCT_WITH_VARS_RESOURCE_PATH, productType.toReference())
                .build();
        oldProduct = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptions getProductSyncOptions() {
        final QuadConsumer<SyncException, Optional<ProductDraft>, Optional<Product>, List<UpdateAction<Product>>>
                errorCallBack = (exception, newResource, oldResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                };

        final TriConsumer<SyncException, Optional<ProductDraft>, Optional<Product>> warningCallBack =
            (exception, newResource, oldResource) -> warningCallBackMessages.add(exception.getMessage());

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .beforeUpdateCallback(KeepOtherVariantsSync::keepOtherVariants)
                                        .build();
    }

    @AfterAll
    static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_withRemovedVariants_shouldNotRemoveVariants() {
        final ProductDraft productDraft =
            createProductDraftBuilder(PRODUCT_NO_VARS_RESOURCE_PATH, referenceOfId(productType.getKey()))
                .build();

        // old product has 1 variant
        assertThat(oldProduct.getMasterData().getStaged().getVariants()).hasSize(1);
        // new product has no variants
        assertThat(productDraft.getVariants()).isEmpty();

        final ProductSync productSync = new ProductSync(syncOptions);

        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        // Assert that the variant wasn't removed
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
            .execute(ProductQuery.of()
                                 .withPredicates(QueryPredicate.of(format("key = \"%s\"", oldProduct.getKey()))))
            .toCompletableFuture().join().head();

        assertThat(productOptional).isNotEmpty();
        final Product fetchedProduct = productOptional.get();
        assertThat(fetchedProduct.getMasterData().getCurrent().getVariants()).hasSize(2);
    }
}
