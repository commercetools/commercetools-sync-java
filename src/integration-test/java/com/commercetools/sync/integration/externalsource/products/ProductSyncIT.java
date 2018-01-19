package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.CategoryOrderHints;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithKeys;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.replaceCategoryOrderHintCategoryIdsWithKeys;
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
    private static List<Reference<Category>> categoryReferencesWithIds;
    private static List<Reference<Category>> categoryReferencesWithKeys;
    private static CategoryOrderHints categoryOrderHintsWithIds;
    private static CategoryOrderHints categoryOrderHintsWithKeys;
    private ProductSyncOptions syncOptions;
    private Product product;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Delete all product related test data from the target project. Then creates for the target CTP project price
     * a product type, a tax category, 2 categories, custom types for the categories and a product state.
     */
    @BeforeClass
    public static void setup() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
            OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);


        final List<CategoryDraft> categoryDrafts = getCategoryDrafts(null, 2);
        final List<Category> categories = createCategories(CTP_TARGET_CLIENT, categoryDrafts);
        categoryReferencesWithIds = getReferencesWithIds(categories);
        categoryReferencesWithKeys = getReferencesWithKeys(categories);
        categoryOrderHintsWithIds = createRandomCategoryOrderHints(categoryReferencesWithIds);
        categoryOrderHintsWithKeys = replaceCategoryOrderHintCategoryIdsWithKeys(categoryOrderHintsWithIds,
            categories);

        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        targetTaxCategory = createTaxCategory(CTP_TARGET_CLIENT);
        targetProductState = createState(CTP_TARGET_CLIENT, StateType.PRODUCT_STATE);
    }

    /**
     * Deletes Products and Types from the target CTP project, then it populates target CTP project with product test
     * data.
     */
    @Before
    public void setupTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptions = buildSyncOptions();

        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, productType.toReference(),
            targetTaxCategory.toReference(), targetProductState.toReference(), categoryReferencesWithIds,
            categoryOrderHintsWithIds);

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
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
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

        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withNewProductAndBeforeCreateCallback_shouldCreateProduct() {
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
                ProductType.referenceOfId(productType.getKey()))
                .taxCategory(null)
                .state(null)
                .build();

        final String keyPrefix = "callback_";
        final ProductSyncOptions options = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                .errorCallback((errorMessage, exception) -> {
                    errorCallBackMessages.add(errorMessage);
                    errorCallBackExceptions.add(exception);
                })
                .warningCallback(warningMessage -> warningCallBackMessages.add(warningMessage))
                .beforeCreateCallback(draft -> prefixDraftKey(draft, keyPrefix))
                .build();

        final ProductSync productSync = new ProductSync(options);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        //Query for a product with key prefixed with "callback_" added by the callback

        final String keyWithCallbackPrefix = format("%s%s", keyPrefix, productDraft.getKey());
        final Optional<Product> productOptional = CTP_TARGET_CLIENT
                .execute(ProductQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", keyWithCallbackPrefix))))
                .toCompletableFuture().join().head();

        assertThat(productOptional).isNotEmpty();
        final Product fetchedProduct = productOptional.get();
        assertThat(fetchedProduct.getKey()).isEqualTo(keyWithCallbackPrefix);
        assertThat(fetchedProduct.getMasterData().getCurrent().getName()).isEqualTo(productDraft.getName());
    }

    @Nonnull
    private static ProductDraft prefixDraftKey(@Nonnull final ProductDraft productDraft, @Nonnull final String prefix) {
        final String newKey = format("%s%s", prefix, productDraft.getKey());
        return ProductDraftBuilder.of(productDraft)
                                  .key(newKey)
                                  .build();
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

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
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
                categoryReferencesWithIds, categoryOrderHintsWithKeys);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withChangedProduct_shouldUpdateProduct() {
        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                TaxCategory.referenceOfId(targetTaxCategory.getKey()), State.referenceOfId(targetProductState.getKey()),
                categoryReferencesWithKeys, categoryOrderHintsWithKeys);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
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

        final ProductSync spyProductSync = new ProductSync(spyOptions);

        final ProductDraft productDraft =
            createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                TaxCategory.referenceOfId(targetTaxCategory.getKey()), State.referenceOfId(targetProductState.getKey()),
                categoryReferencesWithKeys, categoryOrderHintsWithKeys);

        final ProductSyncStatistics syncStatistics =
                executeBlocking(spyProductSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    @SuppressWarnings("PMD")
    public void sync_10000NewProducts_ShouldCreateProducts() {
        final List<ProductDraft> batch1 = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            batch1.add(
                createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
                    ProductType.referenceOfId(productType.getKey()))
                    .taxCategory(null)
                    .state(null)
                    .categories(new ArrayList<>())
                    .categoryOrderHints(CategoryOrderHints.of(new HashMap<>()))
                    .key("productKey" + i)
                    .slug(LocalizedString.of(Locale.ENGLISH, "slug" + i))
                    .masterVariant(ProductVariantDraftBuilder.of().key("newV" + i).build())
                    .build());
        }


        final ProductSync productSync = new ProductSync(syncOptions);

        final long now = System.currentTimeMillis();
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(batch1));
        final long later = System.currentTimeMillis();
        final long totalTime = later - now;

        System.out.println("Syncing 10000 products (all creates) took " + totalTime + " milliseconds.");
        assertThat(syncStatistics).hasValues(10000, 10000, 0, 0);

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withSingleBatchSyncing_ShouldSync() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()), TaxCategory.referenceOfId(targetTaxCategory.getKey()),
            State.referenceOfId(targetProductState.getKey()), categoryReferencesWithKeys,
            categoryOrderHintsWithKeys);

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

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(batch));

        assertThat(syncStatistics).hasValues(5, 4, 1, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withSameSlugInSingleBatch_ShouldNotSyncIt() {
        // Prepare batches from external source
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()), TaxCategory.referenceOfId(targetTaxCategory.getKey()),
            State.referenceOfId(targetProductState.getKey()), categoryReferencesWithKeys,
            categoryOrderHintsWithKeys);

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

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(batch));

        assertThat(syncStatistics).hasValues(5, 1, 1, 3);
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
            State.referenceOfId(targetProductState.getKey()), categoryReferencesWithKeys,
            categoryOrderHintsWithKeys);

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
        batch.add(productDraft);
        batch.add(key3Draft);
        batch.add(key4Draft);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(batch));

        assertThat(syncStatistics).hasValues(3, 0, 1, 2);
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
            categoryReferencesWithKeys, categoryOrderHintsWithKeys);

        final List<ProductDraft> batch = new ArrayList<>();
        batch.add(productDraft);
        batch.add(null);

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(batch));

        assertThat(syncStatistics).hasValues(2, 0, 1, 1);
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
            categoryReferencesWithKeys, categoryOrderHintsWithKeys);

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

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics = executeBlocking(productSync.sync(batch));

        assertThat(syncStatistics).hasValues(2, 0, 2, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withProductBundle_shouldCreateProductReferencingExistingProduct() {
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId(productType.getKey()))
            .taxCategory(null)
            .state(null)
            .build();

        // Creating the attribute draft with the product reference
        final AttributeDraft productReferenceAttribute =
            AttributeDraft.of("product-reference", Reference.of(Product.referenceTypeId(), product.getKey()));

        // Creating the product variant draft with the product reference attribute
        final ProductVariantDraft draftMasterVariant = productDraft.getMasterVariant();
        assertThat(draftMasterVariant).isNotNull();
        final List<AttributeDraft> attributes = draftMasterVariant.getAttributes();
        attributes.add(productReferenceAttribute);
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of(draftMasterVariant)
                                                                            .attributes(attributes)
                                                                            .build();

        final ProductDraft productDraftWithProductReference = ProductDraftBuilder.of(productDraft)
                                                                                 .masterVariant(masterVariant)
                                                                                 .build();


        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
            executeBlocking(productSync.sync(singletonList(productDraftWithProductReference)));

        assertThat(syncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }
}
