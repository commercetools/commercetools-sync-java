package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.services.ProductTypeService;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.producttypes.ProductType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void fetchMatchingProductTypesByKeys_WithBadGateWayException_ShouldFail() {
        final List<String> errorCallBackMessages = new ArrayList<>();
        final List<Throwable> errorCallBackExceptions = new ArrayList<>();

        // Mock sphere client to return BadGatewayException on any request.
        final FakeClient<ProductType> fakeProductTypeClient = new FakeClient<>(new BadGatewayException());

        final ProductTypeSyncOptions spyOptions =
                ProductTypeSyncOptionsBuilder.of(fakeProductTypeClient)
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorCallBackMessages.add(exception.getMessage());
                            errorCallBackExceptions.add(exception.getCause());
                        })
                        .build();

        final ProductTypeService spyProductTypeService = new ProductTypeServiceImpl(spyOptions);

        final Set<String> keys = new HashSet<>();
        keys.add("old_product_type_key");

        // test and assert
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(spyProductTypeService.fetchMatchingProductTypesByKeys(keys))
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(BadGatewayException.class);
    }

}