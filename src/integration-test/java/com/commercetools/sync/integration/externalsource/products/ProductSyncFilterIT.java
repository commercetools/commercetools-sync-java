package com.commercetools.sync.integration.externalsource.products;

import com.commercetools.sync.commons.utils.SyncUtils;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.UpdateFilter;
import com.commercetools.sync.products.UpdateFilterType;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.ResourceIdentifier;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.*;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ProductSyncMockUtils.*;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductSyncFilterIT {

        private static ProductType productType;
        private static Set<ResourceIdentifier<Category>> categoryResourceIdentifiers;
        private ProductSyncOptionsBuilder syncOptionsBuilder;
    private List<String> errorCallBackMessages;
        private List<String> warningCallBackMessages;
        private List<Throwable> errorCallBackExceptions;
        private List<UpdateAction<Product>> updateActionsFromSync;

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
            categoryResourceIdentifiers = createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2))
                    .stream()
                    .map(category -> ResourceIdentifier.<Category>ofIdOrKey(category.getId(), category.getKey(),
                            Category.referenceTypeId())).collect(toSet());
            productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
        }

        /**
         * Deletes Products and Types from target CTP projects, then it populates target CTP project with product test
         * data.
         */
        @Before
        public void setupTest() {
            updateActionsFromSync = new ArrayList<>();
            errorCallBackMessages = new ArrayList<>();
            errorCallBackExceptions = new ArrayList<>();
            warningCallBackMessages = new ArrayList<>();
            deleteAllProducts(CTP_TARGET_CLIENT);

            syncOptionsBuilder = ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
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
                    .setUpdateActionsFilterCallBack(updateActions -> {
                        updateActionsFromSync.addAll(updateActions);
                        return updateActions;
                    });

            final Set<ResourceIdentifier<Category>> categoryResourcesWithIds = categoryResourceIdentifiers
                    .stream()
                    .map(categoryResourceIdentifier ->
                            ResourceIdentifier.<Category>ofId(categoryResourceIdentifier.getId(), Category.referenceTypeId()))
                    .collect(toSet());
            final ProductDraft productDraft = createProductDraft(PRODUCT_KEY_1_RESOURCE_PATH, productType.toReference(),
                    categoryResourcesWithIds,  createRandomCategoryOrderHints(categoryResourceIdentifiers));
            CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)).toCompletableFuture().join();
        }

        @AfterClass
        public static void tearDown() {
            deleteProductSyncTestData(CTP_TARGET_CLIENT);
        }

    @Test
    public void sync_withChangedProductBlackListingCategories_shouldUpdateProductWithoutCategories() {
        final ProductDraft productDraft =
                createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                        categoryResourceIdentifiers, createRandomCategoryOrderHints(categoryResourceIdentifiers));

        final ProductDraft productDraftWithKeysOnReferences =
                SyncUtils.replaceProductDraftsCategoryReferenceIdsWithKeys(Collections.singletonList(productDraft)).get(0);

        final ProductSyncOptions syncOptions = syncOptionsBuilder
                .setSyncFilter(Collections.singletonList(UpdateFilter.CATEGORIES), UpdateFilterType.BLACKLIST).build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                productSync.sync(singletonList(productDraftWithKeysOnReferences)).toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
                .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                        + " failed to sync).", 1, 0, 1, 0));
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
                createProductDraft(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, ProductType.referenceOfId(productType.getKey()),
                        categoryResourceIdentifiers, createRandomCategoryOrderHints(categoryResourceIdentifiers));

        final ProductDraft productDraftWithKeysOnReferences =
                SyncUtils.replaceProductDraftsCategoryReferenceIdsWithKeys(Collections.singletonList(productDraft)).get(0);

        final ProductSyncOptions syncOptions = syncOptionsBuilder
                .setSyncFilter(Collections.singletonList(UpdateFilter.NAME), UpdateFilterType.WHITELIST).build();

        final ProductSync productSync = new ProductSync(syncOptions);
        final ProductSyncStatistics syncStatistics =
                productSync.sync(singletonList(productDraftWithKeysOnReferences)).toCompletableFuture().join();
        assertThat(syncStatistics.getReportMessage())
                .isEqualTo(format("Summary: %d products were processed in total (%d created, %d updated and %d products"
                        + " failed to sync).", 1, 0, 1, 0));
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
