package com.commercetools.sync.sdk2.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyProductTypesKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyProductTypesKeyByKeyRequestBuilder;
import com.commercetools.api.client.ByProjectKeyProductTypesRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptions;
import com.commercetools.sync.sdk2.producttypes.ProductTypeSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.ProductTypeService;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ProductTypeServiceImplTest {

  @Test
  void fetchProductType_WithEmptyKey_ShouldNotFetchProductType() {
    // preparation
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(ctpClient.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesKeyByKeyRequestBuilder
        byProjectKeyProductTypesKeyByKeyRequestBuilder = mock();
    when(byProjectKeyProductTypesRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyProductTypesKeyByKeyRequestBuilder);
    final ByProjectKeyProductTypesKeyByKeyGet byProjectKeyProductTypesKeyByKeyGet = mock();
    when(byProjectKeyProductTypesKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyProductTypesKeyByKeyGet);
    when(byProjectKeyProductTypesKeyByKeyGet.execute()).thenReturn(mock());
    final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder.of(ctpClient).build();
    final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

    // test
    final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType("");

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    verify(byProjectKeyProductTypesKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchProductType_WithNullKey_ShouldNotFetchProductType() {
    // preparation
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(ctpClient.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesKeyByKeyRequestBuilder
        byProjectKeyProductTypesKeyByKeyRequestBuilder = mock();
    when(byProjectKeyProductTypesRequestBuilder.withKey(null))
        .thenReturn(byProjectKeyProductTypesKeyByKeyRequestBuilder);
    final ByProjectKeyProductTypesKeyByKeyGet byProjectKeyProductTypesKeyByKeyGet = mock();
    when(byProjectKeyProductTypesKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyProductTypesKeyByKeyGet);
    when(byProjectKeyProductTypesKeyByKeyGet.execute()).thenReturn(mock());
    final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder.of(ctpClient).build();
    final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

    // test
    final CompletionStage<Optional<ProductType>> result = productTypeService.fetchProductType(null);

    // assertions
    assertThat(result).isCompletedWithValue(Optional.empty());
    verify(byProjectKeyProductTypesKeyByKeyGet, never()).execute();
  }

  @Test
  void fetchProductType_WithBadGateWayException_ShouldCompleteExceptionally() {
    // preparation
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);
    final ByProjectKeyProductTypesRequestBuilder byProjectKeyProductTypesRequestBuilder = mock();
    when(ctpClient.productTypes()).thenReturn(byProjectKeyProductTypesRequestBuilder);
    final ByProjectKeyProductTypesKeyByKeyRequestBuilder
        byProjectKeyProductTypesKeyByKeyRequestBuilder = mock();
    when(byProjectKeyProductTypesRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyProductTypesKeyByKeyRequestBuilder);
    final ByProjectKeyProductTypesKeyByKeyGet byProjectKeyProductTypesKeyByKeyGet = mock();
    when(byProjectKeyProductTypesKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyProductTypesKeyByKeyGet);
    final BadGatewayException badGatewayException =
        new BadGatewayException(500, "", null, "Failed request", null);
    when(byProjectKeyProductTypesKeyByKeyGet.execute())
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(badGatewayException));

    final ProductTypeSyncOptions syncOptions = ProductTypeSyncOptionsBuilder.of(ctpClient).build();
    final ProductTypeService productTypeService = new ProductTypeServiceImpl(syncOptions);

    // test
    final CompletionStage<Optional<ProductType>> result =
        productTypeService.fetchProductType("foo");

    // assertions
    assertThat(result)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }
}
