package com.commercetools.sync.services.impl;

import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductTypeServiceImplTest {

    @Test
    public void fetchProductType_WithEmptyKey_ShouldNotFetchProductType() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();
        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType("");

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        verify(sphereClient, never()).execute(any());
    }

    @Test
    public void fetchProductType_WithNullKey_ShouldNotFetchProductType() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();
        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType(null);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        verify(sphereClient, never()).execute(any());
    }

    @Test
    public void fetchProductType_WithBadGateWayException_ShouldCompleteExceptionally() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        when(sphereClient.execute(any(ProductTypeQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));


        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(sphereClient)
            .build();
        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType("foo");

        // assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
    }

}