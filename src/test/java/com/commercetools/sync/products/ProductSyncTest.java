package com.commercetools.sync.products;

import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.ProductService;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.TaxCategoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.ProductServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.PRODUCT_KEY_2_RESOURCE_PATH;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftBuilder;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSyncTest {

    @Test
    void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId("productTypeKey"))
            .taxCategory(null)
            .state(null)
            .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient mockClient = mock(SphereClient.class);
        when(mockClient.execute(any(ProductQuery.class)))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
            .of(mockClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final ProductService productService = spy(new ProductServiceImpl(syncOptions));

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchCachedProductTypeId(any()))
            .thenReturn(completedFuture(Optional.of(UUID.randomUUID().toString())));

        final CategoryService categoryService = mock(CategoryService.class);
        when(categoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));


        final ProductSync productSync = new ProductSync(syncOptions, productService,
            productTypeService, categoryService, mock(TypeService.class),
            mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
            mock(StateService.class));

        // test
        final ProductSyncStatistics productSyncStatistics = productSync
            .sync(singletonList(productDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).contains("Failed to build a cache of keys to ids.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
                ProductType.referenceOfId("productTypeKey"))
                .taxCategory(null)
                .state(null)
                .build();

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient mockClient = mock(SphereClient.class);
        when(mockClient.execute(any(ProductQuery.class)))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder
                .of(mockClient)
                .errorCallback((errorMessage, exception) -> {
                    errorMessages.add(errorMessage);
                    exceptions.add(exception);
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
        when(categoryService.fetchMatchingCategoriesByKeys(any())).thenReturn(completedFuture(emptySet()));


        final ProductSync productSync = new ProductSync(syncOptions, productService,
                productTypeService, categoryService, mock(TypeService.class),
                mock(ChannelService.class), mock(CustomerGroupService.class), mock(TaxCategoryService.class),
                mock(StateService.class));

        // test
        final ProductSyncStatistics productSyncStatistics = productSync
                .sync(singletonList(productDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message ->
                        assertThat(message).contains("Failed to fetch existing products")
            );

        assertThat(exceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });

        assertThat(productSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_2_RESOURCE_PATH,
            ProductType.referenceOfId("productTypeKey"))
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
            mock(StateService.class));

        // test
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();


        // assertion
        verify(spyProductSyncOptions).applyBeforeCreateCallBack(any());
        verify(spyProductSyncOptions, never()).applyBeforeUpdateCallBack(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final ProductDraft productDraft = createProductDraftBuilder(PRODUCT_KEY_1_WITH_PRICES_RESOURCE_PATH,
            ProductType.referenceOfId("productTypeKey"))
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
            mock(StateService.class));

        // test
        productSync.sync(singletonList(productDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductSyncOptions).applyBeforeUpdateCallBack(any(), any(), any());
        verify(spyProductSyncOptions, never()).applyBeforeCreateCallBack(any());
    }
}
