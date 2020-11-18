package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.producttypes.ProductType;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductTypeServiceImplTest {

    @Test
    void fetchProductType_WithEmptyKey_ShouldNotFetchProductType() {
        // preparation
        final FakeClient<ProductType> fakeProductTypeClient = new FakeClient(mock(ProductType.class));
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(fakeProductTypeClient)
            .build();
        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType("");

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(fakeProductTypeClient.isExecuted()).isFalse();
    }

    @Test
    void fetchProductType_WithNullKey_ShouldNotFetchProductType() {
        // preparation
        final FakeClient<ProductType> fakeProductTypeClient = new FakeClient(mock(ProductType.class));
        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(fakeProductTypeClient)
            .build();
        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType(null);

        // assertions
        assertThat(result).isCompletedWithValue(Optional.empty());
        assertThat(fakeProductTypeClient.isExecuted()).isFalse();
    }

    @Test
    void fetchProductType_WithBadGateWayException_ShouldCompleteExceptionally() {
        // preparation
        final FakeClient<ProductType> fakeProductTypeClient = new FakeClient(new BadGatewayException());

        final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder
            .of(fakeProductTypeClient)
            .build();
        final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

        // test
        final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType("foo");

        // assertions
        assertThat(result)
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

}