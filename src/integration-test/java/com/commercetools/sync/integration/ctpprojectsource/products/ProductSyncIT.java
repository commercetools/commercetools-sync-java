package com.commercetools.sync.integration.ctpprojectsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.taxcategories.TaxCategory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.getDraftWithPriceChannelReferences;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.buildProductQuery;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;
import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ProductSyncIT {
    private static ProductType sourceProductType;
    private static ProductType targetProductType;

    private static TaxCategory sourceTaxCategory;
    private static TaxCategory targetTaxCategory;

    private static State sourceProductState;
    private static State targetProductState;

    private static Channel sourcePriceChannel;
    private static Channel targetPriceChannel;

    private static List<Reference<Category>> sourceCategoryReferencesWithIds;
    private static List<Reference<Category>> targetCategoryReferencesWithIds;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target and source projects. Then creates for both CTP projects price
     * channels, product types, tax categories, categories, custom types for categories and product states.
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


        final List<Category> targetCategories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        targetCategoryReferencesWithIds = getReferencesWithIds(targetCategories);
        final List<Category> sourceCategories = createCategories(CTP_SOURCE_CLIENT, getCategoryDrafts(null, 2));
        sourceCategoryReferencesWithIds = getReferencesWithIds(sourceCategories);


        targetProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        sourceProductType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_SOURCE_CLIENT);

        targetTaxCategory = createTaxCategory(CTP_TARGET_CLIENT);
        sourceTaxCategory = createTaxCategory(CTP_SOURCE_CLIENT);

        targetProductState = createState(CTP_TARGET_CLIENT, StateType.PRODUCT_STATE);
        sourceProductState = createState(CTP_SOURCE_CLIENT, StateType.PRODUCT_STATE);
    }

    /**
     * Deletes Products from the source and target CTP projects, clears the callback collections then it instantiates a
     * new {@link ProductSync} instance.
     */
    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        deleteAllProducts(CTP_SOURCE_CLIENT);
        final ProductSyncOptions syncOptions = buildSyncOptions();
        productSync = new ProductSync(syncOptions);
    }

    private void clearSyncTestCollections() {
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };
        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .setErrorCallBack(errorCallBack)
                                        .setWarningCallBack(warningCallBack)
                                        .build();
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        deleteProductSyncTestData(CTP_SOURCE_CLIENT);
    }

    @Test
    public void sync_withChangesOnly_ShouldUpdateProducts() {
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            targetCategoryReferencesWithIds, createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)).toCompletableFuture().join();

        final ProductDraft newProductDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            sourceProductType.toReference(), sourceTaxCategory.toReference(), sourceProductState.toReference(),
            sourceCategoryReferencesWithIds, createRandomCategoryOrderHints(sourceCategoryReferencesWithIds));
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraft)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withChangesOnlyAndUnPublish_ShouldUpdateProducts() {
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            targetCategoryReferencesWithIds, createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)).toCompletableFuture().join();

        final ProductDraft newProductDraft = createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            sourceProductType.toReference())
            .taxCategory(sourceTaxCategory)
            .state(sourceProductState)
            .categories(sourceCategoryReferencesWithIds)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(false).build();

        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraft)).toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withPriceChannels_ShouldUpdateProducts() {
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
            targetProductType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            targetCategoryReferencesWithIds, createRandomCategoryOrderHints(targetCategoryReferencesWithIds));

        final ProductDraft existingDraftWithPriceChannelReferences =
            getDraftWithPriceChannelReferences(existingProductDraft, targetPriceChannel.toReference());

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingDraftWithPriceChannelReferences))
                         .toCompletableFuture().join();

        final ProductDraft newProductDraft = createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
            sourceProductType.toReference())
            .taxCategory(sourceTaxCategory)
            .state(sourceProductState)
            .categories(sourceCategoryReferencesWithIds)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(false).build();

        final ProductDraft newDraftWithPriceChannelReferences =
            getDraftWithPriceChannelReferences(newProductDraft, sourcePriceChannel.toReference());

        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newDraftWithPriceChannelReferences))
                         .toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }
}
