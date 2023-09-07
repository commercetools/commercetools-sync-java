package com.commercetools.sync.services.impl;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyProductsByIDPost;
import com.commercetools.api.client.ByProjectKeyProductsPost;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.api.models.product.Product;
import com.commercetools.api.models.product.ProductChangeNameActionBuilder;
import com.commercetools.api.models.product.ProductData;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductMixin;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionType;
import com.commercetools.api.models.product.ProductUpdate;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product_type.ProductTypeReference;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
        ProductSyncOptionsBuilder.of(mock())
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
    final Product product = mock();
    final ProductData productData = mock(ProductData.class);
    final ZonedDateTime zonedDateTime = ZonedDateTime.now();
    final ProductTypeReference productTypeReference = mock(ProductTypeReference.class);
    final LocalizedString localizedString = mock(LocalizedString.class);
    final List list = mock(List.class);
    final ProductVariant productVariant = mock(ProductVariant.class);

    when(product.getMasterData()).thenReturn(mock());
    when(product.getMasterData().getStaged()).thenReturn(productData);
    when(product.getMasterData().get(any(ProductProjectionType.class))).thenReturn(productData);
    when(product.getId()).thenReturn("productId");
    when(product.getKey()).thenReturn("productKey");
    when(product.getVersion()).thenReturn(1L);
    when(product.getCreatedAt()).thenReturn(zonedDateTime);
    when(product.getLastModifiedAt()).thenReturn(zonedDateTime);

    when(product.getProductType()).thenReturn(productTypeReference);
    when(productData.getName()).thenReturn(localizedString);
    when(productData.getSlug()).thenReturn(localizedString);
    when(productData.getCategories()).thenReturn(list);
    when(productData.getMasterVariant()).thenReturn(productVariant);
    when(productData.getVariants()).thenReturn(list);

    final ApiHttpResponse<Product> response = mock(ApiHttpResponse.class);
    when(response.getBody()).thenReturn(product);

    final ByProjectKeyProductsPost byProjectKeyProductsPost = mock(ByProjectKeyProductsPost.class);

    when(productSyncOptions.getCtpClient().products()).thenReturn(mock());
    when(productSyncOptions.getCtpClient().products().create(any(ProductDraft.class)))
        .thenReturn(byProjectKeyProductsPost);
    when(byProjectKeyProductsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(response));

    final ProductDraft draft = mock(ProductDraft.class);
    when(draft.getKey()).thenReturn("productKey");
    final Optional<ProductProjection> productOptional =
        service.createProduct(draft).toCompletableFuture().join();

    assertThat(productOptional).isNotEmpty();

    assertThat(productOptional.get())
        .isEqualTo(ProductMixin.toProjection(product, ProductProjectionType.STAGED));
    verify(byProjectKeyProductsPost, times(1)).execute();
  }

  @Test
  void createProduct_WithUnsuccessfulMockCtpResponse_ShouldNotCreateProduct()
      throws JsonProcessingException {
    // preparation
    final Product mock = mock(Product.class);
    when(mock.getId()).thenReturn("productId");

    when(productSyncOptions.getCtpClient().products()).thenReturn(mock());

    final ByProjectKeyProductsPost byProjectKeyProductsPost = mock(ByProjectKeyProductsPost.class);
    when(productSyncOptions.getCtpClient().products().create(any(ProductDraft.class)))
        .thenReturn(byProjectKeyProductsPost);

    final ApiHttpResponse<Object> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(null);
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(409)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    final String json = ow.writeValueAsString(errorResponse);
    when(byProjectKeyProductsPost.execute())
        .thenReturn(
            CompletableFutureUtils.failed(
                new ConcurrentModificationException(
                    409,
                    "",
                    null,
                    "",
                    new ApiHttpResponse<>(409, null, json.getBytes(StandardCharsets.UTF_8)))));

    final ProductDraft draft = mock();
    when(draft.getKey()).thenReturn("productKey");

    // test
    final Optional<ProductProjection> productOptional =
        service.createProduct(draft).toCompletableFuture().join();

    // assertion
    assertAll(
        () -> assertThat(productOptional).isEmpty(),
        () -> assertThat(errorMessages).hasSize(1),
        () ->
            assertThat(errorMessages)
                .singleElement(as(STRING))
                .contains("Failed to create draft with key: 'productKey'.")
                .contains("ApiHttpResponse"),
        () -> assertThat(errorExceptions).hasSize(1),
        () ->
            assertThat(errorExceptions)
                .singleElement(as(THROWABLE))
                .isExactlyInstanceOf(ConcurrentModificationException.class));
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
    final Product product = mock();
    final ProductData productData = mock(ProductData.class);
    final ZonedDateTime zonedDateTime = ZonedDateTime.now();
    final ProductTypeReference productTypeReference = mock(ProductTypeReference.class);
    final LocalizedString localizedString = mock(LocalizedString.class);
    final List list = mock(List.class);
    final ProductVariant productVariant = mock(ProductVariant.class);

    when(product.getMasterData()).thenReturn(mock());
    when(product.getMasterData().getStaged()).thenReturn(productData);
    when(product.getMasterData().get(any(ProductProjectionType.class))).thenReturn(productData);
    when(product.getId()).thenReturn("productId");
    when(product.getKey()).thenReturn("productKey");
    when(product.getVersion()).thenReturn(1L);
    when(product.getCreatedAt()).thenReturn(zonedDateTime);
    when(product.getLastModifiedAt()).thenReturn(zonedDateTime);

    when(product.getProductType()).thenReturn(productTypeReference);
    when(productData.getName()).thenReturn(localizedString);
    when(productData.getSlug()).thenReturn(localizedString);
    when(productData.getCategories()).thenReturn(list);
    when(productData.getMasterVariant()).thenReturn(productVariant);
    when(productData.getVariants()).thenReturn(list);

    final ApiHttpResponse<Product> response = mock(ApiHttpResponse.class);
    when(response.getBody()).thenReturn(product);

    final ByProjectKeyProductsByIDPost byProjectKeyProductsPost =
        mock(ByProjectKeyProductsByIDPost.class);

    when(productSyncOptions.getCtpClient().products()).thenReturn(mock());
    when(productSyncOptions.getCtpClient().products().withId(anyString())).thenReturn(mock());
    when(productSyncOptions
            .getCtpClient()
            .products()
            .withId(anyString())
            .post(any(ProductUpdate.class)))
        .thenReturn(byProjectKeyProductsPost);
    when(byProjectKeyProductsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(response));

    final ProductDraft draft = mock(ProductDraft.class);
    when(draft.getKey()).thenReturn("productKey");

    final List<ProductUpdateAction> updateActions =
        singletonList(
            ProductChangeNameActionBuilder.of()
                .name(LocalizedString.of(ENGLISH, "new name"))
                .build());
    final ProductProjection productProjection =
        service
            .updateProduct(
                ProductMixin.toProjection(product, ProductProjectionType.STAGED), updateActions)
            .toCompletableFuture()
            .join();

    assertThat(productProjection)
        .isEqualTo(ProductMixin.toProjection(product, ProductProjectionType.STAGED));
    verify(byProjectKeyProductsPost, times(1)).execute();
  }
}
