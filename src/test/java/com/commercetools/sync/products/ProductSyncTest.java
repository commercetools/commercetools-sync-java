package com.commercetools.sync.products;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.UnresolvedReferencesService;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_CHANGED_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_TYPE_RESOURCE_PATH;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProductSyncTest {

    @Test
    void sync_WithNoValidDrafts_ShouldCompleteWithoutAnyProcessing() {
        // preparation
        final SphereClient ctpClient = mock(SphereClient.class);
        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
            .of(ctpClient)
            .build();

        final ProductService productService = mock(ProductService.class);
        final ProductSync productSync = new ProductSync(productSyncOptions, productService,
            mock(ProductTypeService.class), mock(CategoryService.class), mock(TypeService.class),
            mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class), mock(CustomObjectService.class));

        final ProductDraft productDraftWithoutKey = ProductDraftBuilder
            .of(ResourceIdentifier.ofKey("productTypeKey"), ofEnglish("name"), ofEnglish("slug"), emptyList())
            .build();

        // test
        final ProductSyncStatistics statistics = productSync
            .sync(singletonList(productDraftWithoutKey))
            .toCompletableFuture()
            .join();

        // assertion
        verifyNoMoreInteractions(ctpClient);
        verifyNoMoreInteractions(productService);
        assertThat(statistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ResourceIdentifier.ofKey("productTypeKey"))
            .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception.getCause());
            })
            .build();

        final ProductService productService = spy(new ProductServiceImpl(syncOptions));

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.cacheKeysToIds(any()))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductSync productSync = new ProductSync(syncOptions, productService,
            productTypeService, mock(CategoryService.class), mock(TypeService.class),
            mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class));

        // test
        final ProductSyncStatistics productSyncStatistics = productSync
            .sync(singletonList(productDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement().satisfies(message ->
                assertThat(message).contains("Failed to build a cache of keys to ids.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .singleElement().satisfies(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
                ResourceIdentifier.ofKey("productTypeKey"))
                .taxCategory(null)
                .state(null)
                .build();

        final SphereClient mockClient = mock(SphereClient.class);

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
                .of(mockClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .build();

        final ProductService productService = spy(new ProductServiceImpl(syncOptions));
        final Map<String, String> keyToIds = new HashMap<>();
        when(productService.fetchMatchingProductsByKeys(anySet()))
            .thenReturn(supplyAsync(() -> { throw new CompletionException(new SphereException()); }));

        keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
        when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
                .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));


        final ProductSync productSync = new ProductSync(syncOptions, productService,
                productTypeService, categoryService, mock(TypeService.class),
                mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
                mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class));

        // test
        final ProductSyncStatistics productSyncStatistics = productSync
                .sync(singletonList(productDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
                .hasSize(1)
                .singleElement().satisfies(message ->
                        assertThat(message).contains("Failed to fetch existing products")
            );

        assertThat(exceptions)
                .hasSize(1)
                .singleElement().satisfies(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });

        assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final ProductService productService = mock(ProductService.class);
        when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));
        when(productService.fetchMatchingProductsByKeys(anySet())).thenReturn(completedFuture(emptySet()));
        when(productService.createProduct(any())).thenReturn(completedFuture(Optional.empty()));

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));

        final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

        final ProductSync productSync = new ProductSync(spyProductSyncOptions, productService,
            productTypeService, categoryService, mock(TypeService.class),
            mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class));

        // test
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();


        // assertion
        verify(spyProductSyncOptions).applyBeforeCreateCallback(any());
        verify(spyProductSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
            ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

        final Product mockedExistingProduct =
            readObjectFromResource(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, Product.class);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final ProductService productService = mock(ProductService.class);
        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());
        when(productService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(productService.fetchMatchingProductsByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockedExistingProduct)));
        when(productService.updateProduct(any(), any())).thenReturn(completedFuture(mockedExistingProduct));

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
        when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
            .thenReturn(completedFuture(Optional.of(new HashMap<>())));

        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));

        final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

        final ProductSync productSync = new ProductSync(spyProductSyncOptions, productService,
            productTypeService, categoryService, mock(TypeService.class),
            mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class));

        // test
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spyProductSyncOptions, never()).applyBeforeCreateCallback(any());
    }

    @Test
    void sync_WithEmptyAttributeMetaDataMap_ShouldCallErrorCallback() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
            ResourceIdentifier.ofKey("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

        final Product mockedExistingProduct =
            readObjectFromResource(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH, Product.class);
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
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
        when(productService.updateProduct(any(), any())).thenReturn(completedFuture(mockedExistingProduct));

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
        when(productTypeService.fetchCachedProductAttributeMetaDataMap(any()))
            .thenReturn(completedFuture(Optional.empty()));

        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));

        final ProductSyncOptions spyProductSyncOptions = spy(productSyncOptions);

        final ProductSync productSync = new ProductSync(spyProductSyncOptions, productService,
            productTypeService, categoryService, mock(TypeService.class),
            mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
            mock(StateService.class),
            mock(UnresolvedReferencesService.class),
            mock(CustomObjectService.class));

        // test
        ProductSyncStatistics productSyncStatistics =
            productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

        // assertion

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement().satisfies(message ->
                assertThat(message).contains("Failed to update Product with key: 'productKey1'. Reason: Failed to"
                    + " fetch a productType for the product to build the products' attributes metadata.")
            );

        AssertionsForStatistics.assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_withChangedProductButConcurrentModificationException_shouldRetryAndUpdateProduct() {
        // preparation

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final ProductDraft productDraft =
                createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
                        ResourceIdentifier.ofKey("syncProductType"))
                        .taxCategory(null)
                        .state(null)
                        .build();

        final ProductService mockProductService = buildMockProductServiceWithSuccessfulUpdateOnRetry(productDraft);
        final ProductTypeService mockProductTypeService = buildMockProductTypeService();
        final CategoryService mockCategoryService = buildMockCategoryService();

        final ProductSync productSync = new ProductSync(spy(syncOptions), mockProductService,
                mockProductTypeService, mockCategoryService, mock(TypeService.class),
                mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
                mock(StateService.class),
                mock(UnresolvedReferencesService.class),
                mock(CustomObjectService.class));

        final ProductSyncStatistics syncStatistics = productSync
                .sync(singletonList(productDraft))
                .toCompletableFuture()
                .join();

        // assertion
        assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
        assertThat(exceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    void sync_withChangedProductButConcurrentModificationExceptionAndFailedFetchOnRetry_shouldFailedUpdateProduct() {
        // preparation

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final ProductDraft productDraft =
                createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
                        ResourceIdentifier.ofKey("syncProductType"))
                        .taxCategory(null)
                        .state(null)
                        .build();

        final ProductService mockProductService = buildMockProductServiceWithFailedFetchOnRetry(productDraft);
        final ProductTypeService mockProductTypeService = buildMockProductTypeService();
        final CategoryService mockCategoryService = buildMockCategoryService();

        final ProductSync productSync = new ProductSync(spy(syncOptions), mockProductService,
                mockProductTypeService, mockCategoryService, mock(TypeService.class),
                mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
                mock(StateService.class),
                mock(UnresolvedReferencesService.class),
                mock(CustomObjectService.class));

        final ProductSyncStatistics syncStatistics = productSync
                .sync(singletonList(productDraft))
                .toCompletableFuture()
                .join();

        // assertion

        // Test and assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions.get(0)).isNotNull();
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update Product with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                        + "after concurrency modification.", productDraft.getKey()));

    }

    @Test
    void sync_withChangedProductButConcurrentModificationExceptionAndFetchNothingOnRetry_shouldFailedUpdateProduct() {
        // preparation

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final ProductDraft productDraft =
                createProductDraftBuilder(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH,
                        ResourceIdentifier.ofKey("syncProductType"))
                        .taxCategory(null)
                        .state(null)
                        .build();

        final ProductService mockProductService = buildMockProductServiceWithNotFoundFetchOnRetry(productDraft);
        final ProductTypeService mockProductTypeService = buildMockProductTypeService();
        final CategoryService mockCategoryService = buildMockCategoryService();

        final ProductSync productSync = new ProductSync(spy(syncOptions), mockProductService,
                mockProductTypeService, mockCategoryService, mock(TypeService.class),
                mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
                mock(StateService.class),
                mock(UnresolvedReferencesService.class),
                mock(CustomObjectService.class));

        final ProductSyncStatistics syncStatistics = productSync
                .sync(singletonList(productDraft))
                .toCompletableFuture()
                .join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update Product with key: '%s'. Reason: Not found when attempting to fetch while"
                        + " retrying after concurrency modification.", productDraft.getKey()));
    }

    @Nonnull
    private ProductTypeService buildMockProductTypeService() {
        final ProductTypeService mockProductTypeService = mock(ProductTypeService.class);

        final ProductType mockProductType =
                readObjectFromResource(PRODUCT_TYPE_RESOURCE_PATH, ProductType.class);

        final  Map<String, AttributeMetaData> attributeMetaDataMap =
                mockProductType.getAttributes()
                        .stream()
                        .map(AttributeMetaData::of)
                        .collect(Collectors.toMap(AttributeMetaData::getName, attributeMetaData -> attributeMetaData));

        when(mockProductTypeService.fetchCachedProductTypeId(any()))
                .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));
        when(mockProductTypeService.fetchCachedProductAttributeMetaDataMap(any()))
                .thenReturn(completedFuture(Optional.of(attributeMetaDataMap)));

        return mockProductTypeService;
    }

    @Nonnull
    private ProductService buildMockProductServiceWithSuccessfulUpdateOnRetry(
            @Nonnull final ProductDraft productDraft) {

        final ProductService mockProductService = mock(ProductService.class);

        final Product mockProduct =
                readObjectFromResource(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, Product.class);

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());

        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(supplyAsync(() -> { throw new CompletionException(new SphereException()); }));
        when(mockProductService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockProduct)));
        when(mockProductService.fetchProduct(any())).thenReturn(completedFuture(Optional.of(mockProduct)));
        when(mockProductService.updateProduct(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockProduct));
        return mockProductService;
    }

    @Nonnull
    private ProductService buildMockProductServiceWithFailedFetchOnRetry(
            @Nonnull final ProductDraft productDraft) {

        final ProductService mockProductService = mock(ProductService.class);

        final Product mockProduct =
                readObjectFromResource(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, Product.class);

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());

        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(supplyAsync(() -> { throw new CompletionException(new SphereException()); }));
        when(mockProductService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockProduct)));
        when(mockProductService.fetchProduct(any()))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));
        when(mockProductService.updateProduct(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockProduct));
        return mockProductService;
    }

    @Nonnull
    private ProductService buildMockProductServiceWithNotFoundFetchOnRetry(
            @Nonnull final ProductDraft productDraft) {

        final ProductService mockProductService = mock(ProductService.class);

        final Product mockProduct =
                readObjectFromResource(PRODUCT_KEY_1_CHANGED_RESOURCE_PATH, Product.class);

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(productDraft.getKey(), UUID.randomUUID().toString());

        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(supplyAsync(() -> { throw new CompletionException(new SphereException()); }));
        when(mockProductService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockProductService.fetchMatchingProductsByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockProduct)));
        when(mockProductService.fetchProduct(any())).thenReturn(completedFuture(Optional.empty()));
        when(mockProductService.updateProduct(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockProduct));
        return mockProductService;
    }

    @Nonnull
    private CategoryService buildMockCategoryService() {
        final CategoryService mockCategoryService = mock(CategoryService.class);
        when(mockCategoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));
        return mockCategoryService;
    }
}
