package com.commercetools.sync.services.impl;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.*;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponseBuilder;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product_type.ProductTypeResourceIdentifierBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolvedProducts;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
class UnresolvedReferencesServiceImplTest {

  private UnresolvedReferencesServiceImpl<WaitingToBeResolvedProducts> service;
  private ProductSyncOptions productSyncOptions;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  @BeforeEach
  void setUp() {
    final ProjectApiRoot apiRoot = mock(ProjectApiRoot.class);
    when(apiRoot.customObjects()).thenReturn(mock());
    when(apiRoot.customObjects().withContainer(anyString())).thenReturn(mock());
    when(apiRoot.customObjects().withContainerAndKey(anyString(), anyString())).thenReturn(mock());
    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    productSyncOptions =
        ProductSyncOptionsBuilder.of(apiRoot)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception);
                })
            .build();

    service = new UnresolvedReferencesServiceImpl<>(productSyncOptions);
  }

  @Test
  void fetch_WithEmptyKeySet_ShouldReturnEmptySet() {
    // preparation
    final Set<String> keys = new HashSet<>();

    // test
    final Set<WaitingToBeResolvedProducts> result =
        service
            .fetch(
                keys,
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(result).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  void fetch_OnSuccess_ShouldReturnMock() {
    // preparation
    final CustomObject customObjectMock = mock(CustomObject.class);
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key("product-draft-key")
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();

    final WaitingToBeResolvedProducts waitingToBeResolved =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));
    when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

    final ApiHttpResponse<CustomObjectPagedQueryResponse> result =
        getMockCustomObjectPagedQueryResponse(singletonList(customObjectMock));
    final ByProjectKeyCustomObjectsByContainerGet customObjectGet =
        mock(ByProjectKeyCustomObjectsByContainerGet.class);
    when(customObjectGet.withWhere(anyString())).thenReturn(customObjectGet);
    when(customObjectGet.withPredicateVar(anyString(), anyCollection()))
        .thenReturn(customObjectGet);
    when(customObjectGet.execute()).thenReturn(CompletableFuture.completedFuture(result));
    when(productSyncOptions.getCtpClient().customObjects().withContainer(anyString()).get())
        .thenReturn(customObjectGet);

    // test
    final Set<WaitingToBeResolvedProducts> toBeResolvedOptional =
        service
            .fetch(
                singleton("product-draft-key"),
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(toBeResolvedOptional).containsOnly(waitingToBeResolved);
  }

  @SuppressWarnings("unchecked")
  @Test
  void fetch_OnSuccess_ShouldRequestHashedKeys() {
    // preparation
    final CustomObject customObjectMock = mock(CustomObject.class);
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key("product-draft-key")
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();

    final WaitingToBeResolvedProducts waitingToBeResolved =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));
    when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

    final ApiHttpResponse<CustomObjectPagedQueryResponse> result =
        getMockCustomObjectPagedQueryResponse(singletonList(customObjectMock));
    final ByProjectKeyCustomObjectsByContainerGet customObjectGet =
        mock(ByProjectKeyCustomObjectsByContainerGet.class);
    when(customObjectGet.withWhere(anyString())).thenReturn(customObjectGet);
    when(customObjectGet.withPredicateVar(anyString(), anyCollection()))
        .thenReturn(customObjectGet);
    when(customObjectGet.execute()).thenReturn(CompletableFuture.completedFuture(result));
    when(productSyncOptions.getCtpClient().customObjects().withContainer(anyString()).get())
        .thenReturn(customObjectGet);

    final ArgumentCaptor<List<String>> requestArgumentCaptor = ArgumentCaptor.forClass(List.class);

    // test
    final Set<String> setOfSpecialCharKeys = new HashSet<>();
    setOfSpecialCharKeys.addAll(
        Arrays.asList(
            "Get a $100 Visa® Reward Card because you’re ordering TV",
            "product$",
            "Visa®",
            "Visa©"));
    final Set<WaitingToBeResolvedProducts> toBeResolvedOptional =
        service
            .fetch(
                setOfSpecialCharKeys,
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    verify(customObjectGet).withPredicateVar(anyString(), requestArgumentCaptor.capture());
    assertThat(toBeResolvedOptional).containsOnly(waitingToBeResolved);
    setOfSpecialCharKeys.forEach(
        key -> assertThat(requestArgumentCaptor.getValue()).contains(sha1Hex(key)));
  }

  @Test
  void save_OnSuccess_ShouldSaveMock() {
    // preparation
    final CustomObject customObjectMock = mock(CustomObject.class);
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key("product-draft-key")
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();

    final WaitingToBeResolvedProducts waitingToBeResolved =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));
    when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

    final ApiHttpResponse<CustomObject> coResponse = getMockCustomObjectResponse(customObjectMock);
    final ByProjectKeyCustomObjectsPost customObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    when(customObjectsPost.execute()).thenReturn(CompletableFuture.completedFuture(coResponse));
    when(productSyncOptions.getCtpClient().customObjects().post(any(CustomObjectDraft.class)))
        .thenReturn(customObjectsPost);

    // test
    final Optional<WaitingToBeResolvedProducts> result =
        service
            .save(
                waitingToBeResolved,
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(result).contains(waitingToBeResolved);
  }

  @SuppressWarnings("unchecked")
  @Test
  void save_OnSuccess_ShouldSaveMockWithSha1HashedKey() {
    // preparation
    final CustomObject customObjectMock = mock(CustomObject.class);
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key("product-draft-key")
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();

    final WaitingToBeResolvedProducts waitingToBeResolved =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));
    when(customObjectMock.getValue()).thenReturn(waitingToBeResolved);

    final ApiHttpResponse<CustomObject> coResponse = getMockCustomObjectResponse(customObjectMock);
    final ByProjectKeyCustomObjectsPost customObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    when(customObjectsPost.execute()).thenReturn(CompletableFuture.completedFuture(coResponse));
    final ByProjectKeyCustomObjectsRequestBuilder requestBuilder =
        mock(ByProjectKeyCustomObjectsRequestBuilder.class);
    when(requestBuilder.post(any(CustomObjectDraft.class))).thenReturn(customObjectsPost);
    when(productSyncOptions.getCtpClient().customObjects()).thenReturn(requestBuilder);

    final ArgumentCaptor<CustomObjectDraft> requestArgumentCaptor =
        ArgumentCaptor.forClass(CustomObjectDraft.class);

    // test
    final Optional<WaitingToBeResolvedProducts> result =
        service
            .save(
                waitingToBeResolved,
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    verify(requestBuilder).post(requestArgumentCaptor.capture());
    assertThat(result).contains(waitingToBeResolved);
    assertThat(requestArgumentCaptor.getValue().getKey())
        .isEqualTo(sha1Hex(productDraftMock.getKey()));
  }

  @Test
  void save_WithUnsuccessfulMockCtpResponse_ShouldNotSaveMock() throws JsonProcessingException {
    // preparation
    final String productKey = "product-draft-key";
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key(productKey)
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();
    final WaitingToBeResolvedProducts waitingToBeResolved =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock());
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(400)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    final String json = ow.writeValueAsString(errorResponse);

    final ByProjectKeyCustomObjectsPost customObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    when(customObjectsPost.execute())
        .thenReturn(
            CompletableFutureUtils.failed(
                new BadRequestException(
                    400,
                    "",
                    null,
                    "bad request",
                    new ApiHttpResponse<>(400, null, json.getBytes(StandardCharsets.UTF_8)))));
    when(productSyncOptions.getCtpClient().customObjects().post(any(CustomObjectDraft.class)))
        .thenReturn(customObjectsPost);

    // test
    final Optional<WaitingToBeResolvedProducts> result =
        service
            .save(
                waitingToBeResolved,
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(result).isEmpty();
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            format(
                "Failed to save CustomObject with key: '%s' (hash of product key: '%s').",
                sha1Hex(productKey), productKey));

    assertThat(errorExceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(BadRequestException.class);
  }

  @Test
  void delete_WithUnsuccessfulMockCtpResponse_ShouldReturnProperException()
      throws JsonProcessingException {
    // preparation
    final String key = "product-draft-key";
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key(key)
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock());
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(400)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    final String json = ow.writeValueAsString(errorResponse);

    final ByProjectKeyCustomObjectsByContainerByKeyDelete customObjectsDelete =
        mock(ByProjectKeyCustomObjectsByContainerByKeyDelete.class);
    when(customObjectsDelete.execute())
        .thenReturn(
            CompletableFutureUtils.failed(
                new BadRequestException(
                    400,
                    "",
                    null,
                    "bad request",
                    new ApiHttpResponse<>(400, null, json.getBytes(StandardCharsets.UTF_8)))));
    when(productSyncOptions
            .getCtpClient()
            .customObjects()
            .withContainerAndKey(anyString(), anyString())
            .delete())
        .thenReturn(customObjectsDelete);

    // test
    final Optional<WaitingToBeResolvedProducts> toBeResolvedOptional =
        service
            .delete(
                "product-draft-key",
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(toBeResolvedOptional).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(errorExceptions).hasSize(1);
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            format(
                "Failed to delete CustomObject with key: '%s' (hash of product key: '%s')",
                sha1Hex(key), key));
    assertThat(errorExceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(BadRequestException.class);
  }

  @Test
  void delete_With404NotFoundResponse_ShouldConsiderAsDeleted() throws JsonProcessingException {
    // preparation
    final String key = "product-draft-key";
    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key(key)
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();

    final ApiHttpResponse<State> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(mock());
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(404)
            .errors(Collections.emptyList())
            .message("test")
            .build();

    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    final String json = ow.writeValueAsString(errorResponse);

    final ByProjectKeyCustomObjectsByContainerByKeyDelete customObjectsDelete =
        mock(ByProjectKeyCustomObjectsByContainerByKeyDelete.class);
    when(customObjectsDelete.execute())
        .thenReturn(
            CompletableFutureUtils.failed(
                new NotFoundException(
                    404,
                    "",
                    null,
                    "not found",
                    new ApiHttpResponse<>(404, null, json.getBytes(StandardCharsets.UTF_8)))));
    when(productSyncOptions
            .getCtpClient()
            .customObjects()
            .withContainerAndKey(anyString(), anyString())
            .delete())
        .thenReturn(customObjectsDelete);

    // test
    final Optional<WaitingToBeResolvedProducts> toBeResolvedOptional =
        service
            .delete(
                "product-draft-key",
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(toBeResolvedOptional).isEmpty();
    assertThat(errorMessages).hasSize(0);
    assertThat(errorExceptions).hasSize(0);
  }

  @SuppressWarnings("unchecked")
  @Test
  void delete_OnSuccess_ShouldRemoveTheResourceObject() {
    // preparation
    final CustomObject customObjectMock = mock(CustomObject.class);

    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key("product-draft-key")
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();
    final WaitingToBeResolvedProducts waitingDraft =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));
    when(customObjectMock.getValue()).thenReturn(waitingDraft);

    ApiHttpResponse<CustomObject> mockCustomObjectResponse =
        getMockCustomObjectResponse(customObjectMock);
    final ByProjectKeyCustomObjectsByContainerByKeyDelete customObjectsDelete =
        mock(ByProjectKeyCustomObjectsByContainerByKeyDelete.class);
    when(customObjectsDelete.execute())
        .thenReturn(CompletableFuture.completedFuture(mockCustomObjectResponse));
    when(productSyncOptions
            .getCtpClient()
            .customObjects()
            .withContainerAndKey(anyString(), anyString())
            .delete())
        .thenReturn(customObjectsDelete);

    // test
    final Optional<WaitingToBeResolvedProducts> toBeResolvedOptional =
        service
            .delete(
                "product-draft-key",
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(toBeResolvedOptional).contains(waitingDraft);
  }

  @SuppressWarnings("unchecked")
  @Test
  void delete_OnSuccess_ShouldMakeRequestWithSha1HashedKey() {
    // preparation
    final CustomObject customObjectMock = mock(CustomObject.class);

    final ProductDraft productDraftMock =
        ProductDraftBuilder.of()
            .productType(ProductTypeResourceIdentifierBuilder.of().key("product-type").build())
            .key("product-draft-key")
            .name(LocalizedString.ofEnglish("product-name"))
            .slug(LocalizedString.ofEnglish("product-slug"))
            .build();
    final WaitingToBeResolvedProducts waitingDraft =
        new WaitingToBeResolvedProducts(productDraftMock, singleton("test-ref"));
    when(customObjectMock.getValue()).thenReturn(waitingDraft);

    ApiHttpResponse<CustomObject> mockCustomObjectResponse =
        getMockCustomObjectResponse(customObjectMock);
    final ByProjectKeyCustomObjectsByContainerByKeyDelete customObjectsDelete =
        mock(ByProjectKeyCustomObjectsByContainerByKeyDelete.class);
    when(customObjectsDelete.execute())
        .thenReturn(CompletableFuture.completedFuture(mockCustomObjectResponse));
    final ByProjectKeyCustomObjectsRequestBuilder requestBuilder =
        mock(ByProjectKeyCustomObjectsRequestBuilder.class);
    final ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder requestBuilderByContainerByKey =
        mock(ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder.class);
    when(requestBuilderByContainerByKey.delete()).thenReturn(customObjectsDelete);
    when(requestBuilder.withContainerAndKey(anyString(), anyString()))
        .thenReturn(requestBuilderByContainerByKey);
    when(productSyncOptions.getCtpClient().customObjects()).thenReturn(requestBuilder);

    final ArgumentCaptor<String> requestArgumentCaptor = ArgumentCaptor.forClass(String.class);

    // test
    final Optional<WaitingToBeResolvedProducts> toBeResolvedOptional =
        service
            .delete(
                "product-draft-key",
                UnresolvedReferencesServiceImpl.CUSTOM_OBJECT_PRODUCT_CONTAINER_KEY,
                WaitingToBeResolvedProducts.class)
            .toCompletableFuture()
            .join();

    // assertions
    // Solve spotbugs @ChangeReturnValue violation
    final ByProjectKeyCustomObjectsByContainerByKeyRequestBuilder
        byProjectKeyCustomObjectsByContainerByKeyRequestBuilder =
            verify(requestBuilder)
                .withContainerAndKey(anyString(), requestArgumentCaptor.capture());
    assertThat(toBeResolvedOptional).contains(waitingDraft);
    assertThat(requestArgumentCaptor.getValue()).contains(sha1Hex(productDraftMock.getKey()));
  }

  @Nonnull
  private ApiHttpResponse<CustomObjectPagedQueryResponse> getMockCustomObjectPagedQueryResponse(
      @Nonnull final List<CustomObject> results) {
    final ApiHttpResponse<CustomObjectPagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    final CustomObjectPagedQueryResponse pagedQueryResponse =
        CustomObjectPagedQueryResponseBuilder.of()
            .results(results)
            .limit(1L)
            .offset(0L)
            .count(1L)
            .build();
    when(apiHttpResponse.getBody()).thenReturn(pagedQueryResponse);
    return apiHttpResponse;
  }

  @Nonnull
  private ApiHttpResponse<CustomObject> getMockCustomObjectResponse(
      @Nonnull final CustomObject result) {
    final ApiHttpResponse<CustomObject> apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(result);
    return apiHttpResponse;
  }
}
