package com.commercetools.sync.integration.ctpprojectsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.utils.SyncUtils.replaceProductsReferenceIdsWithKeys;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.getDraftWithPriceChannelReferences;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.getProductQuery;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ProductSyncIT {
    private static ProductType sourceProductType;
    private static ProductType targetProductType;
    private static Channel sourcePriceChannel;
    private static Channel targetPriceChannel;
    private static Set<ResourceIdentifier<Category>> sourceCategories;
    private static Set<ResourceIdentifier<Category>> targetCategories;
    private static Set<ResourceIdentifier<Category>> sourceCategoryResourcesWithIds;
    private static Set<ResourceIdentifier<Category>> targetCategoryResourcesWithIds;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target and source projects. Then creates custom types for both
     * CTP projects categories.
     *
     * <p>TODO: REFACTOR SETUP of key replacements.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);

        final ChannelDraft channelDraft1 = ChannelDraft.of(SUPPLY_CHANNEL_KEY_1);
        targetPriceChannel = CTP_TARGET_CLIENT
            .execute(ChannelCreateCommand.of(channelDraft1)).toCompletableFuture().join();
        sourcePriceChannel = CTP_SOURCE_CLIENT
            .execute(ChannelCreateCommand.of(channelDraft1)).toCompletableFuture().join();

        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_SOURCE_CLIENT);


        targetCategories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2))
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofIdOrKey(category.getId(), category.getKey(),
                Category.referenceTypeId())).collect(Collectors.toSet());
        sourceCategories = createCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2))
            .stream()
            .map(category -> ResourceIdentifier.<Category>ofIdOrKey(category.getId(), category.getKey(),
                Category.referenceTypeId())).collect(Collectors.toSet());

        targetCategoryResourcesWithIds =
            targetCategories.stream()
                            .map(categoryResourceIdentifier ->
                                ResourceIdentifier.<Category>ofId(categoryResourceIdentifier.getId(),
                                    Category.referenceTypeId()))
                            .collect(toSet());

        sourceCategoryResourcesWithIds =
            sourceCategories.stream()
                            .map(categoryResourceIdentifier ->
                                ResourceIdentifier.<Category>ofId(categoryResourceIdentifier.getId(),
                                    Category.referenceTypeId()))
                            .collect(toSet());

        targetProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        sourceProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes Products and Types from target CTP projects, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();

        deleteAllProducts(CTP_TARGET_CLIENT);
        deleteAllProducts(CTP_SOURCE_CLIENT);

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
    public void sync_withChangesOnly_ShouldUpdateProducts() {
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(), targetCategoryResourcesWithIds,
            createRandomCategoryOrderHints(targetCategories));
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)).toCompletableFuture().join();

        final ProductDraft newProductDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            sourceProductType.toReference(), sourceCategoryResourcesWithIds,
            createRandomCategoryOrderHints(sourceCategories));
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraft)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(getProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));

        Assertions.assertThat(errorCallBackMessages).isEmpty();
        Assertions.assertThat(errorCallBackExceptions).isEmpty();
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withChangesOnlyAndUnPublish_ShouldUpdateProducts() {
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(), targetCategoryResourcesWithIds,
            createRandomCategoryOrderHints(targetCategories));
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)).toCompletableFuture().join();

        final ProductDraft newProductDraft = createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            sourceProductType.toReference())
            .categories(sourceCategoryResourcesWithIds)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategories))
            .publish(false).build();

        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraft)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(getProductQuery())

        final List<Product> products = CTP_SOURCE_CLIENT.execute(getProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));

        Assertions.assertThat(errorCallBackMessages).isEmpty();
        Assertions.assertThat(errorCallBackExceptions).isEmpty();
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }
}
