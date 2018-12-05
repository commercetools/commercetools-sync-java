package com.commercetools.sync.producttypes;

import com.commercetools.sync.producttypes.helpers.ProductTypeSyncStatistics;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductTypeDraftBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.emptyList;
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

public class ProductTypeSyncTest {
    @Test
    public void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
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

        assertThat(productTypeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    public void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraftBuilder
            .of("newProductType", "productType", "a cool type", emptyList())
            .build();

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchMatchingProductTypesByKeys(anySet())).thenReturn(completedFuture(emptySet()));
        when(productTypeService.createProductType(any())).thenReturn(completedFuture(Optional.empty()));

        final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        // test
        new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
            .sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductTypeSyncOptions).applyBeforeCreateCallBack(newProductTypeDraft);
        verify(spyProductTypeSyncOptions, never()).applyBeforeUpdateCallBack(any(), any(), any());
    }

    @Test
    public void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final ProductTypeDraft newProductTypeDraft = ProductTypeDraftBuilder
            .of("newProductType", "productType", "a cool type", emptyList())
            .build();

        final ProductTypeSyncOptions productTypeSyncOptions = ProductTypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final ProductType mockedExistingProductType = mock(ProductType.class);
        when(mockedExistingProductType.getKey()).thenReturn(newProductTypeDraft.getKey());

        final ProductTypeService productTypeService = mock(ProductTypeService.class);
        when(productTypeService.fetchMatchingProductTypesByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockedExistingProductType)));
        when(productTypeService.updateProductType(any(), any())).thenReturn(completedFuture(mockedExistingProductType));

        final ProductTypeSyncOptions spyProductTypeSyncOptions = spy(productTypeSyncOptions);

        // test
        new ProductTypeSync(spyProductTypeSyncOptions, productTypeService)
            .sync(singletonList(newProductTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyProductTypeSyncOptions).applyBeforeUpdateCallBack(any(), any(), any());
        verify(spyProductTypeSyncOptions, never()).applyBeforeCreateCallBack(newProductTypeDraft);
    }

}
