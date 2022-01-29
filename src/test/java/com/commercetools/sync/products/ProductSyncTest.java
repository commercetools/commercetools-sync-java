package com.commercetools.sync.products;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.*;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class ProductSyncTest {

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithNoValidDrafts_ShouldCompleteWithoutAnyProcessing() {
    // preparation
    final SphereClient ctpClient = mock(SphereClient.class);
    final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(ctpClient).build();

    final ProductService productService = mock(ProductService.class);
    final ProductSync productSync =
        new ProductSync(
            productSyncOptions,
            productService,
            mock(ProductTypeService.class),
            mock(CategoryService.class),
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    final ProductDraft productDraftWithoutKey =
        ProductDraftBuilder.of(
                ResourceIdentifier.ofKey("productTypeKey"),
                ofEnglish("name"),
                ofEnglish("slug"),
                emptyList())
            .build();

    // test
    final ProductSyncStatistics statistics =
        productSync.sync(singletonList(productDraftWithoutKey)).toCompletableFuture().join();

    // assertion
    verifyNoMoreInteractions(ctpClient);
    verifyNoMoreInteractions(productService);
    assertThat(statistics).hasValues(1, 0, 0, 1, 0);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, ResourceIdentifier.ofKey("productTypeKey"))
            .build();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductService productService = spy(new ProductServiceImpl(syncOptions));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.cacheKeysToIds(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));

    final ProductSync productSync =
        new ProductSync(
            syncOptions,
            productService,
            productTypeService,
            mock(CategoryService.class),
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    final ProductSyncStatistics productSyncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(SphereException.class);

    assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

    final SphereClient mockClient = mock(SphereClient.class);
    when(mockClient.execute(any(ProductProjectionQuery.class)))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new SphereException();
                }));

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductService productService = spy(new ProductServiceImpl(syncOptions));
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSync productSync =
        new ProductSync(
            syncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    final ProductSyncStatistics productSyncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to fetch existing products");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(SphereException.class);

    assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_2_RESOURCE_PATH, ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final ProductService productService = mock(ProductService.class);
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
    when(productService.fetchMatchingProductsByKeys(anySet()))
        .thenReturn(completedFuture(emptySet()));
    when(productService.createProduct(any())).thenReturn(completedFuture(Optional.empty()));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

    final ProductSync productSync =
        new ProductSync(
            spyProductSyncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertion
    verify(spyProductSyncOptions).applyBeforeCreateCallback(any());
    verify(spyProductSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

    final ProductProjection mockedExistingProduct =
        readObjectFromResource(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, Product.class)
            .toProjection(STAGED);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    final ProductService productService = mock(ProductService.class);
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
    when(productService.fetchMatchingProductsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingProduct)));
    when(productService.updateProduct(any(), any()))
        .thenReturn(completedFuture(mockedExistingProduct));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
    when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
        .thenReturn(completedFuture(Optional.of(new HashMap<>())));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

    final ProductSync productSync =
        new ProductSync(
            spyProductSyncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertion
    verify(spyProductSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyProductSyncOptions, never()).applyBeforeCreateCallback(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void sync_WithEmptyAttributeMetaDataMap_ShouldCallErrorCallback() {
    // preparation
    final ProductDraft productDraft =
        createProductDraftBuilder(
                PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

    final ProductProjection mockedExistingProduct =
        readObjectFromResource(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, Product.class)
            .toProjection(STAGED);
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final ProductService productService = mock(ProductService.class);
    final Map<String, String> keyToIds = new HashMap<>();
    keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
    when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
    when(productService.fetchMatchingProductsByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingProduct)));
    when(productService.updateProduct(any(), any()))
        .thenReturn(completedFuture(mockedExistingProduct));

    final ProductTypeService productTypeService = mock(ProductTypeService.class);
    when(productTypeService.fetchCachedProductTypeId(any()))
        .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
    when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
        .thenReturn(completedFuture(Optional.empty()));

    final CategoryService categoryService = mock(CategoryService.class);
    when(categoryService.fetchMatchingCategoriesByKeys(any()))
        .thenReturn(completedFuture(emptySet()));

    final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

    final ProductSync productSync =
        new ProductSync(
            spyProductSyncOptions,
            productService,
            productTypeService,
            categoryService,
            mock(TypeService.class),
            mock(ChannelService.class),
            mock(CustomerGroupService.class),
            mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class),
            mock(CustomerService.class));

    // test
    ProductSyncStatistics productSyncStatistics =
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

    // assertion

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "Failed to update Product with key: 'productKey1'. Reason: Failed to"
                + " fetch a productType for the product to build the products' attributes metadata.");

    AssertionsForStatistics.assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
  }
}
