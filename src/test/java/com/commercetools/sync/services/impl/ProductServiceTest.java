package com.commercetools.sync.services.impl;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;
import io.sphere.sdk.products.ProductProjectionType;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductUpdateCommand;
import io.sphere.sdk.products.commands.updateactions.ChangeName;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

  private ProductServiceImpl service;
  private ProductSyncOptions productSyncOptions;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  @BeforeEach
  void setUp() {
    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception.getCause());
                })
            .build();
    service = new ProductServiceImpl(productSyncOptions);
  }

  @Test
  void createProduct_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
    final Product product = mock(Product.class);
    final ProductProjection mockProductProjection = mock(ProductProjection.class);

    when(product.toProjection(any())).thenReturn(mockProductProjection);
    when(product.getId()).thenReturn("productId");
    when(product.getKey()).thenReturn("productKey");

    when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(product));

    final ProductDraft draft = mock(ProductDraft.class);
    when(draft.getKey()).thenReturn("productKey");
    final Optional<ProductProjection> productOptional =
        service.createProduct(draft).toCompletableFuture().join();

    assertThat(productOptional).isNotEmpty();
    assertThat(productOptional).containsSame(product.toProjection(ProductProjectionType.STAGED));
    verify(productSyncOptions.getCtpClient()).execute(eq(ProductCreateCommand.of(draft)));
  }

  @Test
  void createProduct_WithUnSuccessfulMockCtpResponse_ShouldNotCreateProduct() {
    // preparation
    final Product mock = mock(Product.class);
    when(mock.getId()).thenReturn("productId");

    when(productSyncOptions.getCtpClient().execute(any()))
        .thenReturn(CompletableFutureUtils.failed(new BadRequestException("bad request")));

    final ProductDraft draft = mock(ProductDraft.class);
    when(draft.getKey()).thenReturn("productKey");

    // test
    final Optional<ProductProjection> productOptional =
        service.createProduct(draft).toCompletableFuture().join();

    // assertion
    assertThat(productOptional).isEmpty();
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to create draft with key: 'productKey'.")
        .contains("BadRequestException");

    assertThat(errorExceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(BadRequestException.class);
  }

  @Test
  void createProduct_WithDraftWithoutKey_ShouldNotCreateProduct() {
    final ProductDraft draft = mock(ProductDraft.class);
    final Optional<ProductProjection> productOptional =
        service.createProduct(draft).toCompletableFuture().join();

    assertThat(productOptional).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(errorExceptions).hasSize(1);
    assertThat(errorMessages.get(0))
        .isEqualTo("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
  }

  @Test
  void updateProduct_WithMockCtpResponse_ShouldReturnMock() {
    final ProductProjection productProjectionMock = mock(ProductProjection.class);
    when(productProjectionMock.getKey()).thenReturn("anyKey");
    final Product product = mock(Product.class);
    when(product.toProjection(any())).thenReturn(productProjectionMock);

    when(productSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(product));

    final List<UpdateAction<Product>> updateActions =
        singletonList(ChangeName.of(LocalizedString.of(ENGLISH, "new name")));
    final ProductProjection productProjection =
        service.updateProduct(productProjectionMock, updateActions).toCompletableFuture().join();

    assertThat(productProjection).isSameAs(productProjectionMock);
    verify(productSyncOptions.getCtpClient())
        .execute(eq(ProductUpdateCommand.of(productProjectionMock, updateActions)));
  }
}
