package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getResourceIdentifiersOfIds;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getResourceIdentifiersOfKeysAndIds;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceCategoryOrderHintCategoryIdsWithKeys;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductDraftCategoryReferenceIdsWithKeys;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductDraftsCategoryReferenceIdsWithKeys;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ProductSyncIT {
    private static ProductType productType;
    private static TaxCategory targetTaxCategory;
    private static State targetProductState;
    private static Set<ResourceIdentifier<Category>> categoryResourceIdentifiers;
    private CategoryOrderHints categoryOrderHintsWithCategoryKeys;
    private ProductSyncOptions syncOptions;
    private Product product;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from target project. Then creates custom types for target CTP project
     * categories.
     *
     * <p>TODO: REFACTOR SETUP of key replacements.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        final List<Category> categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        categoryResourceIdentifiers = getResourceIdentifiersOfKeysAndIds(categories);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        targetTaxCategory = createTaxCategory(CTP_TARGET_CLIENT);
        targetProductState = createState(CTP_TARGET_CLIENT, StateType.PRODUCT_STATE);
    }

    /**
     * Deletes Products and Types from target CTP projects, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();

        final CategoryOrderHints randomCategoryOrderHints = createRandomCategoryOrderHints(categoryResourceIdentifiers);
        categoryOrderHintsWithCategoryKeys = replaceCategoryOrderHintCategoryIdsWithKeys(randomCategoryOrderHints,
            categoryResourceIdentifiers);

        final Set<ResourceIdentifier<Category>> categoryResourceIdentifiersWithIds =
                getResourceIdentifiersOfIds(categoryResourceIdentifiers);

        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, productType.toReference(),
            targetTaxCategory.toReference(), targetProductState.toReference(), categoryResourceIdentifiersWithIds,
            randomCategoryOrderHints);

        product = executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
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
    }

    @Test
    public void sync_withNewProduct_shouldCreateProduct() {
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 1, 0, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withNewProductWithExistingSlug_shouldNotCreateProduct() {
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .slug(product.getMasterData().getStaged().getSlug())
            .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 0, 1));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ErrorResponseException.class);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains(format("A duplicate value '\\\"%s\\\"' exists for field"
            + " 'slug.en' on", product.getMasterData().getStaged().getSlug().get(Locale.ENGLISH)));
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    @Ignore("TODO: Right now there is always a 'setPrice' update action GITHUB ISSUE: #101")
    public void sync_withEqualProduct_shouldNotUpdateProduct() {
        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                TaxCategory.referenceOfId(targetTaxCategory.getKey()), State.referenceOfId(targetProductState.getKey()),
                categoryResourceIdentifiers, categoryOrderHintsWithCategoryKeys);

        final ProductDraft productDraftWithCategoryKeys = replaceProductDraftCategoryReferenceIdsWithKeys(productDraft);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(singletonList(productDraftWithCategoryKeys)));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 0, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProduct() {
        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                TaxCategory.referenceOfId(targetTaxCategory.getKey()), State.referenceOfId(targetProductState.getKey()),
                categoryResourceIdentifiers, categoryOrderHintsWithCategoryKeys);

        final List<ProductDraft> productDraftWithKeysOnReferences =
            replaceProductDraftsCategoryReferenceIdsWithKeys(singletonList(productDraft));

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(productDraftWithKeysOnReferences));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test// TODO handle all retry cases
    public void sync_withChangedProductButConcurrentModificationException_shouldRetryAndUpdateProduct() {
        // Mock sphere client to return ConcurrentModification on the first update request.
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(ProductUpdateCommand.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final ProductSyncOptions spyOptions = ProductSyncOptionsBuilder.of(spyClient)
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

        final ProductSync spyProductSync = new ProductSync(spyOptions);

        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                TaxCategory.referenceOfId(targetTaxCategory.getKey()), State.referenceOfId(targetProductState.getKey()),
                categoryResourceIdentifiers, categoryOrderHintsWithCategoryKeys);
        final List<ProductDraft> productDraftWithKeysOnReferences =
            replaceProductDraftsCategoryReferenceIdsWithKeys(singletonList(productDraft));

        final ProductSyncStatistics syncStatistics =
                executeBlocking(spyProductSync.sync(productDraftWithKeysOnReferences));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 1, 0, 1, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withMultipleBatchSyncing_ShouldSync() {
        // Prepare existing products with keys: productKey1, productKey2, productKey3.
        final ProductDraft key2Draft = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            getResourceIdentifiersOfIds(categoryResourceIdentifiers),
            product.getMasterData().getStaged().getCategoryOrderHints());
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(key2Draft)));

        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toReference())
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug3"))
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").build())
            .taxCategory(TaxCategory.referenceOfId(targetTaxCategory.getId()))
            .build();
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(key3Draft)));


        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.reference(productType.getKey()), TaxCategory.referenceOfId(targetTaxCategory.getKey()),
            State.referenceOfId(targetProductState.getKey()),
            categoryResourceIdentifiers, categoryOrderHintsWithCategoryKeys);

        final List<ProductDraft> batch1 = new ArrayList<>();
        batch1.add(productDraft);

        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey4")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug4"))
            .masterVariant(ProductVariantDraftBuilder.of().key("v4").build())
            .build();

        final List<ProductDraft> batch2 = new ArrayList<>();
        batch2.add(key4Draft);

        final ProductDraft key3DraftNewSlug = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").build())
            .build();

        final List<ProductDraft> batch3 = new ArrayList<>();
        batch3.add(key3DraftNewSlug);

        final List<ProductDraft> batch1WithReferenceKeys =
            replaceProductDraftsCategoryReferenceIdsWithKeys(batch1);
        final List<ProductDraft> batch2WithReferenceKeys =
            replaceProductDraftsCategoryReferenceIdsWithKeys(batch2);
        final List<ProductDraft> batch3WithReferenceKeys =
            replaceProductDraftsCategoryReferenceIdsWithKeys(batch3);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(batch1WithReferenceKeys)
                                            .thenCompose(result -> productSync.sync(batch2WithReferenceKeys))
                                            .thenCompose(result -> productSync.sync(batch3WithReferenceKeys)));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 3, 1, 2, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withSingleBatchSyncing_ShouldSync() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()), TaxCategory.referenceOfId(targetTaxCategory.getKey()),
            State.referenceOfId(targetProductState.getKey()), categoryResourceIdentifiers,
            categoryOrderHintsWithCategoryKeys);

        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(TaxCategory.referenceOfId(targetTaxCategory.getKey()))
            .state(State.referenceOfId(targetProductState.getKey()))
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug3"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(TaxCategory.referenceOfId(targetTaxCategory.getKey()))
            .state(State.referenceOfId(targetProductState.getKey()))
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey4")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug4"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key5Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(TaxCategory.referenceOfId(targetTaxCategory.getKey()))
            .state(State.referenceOfId(targetProductState.getKey()))
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey5")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug5"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key6Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(TaxCategory.referenceOfId(targetTaxCategory.getKey()))
            .state(State.referenceOfId(targetProductState.getKey()))
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey6")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug6"))
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(key3Draft);
        batch.add(key4Draft);
        batch.add(key5Draft);
        batch.add(key6Draft);

        final List<ProductDraft> draftsWithReferenceKeys = replaceProductDraftsCategoryReferenceIdsWithKeys(batch);
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(draftsWithReferenceKeys));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 5, 4, 1, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withSameSlugInSingleBatch_ShouldNotSyncIt() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()), TaxCategory.referenceOfId(targetTaxCategory.getKey()),
            State.referenceOfId(targetProductState.getKey()), categoryResourceIdentifiers,
            categoryOrderHintsWithCategoryKeys);

        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey4")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key5Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey5")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final ProductDraft key6Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("productKey6")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(key3Draft);
        batch.add(key4Draft);
        batch.add(key5Draft);
        batch.add(key6Draft);

        final List<ProductDraft> draftsWithReferenceKeys = replaceProductDraftsCategoryReferenceIdsWithKeys(batch);
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(draftsWithReferenceKeys));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 5, 1, 1, 3));
        assertThat(errorCallBackExceptions).hasSize(3);
        errorCallBackExceptions
            .forEach(exception -> assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class));
        assertThat(errorCallBackMessages).hasSize(3);
        errorCallBackMessages.forEach(errorMessage -> assertThat(errorMessage)
            .contains(format("A duplicate value '\\\"%s\\\"' exists for field 'slug.en' on",
                key3Draft.getSlug().get(Locale.ENGLISH))));
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withADraftsWithBlankKeysInBatch_ShouldNotSyncItAndTriggerErrorCallBack() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.reference(productType.getKey()), TaxCategory.referenceOfId(targetTaxCategory.getKey()),
            State.referenceOfId(targetProductState.getKey()), categoryResourceIdentifiers,
            categoryOrderHintsWithCategoryKeys);
        final ProductDraft productDraftWithCategoryKeysOnReferences =
            replaceProductDraftsCategoryReferenceIdsWithKeys(Collections.singletonList(productDraft)).get(0);

        // Draft with null key
        final ProductDraft key3Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.reference(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .productType(ProductType.referenceOfId(productType.getKey()))
            .build();

        // Draft with empty key
        final ProductDraft key4Draft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.reference(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key("")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .productType(ProductType.referenceOfId(productType.getKey()))
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraftWithCategoryKeysOnReferences);
        batch.add(key3Draft);
        batch.add(key4Draft);

        final List<ProductDraft> draftsWithReferenceKeys = replaceProductDraftsCategoryReferenceIdsWithKeys(batch);
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(draftsWithReferenceKeys));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 3, 0, 1, 2));
        assertThat(errorCallBackExceptions).hasSize(2);
        assertThat(errorCallBackMessages).hasSize(2);
        assertThat(errorCallBackMessages.get(0))
            .isEqualToIgnoringCase(format("ProductDraft with name: %s doesn't have a key.", key3Draft.getName()));
        assertThat(errorCallBackMessages.get(1))
            .isEqualToIgnoringCase(format("ProductDraft with name: %s doesn't have a key.", key4Draft.getName()));
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withANullDraftInBatch_ShouldNotSyncItAndTriggerErrorCallBack() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.reference(productType.getKey()), null, null,
            categoryResourceIdentifiers, categoryOrderHintsWithCategoryKeys);

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(null);

        final List<ProductDraft> draftsWithReferenceKeys = replaceProductDraftsCategoryReferenceIdsWithKeys(batch);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(draftsWithReferenceKeys));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 2, 0, 1, 1));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualToIgnoringCase("ProductDraft is null.");
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withSameDraftsWithChangesInBatch_ShouldRetryUpdateBecauseOfConcurrentModificationExceptions() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.reference(productType.getKey()), null, null,
            categoryResourceIdentifiers, categoryOrderHintsWithCategoryKeys);

        // Draft with same key
        final ProductDraft draftWithSameKey = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.reference(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
            .key(productDraft.getKey())
            .masterVariant(ProductVariantDraftBuilder.of(product.getMasterData().getStaged().getMasterVariant())
                                                     .build())
            .build();

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(draftWithSameKey);

        final List<ProductDraft> draftsWithReferenceKeys = replaceProductDraftsCategoryReferenceIdsWithKeys(batch);
        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(draftsWithReferenceKeys));

        assertThat(syncStatistics.getReportMessage())
            .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                + " failed to sync).", 2, 0, 2, 0));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }
}
