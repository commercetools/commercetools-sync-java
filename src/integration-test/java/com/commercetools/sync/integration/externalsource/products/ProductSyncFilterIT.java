package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.products.ActionGroup.CATEGORIES;
import static com.commercetools.sync.products.ActionGroup.NAME;
import static com.commercetools.sync.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.products.SyncFilter.ofBlackList;
import static com.commercetools.sync.products.SyncFilter.ofWhiteList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.product.ProductAddToCategoryAction;
import com.commercetools.api.models.product.ProductChangeNameAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductPublishActionBuilder;
import com.commercetools.api.models.product.ProductRemoveFromCategoryAction;
import com.commercetools.api.models.product.ProductSetCategoryOrderHintAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncFilterIT {

  private static ProductType productType;
  private static List<CategoryReference> categoryReferencesWithIds;
  private static List<CategoryResourceIdentifier> categoryResourceIdentifiersWithKeys;
  private ProductSyncOptionsBuilder syncOptionsBuilder;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<ProductUpdateAction> updateActionsFromSync;

  /**
   * Delete all product related test data from target project. Then create custom types for the
   * categories and a productType for the products of the target CTP project.
   */
  @BeforeAll
  static void setupAllTests() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    final List<Category> categories =
        ensureCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2, true));
    categoryReferencesWithIds = getReferencesWithIds(categories);
    categoryResourceIdentifiersWithKeys = getResourceIdentifiersWithKeys(categories);
    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /**
   * 1. Deletes all products from target CTP project 2. Clears all sync collections used for test
   * assertions. 3. Creates an instance for {@link
   * com.commercetools.sync.products.ProductSyncOptionsBuilder} that will be used in the tests to
   * build {@link com.commercetools.sync.products.ProductSyncOptions} instances. 4. Create a product
   * in the target CTP project.
   */
  @BeforeEach
  void setupPerTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    syncOptionsBuilder = getProductSyncOptionsBuilder();
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference(),
            null,
            null,
            categoryReferencesWithIds,
            createRandomCategoryOrderHints(categoryReferencesWithIds));
    CTP_TARGET_CLIENT.products().create(productDraft).executeBlocking();
  }

  private void clearSyncTestCollections() {
    updateActionsFromSync = new ArrayList<>();
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  private ProductSyncOptionsBuilder getProductSyncOptionsBuilder() {
    final QuadConsumer<
            SyncException,
            Optional<ProductDraft>,
            Optional<ProductProjection>,
            List<ProductUpdateAction>>
        errorCallBack =
            (exception, newResource, oldResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            };
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    final TriFunction<
            List<ProductUpdateAction>, ProductDraft, ProductProjection, List<ProductUpdateAction>>
        actionsCallBack =
            (updateActions, newDraft, oldProduct) -> {
              updateActionsFromSync.addAll(updateActions);
              return updateActions;
            };

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(errorCallBack)
        .warningCallback(warningCallBack)
        .beforeUpdateCallback(actionsCallBack);
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withChangedProductBlackListingCategories_shouldUpdateProductWithoutCategories() {
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build(),
            null,
            null,
            categoryResourceIdentifiersWithKeys,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

    final ProductSyncOptions syncOptions =
        syncOptionsBuilder.syncFilter(ofBlackList(CATEGORIES)).build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(
            updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof ProductRemoveFromCategoryAction))
        .isTrue();
    assertThat(
            updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof ProductAddToCategoryAction))
        .isTrue();
    assertThat(
            updateActionsFromSync.stream()
                .noneMatch(
                    updateAction -> updateAction instanceof ProductSetCategoryOrderHintAction))
        .isTrue();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withChangedProductWhiteListingName_shouldOnlyUpdateProductName() {
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            null,
            null,
            categoryResourceIdentifiersWithKeys,
            createRandomCategoryOrderHints(categoryReferencesWithIds));

    final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofWhiteList(NAME)).build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync).hasSize(2);
    final ProductUpdateAction updateAction = updateActionsFromSync.get(0);
    assertThat(updateAction).isInstanceOf(ProductChangeNameAction.class);
    assertThat(((ProductChangeNameAction) updateAction).getName()).isEqualTo(ofEnglish("new name"));
    final ProductUpdateAction updateAction2 = updateActionsFromSync.get(1);
    assertThat(updateAction2).isEqualTo(ProductPublishActionBuilder.of().build());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }
}
