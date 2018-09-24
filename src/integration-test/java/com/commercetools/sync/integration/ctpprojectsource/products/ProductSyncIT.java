package com.commercetools.sync.integration.ctpprojectsource.products;

import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.SyncFilter;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import io.sphere.sdk.products.queries.ProductByKeyGet;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.createPricesCustomType;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.getDraftWithPriceReferences;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.createState;
import static com.commercetools.sync.integration.commons.utils.TaxCategoryITUtils.createTaxCategory;
import static com.commercetools.sync.integration.inventories.utils.InventoryITUtils.SUPPLY_CHANNEL_KEY_1;
import static com.commercetools.sync.products.ActionGroup.ATTRIBUTES;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceSetAttributeDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.getProductReferenceWithId;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.buildProductQuery;
import static com.commercetools.sync.products.utils.ProductReferenceReplacementUtils.replaceProductsReferenceIdsWithKeys;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncIT {
    private static ProductType sourceProductType;
    private static ProductType targetProductType;

    private static TaxCategory sourceTaxCategory;
    private static TaxCategory targetTaxCategory;

    private static State sourceProductState;
    private static State targetProductState;

    private static Channel sourcePriceChannel;
    private static Channel targetPriceChannel;

    private static Type sourcePriceCustomType;
    private static Type targetPriceCustomType;

    private static List<Reference<Category>> sourceCategoryReferencesWithIds;
    private static List<Reference<Category>> targetCategoryReferencesWithIds;
    private ProductSync productSync;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<UpdateAction<Product>> updateActions;
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

        targetPriceCustomType = createPricesCustomType("pricesCustomTypeKey", ENGLISH, "pricesCustomTypeName",
            CTP_TARGET_CLIENT);
        sourcePriceCustomType = createPricesCustomType("pricesCustomTypeKey", ENGLISH, "pricesCustomTypeName",
            CTP_SOURCE_CLIENT);

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
        updateActions = new ArrayList<>();
    }

    private ProductSyncOptions buildSyncOptions() {
        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(this::errorCallback)
                                        .warningCallback(warningCallBackMessages::add)
                                        .beforeUpdateCallback(this::beforeUpdateCallback)
                                        .build();
    }

    private void errorCallback(@Nonnull final String errorMessage, @Nullable final Throwable exception) {
        errorCallBackMessages.add(errorMessage);
        errorCallBackExceptions.add(exception);
    }

    private List<UpdateAction<Product>> beforeUpdateCallback(@Nonnull final List<UpdateAction<Product>> updateActions,
                                                             @Nonnull final ProductDraft newProductDraft,
                                                             @Nonnull final Product oldProduct) {
        this.updateActions.addAll(updateActions);
        return updateActions;
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

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
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

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withPriceReferences_ShouldUpdateProducts() {
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
            targetProductType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            targetCategoryReferencesWithIds, createRandomCategoryOrderHints(targetCategoryReferencesWithIds));

        final ProductDraft existingDraftWithPriceReferences =
            getDraftWithPriceReferences(existingProductDraft, targetPriceChannel.toReference(),
                CustomFieldsDraft.ofTypeKeyAndJson(targetPriceCustomType.getKey(), emptyMap()));

        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingDraftWithPriceReferences))
                         .toCompletableFuture().join();

        final ProductDraft newProductDraft = createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_WITH_PRICES_RESOURCE_PATH,
            sourceProductType.toReference())
            .taxCategory(sourceTaxCategory)
            .state(sourceProductState)
            .categories(sourceCategoryReferencesWithIds)
            .categoryOrderHints(createRandomCategoryOrderHints(sourceCategoryReferencesWithIds))
            .publish(false).build();

        final ProductDraft newDraftWithPriceReferences =
            getDraftWithPriceReferences(newProductDraft, sourcePriceChannel.toReference(),
                CustomFieldsDraft.ofTypeKeyAndJson(sourcePriceCustomType.getKey(), emptyMap()));

        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newDraftWithPriceReferences))
                         .toCompletableFuture().join();

        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();

        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);

        final ProductSyncStatistics syncStatistics =  productSync.sync(productDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withProductTypeReference_ShouldUpdateProducts() {
        // Preparation
        // Create custom options with whitelisting and action filter callback..
        final ProductSyncOptions customSyncOptions =
            ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                     .errorCallback(this::errorCallback)
                                     .warningCallback(warningCallBackMessages::add)
                                     .beforeUpdateCallback(this::beforeUpdateCallback)
                                     .syncFilter(SyncFilter.ofWhiteList(ATTRIBUTES))
                                     .build();
        final ProductSync customSync = new ProductSync(customSyncOptions);

        // Create 3 existing products in target project with keys (productKey1, productKey2 and productKey3)
        final ProductDraft existingProductDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH,
            targetProductType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            targetCategoryReferencesWithIds, createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft)).toCompletableFuture().join();

        final ProductDraft existingProductDraft2 = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH,
            targetProductType.toReference(), targetTaxCategory.toReference(), targetProductState.toReference(),
            targetCategoryReferencesWithIds, createRandomCategoryOrderHints(targetCategoryReferencesWithIds));
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft2))
                         .toCompletableFuture().join();

        final ProductDraft existingProductDraft3 = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            targetProductType.toReference())
            .slug(LocalizedString.ofEnglish("newSlug3"))
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").sku("s3").build())
            .taxCategory(null)
            .state(null)
            .categories(Collections.emptySet())
            .categoryOrderHints(null)
            .build();
        CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(existingProductDraft3)).toCompletableFuture().join();

        // Create 2 existing products in source project with keys (productKey2 and productKey3)
        final ProductDraft newProductDraft2 = createProductDraft(PRODUCT_KEY_2_RESOURCE_PATH,
            sourceProductType.toReference(), sourceTaxCategory.toReference(), sourceProductState.toReference(),
            sourceCategoryReferencesWithIds, createRandomCategoryOrderHints(sourceCategoryReferencesWithIds));
        final Product product2 = CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraft2))
                                                  .toCompletableFuture().join();
        final ProductDraft newProductDraft3 = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            sourceProductType.toReference())
            .slug(LocalizedString.ofEnglish("newSlug3"))
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").sku("s3").build())
            .taxCategory(null)
            .state(null)
            .categories(Collections.emptySet())
            .categoryOrderHints(null)
            .build();
        final Product product3 = CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraft3))
                                                  .toCompletableFuture().join();


        // Create existing product with productKey1 in source project that has references to products with keys
        // (productKey2 and productKey3).

        final ObjectNode productReferenceValue1 = getProductReferenceWithId(product2.getId());
        final ObjectNode productReferenceValue2 = getProductReferenceWithId(product3.getId());

        final AttributeDraft productRefAttr = AttributeDraft.of("product-reference", productReferenceValue1);
        final AttributeDraft productSetRefAttr =
            getProductReferenceSetAttributeDraft("product-reference-set", productReferenceValue1,
                productReferenceValue2);
        final List<AttributeDraft> attributeDrafts = Arrays.asList(productRefAttr, productSetRefAttr);

        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of()
                                                                            .key("v1")
                                                                            .sku("s1")
                                                                            .attributes(attributeDrafts).build();

        final ProductDraft newProductDraftWithProductReference =
            createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, sourceProductType.toReference())
                .masterVariant(masterVariant)
                .taxCategory(sourceTaxCategory.toReference())
                .state(sourceProductState.toReference())
                .categories(Collections.emptySet())
                .categoryOrderHints(null)
                .build();
        CTP_SOURCE_CLIENT.execute(ProductCreateCommand.of(newProductDraftWithProductReference))
                         .toCompletableFuture().join();


        // Test
        final List<Product> products = CTP_SOURCE_CLIENT.execute(buildProductQuery())
                                                        .toCompletableFuture().join().getResults();
        final List<ProductDraft> productDrafts = replaceProductsReferenceIdsWithKeys(products);
        final ProductSyncStatistics syncStatistics =  customSync.sync(productDrafts).toCompletableFuture().join();


        // Assertion
        assertThat(syncStatistics).hasValues(3, 0, 1, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();

        final Product targetProduct2 = CTP_TARGET_CLIENT.execute(ProductByKeyGet.of("productKey2"))
                                                        .toCompletableFuture()
                                                        .join();

        final Product targetProduct3 = CTP_TARGET_CLIENT.execute(ProductByKeyGet.of("productKey3"))
                                                        .toCompletableFuture()
                                                        .join();

        final ObjectNode targetProductReferenceValue2 = getProductReferenceWithId(targetProduct2.getId());
        final ObjectNode targetProductReferenceValue3 = getProductReferenceWithId(targetProduct3.getId());

        final AttributeDraft targetProductRefAttr =
            AttributeDraft.of("product-reference", targetProductReferenceValue2);
        final AttributeDraft targetProductSetRefAttr =
            getProductReferenceSetAttributeDraft("product-reference-set", targetProductReferenceValue2,
                targetProductReferenceValue3);

        assertThat(updateActions).containsExactlyInAnyOrder(
            SetAttributeInAllVariants.ofUnsetAttribute("priceInfo", true),
            SetAttributeInAllVariants.ofUnsetAttribute("size", true),
            SetAttributeInAllVariants.ofUnsetAttribute("rinderrasse", true),
            SetAttributeInAllVariants.ofUnsetAttribute("herkunft", true),
            SetAttributeInAllVariants.ofUnsetAttribute("teilstueck", true),
            SetAttributeInAllVariants.ofUnsetAttribute("fuetterung", true),
            SetAttributeInAllVariants.ofUnsetAttribute("reifung", true),
            SetAttributeInAllVariants.ofUnsetAttribute("haltbarkeit", true),
            SetAttributeInAllVariants.ofUnsetAttribute("verpackung", true),
            SetAttributeInAllVariants.ofUnsetAttribute("anlieferung", true),
            SetAttributeInAllVariants.ofUnsetAttribute("zubereitung", true),
            SetAttribute.ofUnsetAttribute(1, "localisedText", true),
            SetAttributeInAllVariants.of(targetProductRefAttr.getName(), targetProductRefAttr.getValue(), true),
            SetAttributeInAllVariants.of(targetProductSetRefAttr.getName(), targetProductSetRefAttr.getValue(), true)
        );

    }
}
