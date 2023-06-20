package com.commercetools.sync.sdk2.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyCustomObjectsByContainerByKeyGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsPost;
import com.commercetools.api.client.ByProjectKeyCustomObjectsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.ConcurrentModificationException;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.api.models.custom_object.CustomObjectPagedQueryResponse;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class CustomObjectServiceImplTest {

  private final ProjectApiRoot client = mock(ProjectApiRoot.class);

  private CustomObjectServiceImpl service;

  private String customObjectId;
  private String customObjectContainer;
  private String customObjectKey;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  @BeforeEach
  void setup() {
    customObjectId = RandomStringUtils.random(15, true, true);
    customObjectContainer = RandomStringUtils.random(15, true, true);
    customObjectKey = RandomStringUtils.random(15, true, true);
    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(client)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  errorExceptions.add(exception.getCause());
                })
            .build();

    service = new CustomObjectServiceImpl(customObjectSyncOptions);
  }

  @AfterEach
  void cleanup() {
    reset(client);
  }

  @Test
  void fetchCachedCustomObjectId_WithKeyAndContainer_ShouldFetchCustomObject() {
    final String key = RandomStringUtils.random(15, true, true);
    final String container = RandomStringUtils.random(15, true, true);
    final String id = RandomStringUtils.random(15, true, true);

    final CustomObject mock = mock(CustomObject.class);
    when(mock.getId()).thenReturn(id);
    when(mock.getContainer()).thenReturn(container);
    when(mock.getKey()).thenReturn(key);

    final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet =
        mock(ByProjectKeyCustomObjectsGet.class);

    when(client.customObjects()).thenReturn(mock(ByProjectKeyCustomObjectsRequestBuilder.class));
    when(client.customObjects().get()).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withPredicateVar(anyString(), anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet, byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withLimit(anyInt())).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withSort(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withSort(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);

    final ApiHttpResponse<CustomObjectPagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    final CustomObjectPagedQueryResponse customObjectPagedQueryResponse =
        mock(CustomObjectPagedQueryResponse.class);
    when(byProjectKeyCustomObjectsGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(apiHttpResponse.getBody()).thenReturn(customObjectPagedQueryResponse);
    when(customObjectPagedQueryResponse.getResults()).thenReturn(Collections.singletonList(mock));

    final Optional<String> fetchedId =
        service
            .fetchCachedCustomObjectId(CustomObjectCompositeIdentifier.of(key, container))
            .toCompletableFuture()
            .join();

    assertThat(fetchedId).contains(id);
    verify(byProjectKeyCustomObjectsGet).execute();
  }

  @Test
  void fetchMatchingCustomObjects_WithKeySet_ShouldFetchCustomObjects() {
    final String key1 = RandomStringUtils.random(15, true, true);
    final String key2 = RandomStringUtils.random(15, true, true);
    final String container1 = RandomStringUtils.random(15, true, true);
    final String container2 = RandomStringUtils.random(15, true, true);

    final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers = new HashSet<>();
    customObjectCompositeIdentifiers.add(CustomObjectCompositeIdentifier.of(key1, container1));
    customObjectCompositeIdentifiers.add(CustomObjectCompositeIdentifier.of(key2, container2));

    final CustomObject mock1 = mock(CustomObject.class);
    when(mock1.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock1.getKey()).thenReturn(key1);
    when(mock1.getContainer()).thenReturn(container1);

    final CustomObject mock2 = mock(CustomObject.class);
    when(mock2.getId()).thenReturn(RandomStringUtils.random(15));
    when(mock2.getKey()).thenReturn(key2);
    when(mock2.getContainer()).thenReturn(container2);

    final ByProjectKeyCustomObjectsGet byProjectKeyCustomObjectsGet =
        mock(ByProjectKeyCustomObjectsGet.class);

    when(client.customObjects()).thenReturn(mock(ByProjectKeyCustomObjectsRequestBuilder.class));
    when(client.customObjects().get()).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWhere(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withPredicateVar(anyString(), anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet, byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withLimit(anyInt())).thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withWithTotal(anyBoolean()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withSort(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);
    when(byProjectKeyCustomObjectsGet.withSort(anyString()))
        .thenReturn(byProjectKeyCustomObjectsGet);

    final ApiHttpResponse<CustomObjectPagedQueryResponse> apiHttpResponse =
        mock(ApiHttpResponse.class);
    final CustomObjectPagedQueryResponse customObjectPagedQueryResponse =
        mock(CustomObjectPagedQueryResponse.class);
    when(byProjectKeyCustomObjectsGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(apiHttpResponse.getBody()).thenReturn(customObjectPagedQueryResponse);
    when(customObjectPagedQueryResponse.getResults()).thenReturn(Arrays.asList(mock1, mock2));

    final Set<CustomObject> customObjects =
        service
            .fetchMatchingCustomObjects(customObjectCompositeIdentifiers)
            .toCompletableFuture()
            .join();

    List<CustomObjectCompositeIdentifier> customObjectCompositeIdlist =
        new ArrayList<>(customObjectCompositeIdentifiers);

    assertAll(
        () -> assertThat(customObjects).contains(mock1, mock2),
        () ->
            assertThat(service.keyToIdCache.asMap())
                .containsKeys(
                    String.valueOf(customObjectCompositeIdlist.get(0)),
                    String.valueOf(customObjectCompositeIdlist.get(1))));
    verify(byProjectKeyCustomObjectsGet).execute();
  }

  @Test
  void fetchCustomObject_WithKeyAndContainer_ShouldFetchCustomObject() {
    final CustomObject mock = mock(CustomObject.class);
    when(mock.getId()).thenReturn(customObjectId);
    when(mock.getKey()).thenReturn(customObjectKey);
    when(mock.getContainer()).thenReturn(customObjectContainer);
    final ByProjectKeyCustomObjectsByContainerByKeyGet
        byProjectKeyCustomObjectsByContainerByKeyGet =
            mock(ByProjectKeyCustomObjectsByContainerByKeyGet.class);

    when(client.customObjects()).thenReturn(mock(ByProjectKeyCustomObjectsRequestBuilder.class));
    when(client.customObjects().withContainerAndKey(anyString(), anyString())).thenReturn(mock());
    when(client.customObjects().withContainerAndKey(anyString(), anyString()).get())
        .thenReturn(byProjectKeyCustomObjectsByContainerByKeyGet);

    final ApiHttpResponse<CustomObject> apiHttpResponse = mock(ApiHttpResponse.class);
    when(byProjectKeyCustomObjectsByContainerByKeyGet.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(apiHttpResponse.getBody()).thenReturn(mock);

    final Optional<CustomObject> customObjectOptional =
        service
            .fetchCustomObject(
                CustomObjectCompositeIdentifier.of(customObjectKey, customObjectContainer))
            .toCompletableFuture()
            .join();

    assertAll(
        () -> assertThat(customObjectOptional).containsSame(mock),
        () ->
            assertThat(
                    service
                        .keyToIdCache
                        .asMap()
                        .get(
                            CustomObjectCompositeIdentifier.of(
                                    customObjectKey, customObjectContainer)
                                .toString()))
                .isEqualTo(customObjectId));
    verify(byProjectKeyCustomObjectsByContainerByKeyGet).execute();
  }

  @Test
  void createCustomObject_WithDraft_ShouldCreateCustomObject() {
    final CustomObject mock = mock(CustomObject.class);
    when(mock.getId()).thenReturn(customObjectId);
    when(mock.getKey()).thenReturn(customObjectKey);
    when(mock.getContainer()).thenReturn(customObjectContainer);

    final ByProjectKeyCustomObjectsPost byProjectKeyCustomObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);

    when(client.customObjects()).thenReturn(mock(ByProjectKeyCustomObjectsRequestBuilder.class));
    when(client.customObjects().post(any(CustomObjectDraft.class)))
        .thenReturn(byProjectKeyCustomObjectsPost);
    final ApiHttpResponse<CustomObject> apiHttpResponse = mock(ApiHttpResponse.class);
    when(byProjectKeyCustomObjectsPost.execute())
        .thenReturn(CompletableFuture.completedFuture(apiHttpResponse));
    when(apiHttpResponse.getBody()).thenReturn(mock);

    final ObjectNode customObjectValue = JsonNodeFactory.instance.objectNode();
    customObjectValue.put("currentHash", "1234-5678-0912-3456");
    customObjectValue.put("convertedAmount", "100");

    final CustomObjectDraft draft =
        CustomObjectDraftBuilder.of()
            .container(customObjectContainer)
            .key(customObjectKey)
            .value(customObjectValue)
            .build();

    final Optional<CustomObject> customObjectOptional =
        service.upsertCustomObject(draft).toCompletableFuture().join();

    assertThat(customObjectOptional).containsSame(mock);
    verify(byProjectKeyCustomObjectsPost).execute();
  }

  @Test
  void createCustomObject_WithRequestException_ShouldNotCreateCustomObject()
      throws JsonProcessingException {
    final CustomObject mock = mock(CustomObject.class);
    when(mock.getId()).thenReturn(customObjectId);

    when(client.customObjects()).thenReturn(mock());

    final ByProjectKeyCustomObjectsPost byProjectKeyCustomObjectsPost =
        mock(ByProjectKeyCustomObjectsPost.class);
    when(client.customObjects().post(any(CustomObjectDraft.class)))
        .thenReturn(byProjectKeyCustomObjectsPost);

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
    when(byProjectKeyCustomObjectsPost.execute())
        .thenReturn(
            CompletableFuture.failedFuture(
                new ConcurrentModificationException(
                    409,
                    "",
                    null,
                    "",
                    new ApiHttpResponse<>(409, null, json.getBytes(StandardCharsets.UTF_8)))));

    final CustomObjectDraft draftMock = mock(CustomObjectDraft.class);
    when(draftMock.getKey()).thenReturn(customObjectKey);
    when(draftMock.getContainer()).thenReturn(customObjectContainer);

    final CompletableFuture<Optional<CustomObject>> future =
        service.upsertCustomObject(draftMock).toCompletableFuture();

    // assertion
    assertAll(
        () -> assertThat(future.isCompletedExceptionally()).isTrue(),
        () ->
            assertThat(future)
                .failsWithin(1, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseExactlyInstanceOf(ConcurrentModificationException.class));
  }
}
