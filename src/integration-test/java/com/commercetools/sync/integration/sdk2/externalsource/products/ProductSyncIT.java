package com.commercetools.sync.integration.sdk2.externalsource.products;

import static com.commercetools.sync.integration.sdk2.commons.utils.CategoryITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.*;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteAllProducts;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductITUtils.deleteProductSyncTestData;
import static com.commercetools.sync.integration.sdk2.commons.utils.ProductTypeITUtils.ensureProductType;
import static com.commercetools.sync.integration.sdk2.commons.utils.StateITUtils.ensureState;
import static com.commercetools.sync.integration.sdk2.commons.utils.TaxCategoryITUtils.ensureTaxCategory;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.*;
import static com.commercetools.sync.sdk2.products.utils.ProductReferenceResolutionUtils.createProductVariantDraft;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantAttributeUpdateActionUtils.ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyProductProjectionsGet;
import com.commercetools.api.client.ByProjectKeyProductProjectionsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyProductProjectionsKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyProductProjectionsRequestBuilder;
import com.commercetools.api.client.ByProjectKeyProductsByIDPost;
import com.commercetools.api.client.ByProjectKeyProductsByIDRequestBuilder;
import com.commercetools.api.client.ByProjectKeyProductsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryResourceIdentifier;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.common.PriceDraftBuilder;
import com.commercetools.api.models.error.DuplicateFieldError;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.AttributeBuilder;
import com.commercetools.api.models.product.CategoryOrderHints;
import com.commercetools.api.models.product.CategoryOrderHintsBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductPagedQueryResponse;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductPublishActionBuilder;
import com.commercetools.api.models.product.ProductRemoveFromCategoryAction;
import com.commercetools.api.models.product.ProductSetAttributeActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsActionBuilder;
import com.commercetools.api.models.product.ProductSetTaxCategoryAction;
import com.commercetools.api.models.product.ProductUpdate;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.tax_category.TaxCategory;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifier;
import com.commercetools.api.models.tax_category.TaxCategoryResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.products.ProductSync;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.ProductSyncStatistics;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductSyncIT {
  private static ProductType productType;
  private static TaxCategory targetTaxCategory;
  private static State targetProductState;
  private static List<CategoryReference> categoryReferencesWithIds;
  private static Set<CategoryResourceIdentifier> categoryResourceIdentifiersWithKeys;
  private static CategoryOrderHints categoryOrderHintsWithIds;
  private static CategoryOrderHints categoryOrderHintsWithKeys;
  private ProductSyncOptions syncOptions;
  private Product product;
  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Delete all product related test data from the target project. Then creates for the target CTP
   * project price a product type, a tax category, 2 categories, custom types for the categories and
   * a product state.
   */
  @BeforeAll
  static void setup() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
    ensureCategoriesCustomType(
        OLD_CATEGORY_CUSTOM_TYPE_KEY,
        Locale.ENGLISH,
        OLD_CATEGORY_CUSTOM_TYPE_NAME,
        CTP_TARGET_CLIENT);

    final List<CategoryDraft> categoryDrafts = getCategoryDrafts(null, 2);
    final List<Category> categories = ensureCategories(CTP_TARGET_CLIENT, categoryDrafts);
    categoryReferencesWithIds = getReferencesWithIds(categories);
    categoryResourceIdentifiersWithKeys = getResourceIdentifiersWithKeys(categories);
    categoryOrderHintsWithIds = createRandomCategoryOrderHints(categoryReferencesWithIds);
    categoryOrderHintsWithKeys =
        replaceCategoryOrderHintCategoryIdsWithKeys(categoryOrderHintsWithIds, categories);

    productType = ensureProductType(PRODUCT_TYPE_RESOURCE_PATH, CTP_TARGET_CLIENT);
    targetTaxCategory = ensureTaxCategory(CTP_TARGET_CLIENT);
    targetProductState = ensureState(CTP_TARGET_CLIENT, StateTypeEnum.PRODUCT_STATE);
  }

  /**
   * Deletes Products and Types from the target CTP project, then it populates target CTP project
   * with product test data.
   */
  @BeforeEach
  void setupTest() {
    clearSyncTestCollections();
    deleteAllProducts(CTP_TARGET_CLIENT);
    syncOptions = buildSyncOptions();

    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            productType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            categoryReferencesWithIds,
            categoryOrderHintsWithIds);

    product =
        CTP_TARGET_CLIENT
            .products()
            .create(productDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
  }

  private void clearSyncTestCollections() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();
  }

  private ProductSyncOptions buildSyncOptions() {
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    return ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
        .errorCallback(
            (exception, oldResource, newResource, updateActions) ->
                collectErrors(exception.getMessage(), exception.getCause()))
        .warningCallback(warningCallBack)
        .build();
  }

  @AfterAll
  static void tearDown() {
    deleteProductSyncTestData(CTP_TARGET_CLIENT);
  }

  @Test
  void sync_withNewProduct_shouldCreateProduct() {
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withMissingPriceChannel_shouldCreateProductDistributionPriceChannel() {
    final PriceDraft priceDraftWithMissingChannelRef =
        PriceDraftBuilder.of()
            .value(moneyBuilder -> moneyBuilder.currencyCode("EUR").centAmount(20L))
            .channel(ChannelResourceIdentifierBuilder.of().key("missingKey").build())
            .build();

    final ProductVariantDraft masterVariantDraft =
        ProductVariantDraftBuilder.of()
            .key("v2")
            .sku("1065833")
            .prices(Collections.singletonList(priceDraftWithMissingChannelRef))
            .build();

    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .masterVariant(masterVariantDraft)
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    collectErrors(exception.getMessage(), exception.getCause()))
            .warningCallback(warningCallBack)
            .ensurePriceChannels(true)
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    final Product productFromTargetProject =
        CTP_TARGET_CLIENT
            .products()
            .withKey(productDraft.getKey())
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
    final List<Price> prices =
        productFromTargetProject.getMasterData().getStaged().getMasterVariant().getPrices();
    assertThat(prices.size()).isEqualTo(1);

    final Channel channel =
        CTP_TARGET_CLIENT
            .channels()
            .withId(prices.get(0).getChannel().getId())
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join();
    assertThat(channel.getRoles()).containsOnly(ChannelRoleEnum.PRODUCT_DISTRIBUTION);
  }

  @Test
  void sync_withNewProductAndBeforeCreateCallback_shouldCreateProduct() {
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    final String keyPrefix = "callback_";
    final ProductSyncOptions options =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    collectErrors(exception.getMessage(), exception.getCause()))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .beforeCreateCallback(draft -> prefixDraftKey(draft, keyPrefix))
            .build();

    final ProductSync productSync = new ProductSync(options);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 1, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();

    // Query for a product with key prefixed with "callback_" added by the callback

    final String keyWithCallbackPrefix = format("%s%s", keyPrefix, productDraft.getKey());
    final Product fetchedProduct =
        CTP_TARGET_CLIENT
            .products()
            .get()
            .withWhere("key=:key")
            .withPredicateVar("key", keyWithCallbackPrefix)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .thenApply(ProductPagedQueryResponse::getResults)
            .thenApply(products -> products.isEmpty() ? null : products.get(0))
            .toCompletableFuture()
            .join();

    assertThat(fetchedProduct).isNotNull();
    assertThat(fetchedProduct.getKey()).isEqualTo(keyWithCallbackPrefix);
    assertThat(fetchedProduct.getMasterData().getCurrent().getName())
        .isEqualTo(productDraft.getName());
  }

  @Test
  void sync_withNewProductWithExistingSlug_shouldNotCreateProduct() {
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .slug(product.getMasterData().getStaged().getSlug())
            .build();

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);

    final String duplicatedSlug = product.getMasterData().getStaged().getSlug().get(Locale.ENGLISH);

    assertThat(errorCallBackExceptions)
        .hasSize(1)
        .allSatisfy(
            exception -> {
              BadRequestException badRequestException = (BadRequestException) exception.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue()).isEqualTo(duplicatedSlug);
                      });
            });

    assertThat(errorCallBackMessages)
        .hasSize(1)
        .allSatisfy(
            errorMessage -> {
              assertThat(errorMessage).contains("\"code\" : \"DuplicateField\"");
              assertThat(errorMessage).contains("\"field\" : \"slug.en\"");
              assertThat(errorMessage)
                  .contains(format("\"duplicateValue\" : \"%s\"", duplicatedSlug));
            });

    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withEqualProduct_shouldNotUpdateProduct() {
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withChangedProduct_shouldUpdateProduct() {
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withChangedProductButConcurrentModificationException_shouldRetryAndUpdateProduct() {
    // preparation
    final ProjectApiRoot spyClient = buildClientWithConcurrentModificationUpdate();

    final ProductSyncOptions spyOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    collectErrors(exception.getMessage(), exception.getCause()))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final ProductSync spyProductSync = new ProductSync(spyOptions);

    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductSyncStatistics syncStatistics =
        spyProductSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

    final ProductSyncOptions spyOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    collectErrors(exception.getMessage(), exception.getCause()))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final ProductSync spyProductSync = new ProductSync(spyOptions);

    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductSyncStatistics syncStatistics =
        spyProductSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackExceptions).hasSize(1);

    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorCallBackMessages.get(0)).contains("test");
  }

  @Test
  void
      syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final ProjectApiRoot spyClient =
        buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

    final ProductSyncOptions spyOptions =
        ProductSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    collectErrors(exception.getMessage(), exception.getCause()))
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    final ProductSync spyProductSync = new ProductSync(spyOptions);

    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductSyncStatistics syncStatistics =
        spyProductSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // Test and assertion
    assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackExceptions).hasSize(1);

    assertThat(errorCallBackMessages.get(0))
        .contains(
            format("Failed to fetch existing products with keys: '[%s]'", productDraft.getKey()));
  }

  @Test
  void sync_withMultipleBatchSyncing_ShouldSync() {
    // Prepare existing products with keys: productKey1, productKey2, productKey3.
    final ProductDraft key2Draft =
        createProductDraft(
            PRODUCT_KEY_2_RESOURCE_PATH,
            productType.toReference(),
            targetTaxCategory.toReference(),
            targetProductState.toReference(),
            categoryReferencesWithIds,
            product.getMasterData().getStaged().getCategoryOrderHints());
    CTP_TARGET_CLIENT.products().create(key2Draft).executeBlocking();

    final ProductDraft key3Draft =
        createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH, productType.toResourceIdentifier())
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey3")
            .slug(LocalizedString.ofEnglish("slug3"))
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").build())
            .taxCategory(
                TaxCategoryResourceIdentifierBuilder.of().id(targetTaxCategory.getId()).build())
            .build();
    CTP_TARGET_CLIENT.products().create(key3Draft).executeBlocking();

    // Prepare batches from external source
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final List<ProductDraft> batch1 = new ArrayList<>();
    batch1.add(productDraft);

    final ProductDraft key4Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey4")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug4"))
            .masterVariant(ProductVariantDraftBuilder.of().key("v4").sku("sku4").build())
            .build();

    final List<ProductDraft> batch2 = new ArrayList<>();
    batch2.add(key4Draft);

    final ProductDraft key3DraftNewSlug =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "newSlug"))
            .masterVariant(ProductVariantDraftBuilder.of().key("v3").sku("sku3").build())
            .build();

    final List<ProductDraft> batch3 = new ArrayList<>();
    batch3.add(key3DraftNewSlug);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(batch1)
            .thenCompose(result -> productSync.sync(batch2))
            .thenCompose(result -> productSync.sync(batch3))
            .toCompletableFuture()
            .join();

    assertThat(syncStatistics).hasValues(3, 1, 2, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withSingleBatchSyncing_ShouldSync() {
    // Prepare batches from external source
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductDraft key3Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory(
                TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build())
            .state(StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build())
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey3")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug3"))
            .masterVariant(ProductVariantDraftBuilder.of().key("mv3").sku("sku3").build())
            .build();

    final ProductDraft key4Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory(
                TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build())
            .state(StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build())
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey4")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug4"))
            .masterVariant(ProductVariantDraftBuilder.of().key("mv4").sku("sku4").build())
            .build();

    final ProductDraft key5Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory(
                TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build())
            .state(StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build())
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey5")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug5"))
            .masterVariant(ProductVariantDraftBuilder.of().key("mv5").sku("sku5").build())
            .build();

    final ProductDraft key6Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory(
                TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build())
            .state(StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build())
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey6")
            .slug(LocalizedString.of(Locale.ENGLISH, "slug6"))
            .masterVariant(ProductVariantDraftBuilder.of().key("mv6").sku("sku6").build())
            .build();

    final List<ProductDraft> batch = new ArrayList<>();
    batch.add(productDraft);
    batch.add(key3Draft);
    batch.add(key4Draft);
    batch.add(key5Draft);
    batch.add(key6Draft);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(batch).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(5, 4, 1, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withSameSlugInSingleBatch_ShouldNotSyncIt() {
    // Prepare batches from external source
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final ProductDraft key3Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey3")
            .masterVariant(ProductVariantDraftBuilder.of().key("k3").sku("s3").build())
            .build();

    final ProductDraft key4Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey4")
            .masterVariant(ProductVariantDraftBuilder.of().key("k4").sku("s4").build())
            .build();

    final ProductDraft key5Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey5")
            .masterVariant(ProductVariantDraftBuilder.of().key("k5").sku("s5").build())
            .build();

    final ProductDraft key6Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("productKey6")
            .masterVariant(ProductVariantDraftBuilder.of().key("k6").sku("s6").build())
            .build();

    final List<ProductDraft> batch = new ArrayList<>();
    batch.add(productDraft);
    batch.add(key3Draft);
    batch.add(key4Draft);
    batch.add(key5Draft);
    batch.add(key6Draft);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(batch).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(5, 1, 1, 3, 0);

    final String duplicatedSlug = key3Draft.getSlug().get(Locale.ENGLISH);
    assertThat(errorCallBackExceptions)
        .hasSize(3)
        .allSatisfy(
            exception -> {
              BadRequestException badRequestException = (BadRequestException) exception.getCause();

              final List<DuplicateFieldError> fieldErrors =
                  badRequestException.getErrorResponse().getErrors().stream()
                      .map(
                          ctpError -> {
                            assertThat(ctpError.getCode())
                                .isEqualTo(DuplicateFieldError.DUPLICATE_FIELD);
                            return (DuplicateFieldError) ctpError;
                          })
                      .collect(toList());
              assertThat(fieldErrors).hasSize(1);
              assertThat(fieldErrors)
                  .allSatisfy(
                      error -> {
                        assertThat(error.getField()).isEqualTo("slug.en");
                        assertThat(error.getDuplicateValue()).isEqualTo(duplicatedSlug);
                      });
            });

    assertThat(errorCallBackMessages)
        .hasSize(3)
        .allSatisfy(
            errorMessage -> {
              assertThat(errorMessage).contains("\"code\" : \"DuplicateField\"");
              assertThat(errorMessage).contains("\"field\" : \"slug.en\"");
              assertThat(errorMessage)
                  .contains(format("\"duplicateValue\" : \"%s\"", duplicatedSlug));
            });
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withADraftsWithBlankKeysInBatch_ShouldNotSyncItAndTriggerErrorCallBack() {
    // Prepare batches from external source
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build(),
            TaxCategoryResourceIdentifierBuilder.of().key(targetTaxCategory.getKey()).build(),
            StateResourceIdentifierBuilder.of().key(targetProductState.getKey()).build(),
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    // Draft with null key
    final ProductDraft key3Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key(null)
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .build();

    // Draft with empty key
    final ProductDraft key4Draft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key("")
            .masterVariant(ProductVariantDraftBuilder.of().build())
            .productType(
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .build();

    final List<ProductDraft> batch = new ArrayList<>();
    batch.add(productDraft);
    batch.add(key3Draft);
    batch.add(key4Draft);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(batch).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(3, 0, 1, 2, 0);
    assertThat(errorCallBackExceptions).hasSize(2);
    assertThat(errorCallBackMessages).hasSize(2);
    assertThat(errorCallBackMessages.get(0))
        .containsIgnoringCase(
            format("ProductDraft with name: %s doesn't have a key.", key3Draft.getName()));
    assertThat(errorCallBackMessages.get(1))
        .containsIgnoringCase(
            format("ProductDraft with name: %s doesn't have a key.", key4Draft.getName()));
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withANullDraftInBatch_ShouldNotSyncItAndTriggerErrorCallBack() {
    // Prepare batches from external source
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().id(productType.getKey()).build(),
            null,
            null,
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    final List<ProductDraft> batch = new ArrayList<>();
    batch.add(productDraft);
    batch.add(null);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(batch).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 0, 1, 1, 0);
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0)).isEqualToIgnoringCase("ProductDraft is null.");
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void
      sync_withSameDraftsWithChangesInBatch_ShouldRetryUpdateBecauseOfConcurrentModificationExceptions() {
    // Prepare batches from external source
    final ProductDraft productDraft =
        createProductDraft(
            PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
            ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build(),
            null,
            null,
            new ArrayList<>(categoryResourceIdentifiersWithKeys),
            categoryOrderHintsWithKeys);

    // Draft with same key
    final ProductDraft draftWithSameKey =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .categories(new ArrayList<>())
            .categoryOrderHints(CategoryOrderHintsBuilder.of().values(new HashMap<>()).build())
            .key(productDraft.getKey())
            .masterVariant(
                createProductVariantDraft(product.getMasterData().getStaged().getMasterVariant()))
            .build();

    final List<ProductDraft> batch = new ArrayList<>();
    batch.add(productDraft);
    batch.add(draftWithSameKey);

    final ProductSync productSync = new ProductSync(syncOptions);
    final ProductSyncStatistics syncStatistics =
        productSync.sync(batch).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(2, 0, 2, 0, 0);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withProductContainingAttributeChanges_shouldSyncProductCorrectly() {
    // preparation
    final List<ProductUpdateAction> updateActions = new ArrayList<>();
    final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
        warningCallBack =
            (exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage());

    final ProductSyncOptions customOptions =
        ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) ->
                    collectErrors(exception.getMessage(), exception.getCause()))
            .warningCallback(warningCallBack)
            .beforeUpdateCallback(
                (actions, draft, old) -> {
                  updateActions.addAll(actions);
                  return actions;
                })
            .build();

    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_RESOURCE_PATH,
                ProductTypeResourceIdentifierBuilder.of().key(productType.getKey()).build())
            .categories(emptyList())
            .taxCategory((TaxCategoryResourceIdentifier) null)
            .state((StateResourceIdentifier) null)
            .build();

    // Creating the attribute draft with the changes
    final Attribute priceInfoAttrDraft =
        AttributeBuilder.of().name("priceInfo").value("100/kg").build();
    final Attribute angebotAttrDraft =
        AttributeBuilder.of().name("angebot").value("big discount").build();
    final Attribute unknownAttrDraft =
        AttributeBuilder.of().name("unknown").value("unknown").build();

    // Creating the product variant draft with the product reference attribute
    final List<Attribute> attributes =
        asList(priceInfoAttrDraft, angebotAttrDraft, unknownAttrDraft);

    final ProductVariantDraft masterVariant =
        ProductVariantDraftBuilder.of(productDraft.getMasterVariant())
            .attributes(attributes)
            .build();

    final ProductDraft productDraftWithChangedAttributes =
        ProductDraftBuilder.of(productDraft).masterVariant(masterVariant).build();

    // test
    final ProductSync productSync = new ProductSync(customOptions);
    final ProductSyncStatistics syncStatistics =
        productSync
            .sync(singletonList(productDraftWithChangedAttributes))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);

    final String causeErrorMessage =
        format(ATTRIBUTE_NOT_IN_ATTRIBUTE_METADATA, unknownAttrDraft.getName());
    final String expectedErrorMessage =
        format(
            FAILED_TO_BUILD_ATTRIBUTE_UPDATE_ACTION,
            unknownAttrDraft.getName(),
            productDraft.getMasterVariant().getKey(),
            productDraft.getKey(),
            causeErrorMessage);

    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0).getMessage()).isEqualTo(expectedErrorMessage);

    assertThat(errorCallBackExceptions.get(0).getCause().getMessage()).isEqualTo(causeErrorMessage);
    assertThat(errorCallBackMessages).containsExactly(expectedErrorMessage);
    assertThat(warningCallBackMessages).isEmpty();

    assertThat(updateActions)
        .filteredOn(updateAction -> !(updateAction instanceof ProductSetTaxCategoryAction))
        .filteredOn(updateAction -> !(updateAction instanceof ProductRemoveFromCategoryAction))
        .containsExactlyInAnyOrder(
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .staged(true)
                .name(priceInfoAttrDraft.getName())
                .value(priceInfoAttrDraft.getValue())
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(1L)
                .name(angebotAttrDraft.getName())
                .value(angebotAttrDraft.getValue())
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("size")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("rinderrasse")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("herkunft")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("teilstueck")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("fuetterung")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("reifung")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("haltbarkeit")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("verpackung")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("anlieferung")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeInAllVariantsActionBuilder.of()
                .name("zubereitung")
                .value(null)
                .staged(true)
                .build(),
            ProductSetAttributeActionBuilder.of()
                .variantId(1L)
                .name("localisedText")
                .value(null)
                .staged(true)
                .build(),
            ProductPublishActionBuilder.of().build());
  }

  @Nonnull
  private static ProductDraft prefixDraftKey(
      @Nonnull final ProductDraft productDraft, @Nonnull final String prefix) {
    final String newKey = format("%s%s", prefix, productDraft.getKey());
    return ProductDraftBuilder.of(productDraft).key(newKey).build();
  }

  private void collectErrors(final String errorMessage, final Throwable exception) {
    errorCallBackMessages.add(errorMessage);
    errorCallBackExceptions.add(exception);
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdate() {
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
    final ByProjectKeyProductsRequestBuilder byProjectKeyProductsRequestBuilder = mock();
    final ByProjectKeyProductsByIDRequestBuilder byProjectKeyProductsByIDRequestBuilder = mock();
    final ByProjectKeyProductsByIDPost byProjectKeyProductsByIDPost = mock();

    when(spyClient.products()).thenReturn(byProjectKeyProductsRequestBuilder).thenCallRealMethod();
    when(byProjectKeyProductsRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyProductsByIDRequestBuilder);
    when(byProjectKeyProductsByIDRequestBuilder.post(any(ProductUpdate.class)))
        .thenReturn(byProjectKeyProductsByIDPost)
        .thenCallRealMethod();

    when(byProjectKeyProductsByIDPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException()));

    return spyClient;
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

    final ByProjectKeyProductsRequestBuilder byProjectKeyProductsRequestBuilder = mock();
    final ByProjectKeyProductsByIDRequestBuilder byProjectKeyProductsByIDRequestBuilder = mock();
    final ByProjectKeyProductsByIDPost byProjectKeyProductsByIDPost = mock();

    when(spyClient.products()).thenReturn(byProjectKeyProductsRequestBuilder);
    when(byProjectKeyProductsRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyProductsByIDRequestBuilder);
    when(byProjectKeyProductsByIDRequestBuilder.post(any(ProductUpdate.class)))
        .thenReturn(byProjectKeyProductsByIDPost);

    when(byProjectKeyProductsByIDPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException()))
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(createBadGatewayException()));

    return spyClient;
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);

    final ByProjectKeyProductProjectionsRequestBuilder
        byProjectKeyProductProjectionsRequestBuilder = mock();
    final ByProjectKeyProductProjectionsKeyByKeyRequestBuilder
        byProjectKeyProductProjectionsKeyByKeyRequestBuilder = mock();
    final ByProjectKeyProductProjectionsKeyByKeyGet byProjectKeyProductProjectionsKeyByKeyGet =
        mock();
    final ByProjectKeyProductProjectionsGet byProjectKeyProductProjectionsGet = mock();

    when(spyClient.productProjections()).thenReturn(byProjectKeyProductProjectionsRequestBuilder);
    when(byProjectKeyProductProjectionsRequestBuilder.get())
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withStaged(anyBoolean()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withPredicateVar(anyString(), any(Set.class)))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withLimit(anyInt()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyProductProjectionsGet);
    when(byProjectKeyProductProjectionsRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyProductProjectionsKeyByKeyRequestBuilder);
    when(byProjectKeyProductProjectionsKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyProductProjectionsKeyByKeyGet);

    final ByProjectKeyProductsRequestBuilder byProjectKeyProductsRequestBuilder = mock();
    final ByProjectKeyProductsByIDRequestBuilder byProjectKeyProductsByIDRequestBuilder = mock();
    final ByProjectKeyProductsByIDPost byProjectKeyProductsByIDPost = mock();

    when(spyClient.products()).thenReturn(byProjectKeyProductsRequestBuilder);
    when(byProjectKeyProductsRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyProductsByIDRequestBuilder);
    when(byProjectKeyProductsByIDRequestBuilder.post(any(ProductUpdate.class)))
        .thenReturn(byProjectKeyProductsByIDPost);

    when(byProjectKeyProductsByIDPost.execute())
        .thenReturn(
            CompletableFutureUtils.exceptionallyCompletedFuture(
                createConcurrentModificationException()));

    when(byProjectKeyProductProjectionsGet.execute())
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(createNotFoundException()));
    when(byProjectKeyProductProjectionsKeyByKeyGet.execute())
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(createNotFoundException()));

    return spyClient;
  }
}
