package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.producttypes.ProductType;
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
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithKeys;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.createProductType;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ActionGroup.CATEGORIES;
import static com.commercetools.sync.products.ActionGroup.NAME;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraft;
import static com.commercetools.sync.products.ProductSyncMockUtils.createRandomCategoryOrderHints;
import static com.commercetools.sync.products.SyncFilter.ofBlackList;
import static com.commercetools.sync.products.SyncFilter.ofWhiteList;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.producttypes.ProductType.referenceOfId;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncFilterIT {

    private static ProductType productType;
    private static List<Reference<Category>> categoryReferencesWithIds;
    private static List<Reference<Category>> categoryReferencesWithKeys;
    private ProductSyncOptionsBuilder syncOptionsBuilder;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;
    private List<UpdateAction<Product>> updateActionsFromSync;

    /**
     * Delete all product related test data from target project. Then create custom types for the categories and a
     * productType for the products of the target CTP project.
     */
    @BeforeClass
    public static void setupAllTests() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
        createCategoriesCustomType(OLD_CATEGORY_CUSTOM_TYPE_KEY, Locale.ENGLISH,
                OLD_CATEGORY_CUSTOM_TYPE_NAME, CTP_TARGET_CLIENT);
        final List<Category> categories = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
        categoryReferencesWithIds = getReferencesWithIds(categories);
        categoryReferencesWithKeys = getReferencesWithKeys(categories);
        productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    }

    /**
     * 1. Deletes all products from target CTP project
     * 2. Clears all sync collections used for test assertions.
     * 3. Creates an instance for {@link ProductSyncOptionsBuilder} that will be used in the tests to build
     * {@link ProductSyncOptions} instances.
     * 4. Create a product in the target CTP project.
     */
    @Before
    public void setupPerTest() {
        clearSyncTestCollections();
        deleteAllProducts(CTP_TARGET_CLIENT);
        syncOptionsBuilder = getProductSyncOptionsBuilder();
        final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, productType.toReference(),
            null, null, categoryReferencesWithIds, createRandomCategoryOrderHints(categoryReferencesWithIds));
        executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
    }

    private void clearSyncTestCollections() {
        updateActionsFromSync = new ArrayList<>();
        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();
    }

    private ProductSyncOptionsBuilder getProductSyncOptionsBuilder() {
        final BiConsumer<String, Throwable> errorCallBack = (errorMessage, exception) -> {
            errorCallBackMessages.add(errorMessage);
            errorCallBackExceptions.add(exception);
        };

        final Consumer<String> warningCallBack = warningMessage -> warningCallBackMessages.add(warningMessage);

        final TriFunction<List<UpdateAction<Product>>, ProductDraft, Product, List<UpdateAction<Product>>>
            actionsCallBack = (updateActions, newDraft, oldProduct) -> {
                updateActionsFromSync.addAll(updateActions);
                return updateActions;
            };

        return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                                        .errorCallback(errorCallBack)
                                        .warningCallback(warningCallBack)
                                        .beforeUpdateCallback(actionsCallBack);
    }

    @AfterClass
    public static void tearDown() {
        deleteProductSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    public void sync_withChangedProductBlackListingCategories_shouldUpdateProductWithoutCategories() {
        final ProductDraft productDraft =
                createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, referenceOfId(productType.getKey()), null,
                    null, categoryReferencesWithKeys, createRandomCategoryOrderHints(categoryReferencesWithKeys));

        final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofBlackList(CATEGORIES))
                                                                 .build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);

        assertThat(updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof RemoveFromCategory)).isTrue();
        assertThat(updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof AddToCategory)).isTrue();
        assertThat(updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof SetCategoryOrderHint)).isTrue();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    public void sync_withChangedProductWhiteListingName_shouldOnlyUpdateProductName() {
        final ProductDraft productDraft =
                createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, referenceOfId(productType.getKey()), null,
                    null, categoryReferencesWithKeys, createRandomCategoryOrderHints(categoryReferencesWithKeys));

        final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofWhiteList(NAME)).build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                executeBlocking(productSync.sync(singletonList(productDraft)));

        assertThat(syncStatistics.getProcessed()).isEqualTo(1);
        assertThat(syncStatistics.getCreated()).isEqualTo(0);
        assertThat(syncStatistics.getUpdated()).isEqualTo(1);
        assertThat(syncStatistics.getFailed()).isEqualTo(0);

        assertThat(updateActionsFromSync).hasSize(1);
        final UpdateAction<Product> updateAction = updateActionsFromSync.get(0);
        assertThat(updateAction.getAction()).isEqualTo("changeName");
        assertThat(updateAction).isExactlyInstanceOf(ChangeName.class);
        assertThat(((ChangeName) updateAction).getName()).isEqualTo(LocalizedString.of(Locale.ENGLISH, "new name"));
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }
}
