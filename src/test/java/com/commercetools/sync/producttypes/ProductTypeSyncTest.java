package com.commercetools.sync.producttypes;

import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import com.commercetools.sync.services.impl.ProductTypeServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraft;
import io.sphere.sdk.products.attributes.AttributeDefinitionDraftBuilder;
import io.sphere.sdk.products.attributes.NestedAttributeType;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeUpdateCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.queries.PagedQueryResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
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

class ProductTypeSyncTest {

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            emptyList()
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = mock(ProductTypeService.class);

        when(mockProductTypeService.fetchMatchingProductTypesByKeys(singleton(newProductTypeDraft.getKey())))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));
        when(mockProductTypeService.cacheKeysToIds(anySet()))
            .thenReturn(CompletableFuture.completedFuture(emptyMap()));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to fetch existing product types with keys: '[foo]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithErrorCachingKeysButNoKeysToCache_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation

        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            emptyList()
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = new ProductTypeServiceImpl(syncOptions);


        when(sphereClient.execute(any(ProductTypeQuery.class)))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to fetch existing product types with keys: '[foo]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithErrorCachingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation
        final AttributeDefinitionDraft nestedTypeAttrDefDraft = AttributeDefinitionDraftBuilder
            .of(NestedAttributeType.of(ProductType.referenceOfId("x")), "validNested", ofEnglish("koko"), true)
            .build();


        final ProductTypeDraft newProductTypeDraft = ProductTypeDraft.ofAttributeDefinitionDrafts(
            "foo",
            "name",
            "desc",
            singletonList(nestedTypeAttrDefDraft)
        );

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();


        final ProductTypeService mockProductTypeService = new ProductTypeServiceImpl(syncOptions);


        when(sphereClient.execute(any(ProductTypeQuery.class)))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final ProductTypeSync productTypeSync = new ProductTypeSync(syncOptions, mockProductTypeService);

        // test
        final ProductTypeSyncStatistics productTypeSyncStatistics = productTypeSync
            .sync(singletonList(newProductTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(message ->
                assertThat(message).isEqualTo("Failed to build a cache of keys to ids.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .hasOnlyOneElementSatisfying(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1, 0);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraftBuilder
            .of("newProductType", "productType", "a cool type", emptyList())
            .build();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();

        final ProductTypeService productTypeService = new ProductTypeServiceImpl(productTypeSyncOptions);
        when(sphereClient.execute(any(ProductTypeQuery.class))).thenReturn(completedFuture(PagedQueryResult.empty()));
        final ProductType createdProductType = mock(ProductType.class);
        when(createdProductType.getKey()).thenReturn(newProductTypeDraft.getKey());
        when(createdProductType.getId()).thenReturn(UUID.randomUUID().toString());
        when(sphereClient.execute(any(ProductTypeCreateCommand.class))).thenReturn(completedFuture(createdProductType));

        final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        // test
        new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
            .sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductTypeSyncOptions).applyBeforeCreateCallBack(newProductTypeDraft);
        verify(spyProductTypeSyncOptions, never()).applyBeforeUpdateCallBack(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraftBuilder
            .of("newProductType", "productType", "a cool type", emptyList())
            .build();

        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();

        final ProductType mockedExistingProductType = mock(ProductType.class);
        when(mockedExistingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());
        when(mockedExistingProductType.getId()).thenReturn(UUID.randomUUID().toString());

        final ProductTypeService productTypeService = new ProductTypeServiceImpl(productTypeSyncOptions);
        final PagedQueryResult<ProductType> productTypePagedQueryResult = spy(PagedQueryResult.empty());
        when(productTypePagedQueryResult.getResults()).thenReturn(singletonList(mockedExistingProductType));
        when(sphereClient.execute(any(ProductTypeQuery.class)))
            .thenReturn(completedFuture(productTypePagedQueryResult));
        when(sphereClient.execute(any(ProductTypeUpdateCommand.class)))
            .thenReturn(completedFuture(mockedExistingProductType));

        final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        // test
        new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
            .sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductTypeSyncOptions).applyBeforeUpdateCallBack(any(), any(), any());
        verify(spyProductTypeSyncOptions, never()).applyBeforeCreateCallBack(newProductTypeDraft);
    }

}
