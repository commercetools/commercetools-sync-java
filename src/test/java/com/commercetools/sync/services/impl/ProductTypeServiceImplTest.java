package com.commercetools.sync.services.impl;

import com.commercetools.sync.internals.helpers.CustomHeaderSphereClientDecorator;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductTypeServiceImplTest {

    @Test
    void fetchProductType_WithEmptyKey_ShouldNotFetchProductType() {
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
    void fetchProductType_WithNullKey_ShouldNotFetchProductType() {
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
    void fetchProductType_WithBadGateWayException_ShouldCompleteExceptionally() {
        // preparation
        final SphereClient sphereClient = mock(SphereClient.class);
        final SphereClient mockDecoratedClient = mock(CustomHeaderSphereClientDecorator.class);
        final ProductTypeSyncOptions syncOptions = spy(ProductTypeSyncOptionsBuilder
                .of(sphereClient)
                .build());
        when(syncOptions.getCtpClient()).thenReturn(mockDecoratedClient);
        when(mockDecoratedClient.execute(any(ProductTypeQuery.class)))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType("foo");

        // assertions
        assertThat(result).hasFailedWithThrowableThat().isExactlyInstanceOf(BadGatewayException.class);
    }

}