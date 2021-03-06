package com.commercetools.sync.integration.externalsource.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_KEY;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.OLD_CATEGORY_CUSTOM_TYPE_NAME;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategories;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.createCategoriesCustomType;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.geResourceIdentifiersWithKeys;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getCategoryDrafts;
import static com.commercetools.sync.integration.commons.utils.CategoryITUtils.getReferencesWithIds;
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

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.products.ProductSync;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.updateactions.AddToCategory;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.products.commands.updateactions.Publish;
import io.sphere.sdk.products.commands.updateactions.RemoveFromCategory;
import io.sphere.sdk.products.commands.updateactions.SetCategoryOrderHint;
import io.sphere.sdk.producttypes.ProductType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncFilterIT {

  private static ProductType productType;
  private static List<Reference<Category>> categoryReferencesWithIds;
  private static Set<ResourceIdentifier<Category>> categoryResourceIdentifiersWithKeys;
  private ProductSyncOptionsBuilder syncOptionsBuilder;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;
  private List<UpdateAction<Product>> updateActionsFromSync;

  /**
   * Delete all product related test data from target project. Then create custom types for the
   * categories and a productType for the products of the target CTP project.
   */
  @BeforeAll
  static void setupAllTests() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    createCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);
    final List<Category> categories =
        createCategories(CTP_TARGET_CLIENT, getCategoryDrafts(null, 2));
    categoryReferencesWithIds = getReferencesWithIds(categories);
    categoryResourceIdentifiersWithKeys = geResourceIdentifiersWithKeys(categories);
    productType = createProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
  }

  /**
   * 1. Deletes all products from target CTP project 2. Clears all sync collections used for test
   * assertions. 3. Creates an instance for {@link ProductSyncOptionsBuilder} that will be used in
   * the tests to build {@link ProductSyncOptions} instances. 4. Create a product in the target CTP
   * project.
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
    executeBlocking(CTP_TARGET_CLIENT.execute(ProductCreateCommand.of(productDraft)));
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
            List<UpdateAction<Product>>>
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
            List<UpdateAction<Product>>,
            ProductDraft,
            ProductProjection,
            List<UpdateAction<Product>>>
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
            referenceOfId(productType.getKey()),
            null,
            null,
            categoryResourceIdentifiersWithKeys,
            createRandomCategoryOrderHints(categoryResourceIdentifiersWithKeys));

    final ProductSyncOptions syncOptions =
        syncOptionsBuilder.syncFilter(ofBlackList(CATEGORIES)).build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(productDraft)));

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(
            updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof RemoveFromCategory))
        .isTrue();
    assertThat(
            updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof AddToCategory))
        .isTrue();
    assertThat(
            updateActionsFromSync.stream()
                .noneMatch(updateAction -> updateAction instanceof SetCategoryOrderHint))
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
            referenceOfId(productType.getKey()),
            null,
            null,
            categoryResourceIdentifiersWithKeys,
            createRandomCategoryOrderHints(categoryResourceIdentifiersWithKeys));

    final ProductSyncOptions syncOptions = syncOptionsBuilder.syncFilter(ofWhiteList(NAME)).build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        executeBlocking(productSync.sync(singletonList(productDraft)));

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(updateActionsFromSync).hasSize(2);
    final UpdateAction<Product> updateAction = updateActionsFromSync.get(0);
    assertThat(updateAction.getAction()).isEqualTo("changeName");
    assertThat(updateAction).isExactlyInstanceOf(ChangeName.class);
    assertThat(((ChangeName) updateAction).getName())
        .isEqualTo(LocalizedString.of(Locale.ENGLISH, "new name"));
    final UpdateAction<Product> updateAction2 = updateActionsFromSync.get(1);
    assertThat(updateAction2).isEqualTo(Publish.of());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }
}
