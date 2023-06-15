package com.commercetools.sync.integration.sdk2.externalsource.customobjects;

import static com.commercetools.sync.integration.sdk2.commons.utils.CustomObjectITUtils.createCustomObject;
import static com.commercetools.sync.integration.sdk2.commons.utils.CustomObjectITUtils.deleteCustomObject;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createBadGatewayException;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createConcurrentModificationException;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.api.models.category.CategoryReference;
import com.commercetools.api.models.category.CategoryReferenceBuilder;
import com.commercetools.api.models.common.CentPrecisionMoneyDraft;
import com.commercetools.api.models.common.CentPrecisionMoneyDraftBuilder;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.api.models.error.ErrorResponse;
import com.commercetools.api.models.error.ErrorResponseBuilder;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSync;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectSyncStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.ApiHttpMethod;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class CustomObjectSyncIT {
  private static final String CUSTOM_OBJECT_KEY_1 = "key1";
  private static final String CUSTOM_OBJECT_CONTAINER_1 = "container1";
  private static final ObjectNode CUSTOM_OBJECT_VALUE_JSON_1 =
      JsonNodeFactory.instance.objectNode().put("name", "value1");

  @BeforeEach
  void setup() {
    deleteCustomObject(CTP_TARGET_CLIENT, CUSTOM_OBJECT_KEY_1, CUSTOM_OBJECT_CONTAINER_1);
    deleteCustomObject(CTP_TARGET_CLIENT, "key2", "container2");

    createCustomObject(
        CTP_TARGET_CLIENT,
        CUSTOM_OBJECT_KEY_1,
        CUSTOM_OBJECT_CONTAINER_1,
        CUSTOM_OBJECT_VALUE_JSON_1);
  }

  @AfterAll
  static void tearDown() {
    deleteCustomObject(CTP_TARGET_CLIENT, "key1", "container1");
    deleteCustomObject(CTP_TARGET_CLIENT, "key2", "container2");
  }

  @Test
  void sync_withNewCustomObject_shouldCreateCustomObject() {

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

    final ObjectNode customObject2Value =
        JsonNodeFactory.instance.objectNode().put("name", "value1");

    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("container2")
            .key("key2")
            .value(customObject2Value)
            .build();

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 1, 0, 0);
  }

  @Test
  void sync_withExistingCustomObjectThatHasDifferentJsonValue_shouldUpdateCustomObject() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

    final ObjectNode customObject2Value =
        JsonNodeFactory.instance.objectNode().put("name", "value2");

    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CUSTOM_OBJECT_CONTAINER_1)
            .key(CUSTOM_OBJECT_KEY_1)
            .value(customObject2Value)
            .build();

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_withExistingCustomObjectThatHasDifferentMoneyValue_shouldUpdateCustomObject() {
    final CentPrecisionMoneyDraft originalMoneyValue =
        CentPrecisionMoneyDraftBuilder.of()
            .centAmount(100L)
            .currencyCode("EUR")
            .fractionDigits(2)
            .build();
    // Change value of exiting CustomObject before test
    createCustomObject(
        CTP_TARGET_CLIENT, CUSTOM_OBJECT_KEY_1, CUSTOM_OBJECT_CONTAINER_1, originalMoneyValue);
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();
    final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

    final CentPrecisionMoneyDraft newMoneyValue =
        CentPrecisionMoneyDraftBuilder.of()
            .centAmount(200L)
            .currencyCode("EUR")
            .fractionDigits(2)
            .build();

    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CUSTOM_OBJECT_CONTAINER_1)
            .key(CUSTOM_OBJECT_KEY_1)
            .value(newMoneyValue)
            .build();

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_withExistingCustomObjectThatHasSameJsonValue_shouldNotUpdateCustomObject() {

    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CUSTOM_OBJECT_CONTAINER_1)
            .key(CUSTOM_OBJECT_KEY_1)
            .value(CUSTOM_OBJECT_VALUE_JSON_1)
            .build();

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_TARGET_CLIENT).build();

    final CustomObjectSync customObjectSync = new CustomObjectSync(customObjectSyncOptions);

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 0);
  }

  @Test
  void
      sync_withChangedCustomObjectAndConcurrentModificationException_shouldRetryAndUpdateCustomObject() {
    final ProjectApiRoot mockClient =
        buildClientWithConcurrentModificationAndOptionalExceptionOnFetch(null);
    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<String> warningCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

    final CategoryReference newCustomObjectValue =
        CategoryReferenceBuilder.of().id("category-id").build();
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CUSTOM_OBJECT_CONTAINER_1)
            .key(CUSTOM_OBJECT_KEY_1)
            .value(newCustomObjectValue)
            .build();

    // test
    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 1, 0);
    Assertions.assertThat(errorCallBackExceptions).isEmpty();
    Assertions.assertThat(errorCallBackMessages).isEmpty();
    Assertions.assertThat(warningCallBackMessages).isEmpty();
  }

  @Test
  void sync_withChangedCustomObjectWithBadGatewayExceptionInsideUpdateRetry_shouldFailToUpdate() {
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(500)
            .errors(Collections.emptyList())
            .message("test")
            .build();
    final String responseJsonString = getResponseJsonString(errorResponse);
    final ProjectApiRoot mockClient =
        buildClientWithConcurrentModificationAndOptionalExceptionOnFetch(responseJsonString);

    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

    final ObjectNode newCustomObjectValue =
        JsonNodeFactory.instance.objectNode().put("name", "value2");
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CUSTOM_OBJECT_CONTAINER_1)
            .key(CUSTOM_OBJECT_KEY_1)
            .value(newCustomObjectValue)
            .build();

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
    Assertions.assertThat(errorCallBackMessages).hasSize(1);
    Assertions.assertThat(errorCallBackExceptions).hasSize(1);
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update custom object with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                    + "after concurrency modification.",
                CustomObjectCompositeIdentifier.of(customObjectDraft)));
  }

  @Test
  void sync_withConcurrentModificationExceptionAndUnexpectedDelete_shouldFailToReFetchAndUpdate() {
    final ErrorResponse errorResponse =
        ErrorResponseBuilder.of()
            .statusCode(404)
            .errors(Collections.emptyList())
            .message("test")
            .build();
    final String responseJsonString = getResponseJsonString(errorResponse);
    final ProjectApiRoot mockClient =
        buildClientWithConcurrentModificationAndOptionalExceptionOnFetch(responseJsonString);

    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

    final ObjectNode newCustomObjectValue =
        JsonNodeFactory.instance.objectNode().put("name", "value2");
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(CUSTOM_OBJECT_CONTAINER_1)
            .key(CUSTOM_OBJECT_KEY_1)
            .value(newCustomObjectValue)
            .build();

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
    Assertions.assertThat(errorCallBackMessages).hasSize(1);
    Assertions.assertThat(errorCallBackExceptions).hasSize(1);

    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to update custom object with key: '%s'. Reason: Not found when attempting to fetch while"
                    + " retrying after concurrency modification.",
                CustomObjectCompositeIdentifier.of(customObjectDraft)));
  }

  @Test
  void sync_withNewCustomObjectAndBadRequest_shouldNotCreateButHandleError() {
    final ProjectApiRoot mockClient = buildClientWithExceptionOnUpsert();

    final ObjectNode newCustomObjectValue =
        JsonNodeFactory.instance.objectNode().put("name", "value2");
    final CustomObjectDraft customObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("container2")
            .key("key2")
            .value(newCustomObjectValue)
            .build();

    final List<String> errorCallBackMessages = new ArrayList<>();
    final List<Throwable> errorCallBackExceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(mockClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions);

    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync
            .sync(Collections.singletonList(customObjectDraft))
            .toCompletableFuture()
            .join();

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
    Assertions.assertThat(errorCallBackMessages).hasSize(1);
    Assertions.assertThat(errorCallBackExceptions).hasSize(1);
    Assertions.assertThat(errorCallBackExceptions.get(0))
        .isExactlyInstanceOf(CompletionException.class);
    Assertions.assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(BadGatewayException.class);
    Assertions.assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to create custom object with key: '%s'.",
                CustomObjectCompositeIdentifier.of(customObjectDraft)));
  }

  @Nonnull
  private ProjectApiRoot buildClientWithConcurrentModificationAndOptionalExceptionOnFetch(
      @Nullable final String responseString) {

    // Reference assignment to have an atomic string reference
    final String responseAsJson = responseString;

    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger postRequestInvocationCounter = new AtomicInteger(0);
    final AtomicInteger getRequestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("custom-objects") && ApiHttpMethod.POST.equals(method)) {
                    if (postRequestInvocationCounter.getAndIncrement() == 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createConcurrentModificationException());
                    }
                  }
                  if (uri.contains("custom-objects")
                      && ApiHttpMethod.GET.equals(method)
                      && responseAsJson != null) {
                    if (getRequestInvocationCounter.getAndIncrement() > 0) {
                      if (responseAsJson.contains("500")) {
                        return CompletableFutureUtils.exceptionallyCompletedFuture(
                            new BadGatewayException(
                                500,
                                "",
                                null,
                                "",
                                new ApiHttpResponse<>(
                                    500, null, responseAsJson.getBytes(StandardCharsets.UTF_8))));
                      } else if (responseAsJson.contains("404")) {
                        return CompletableFutureUtils.exceptionallyCompletedFuture(
                            new NotFoundException(
                                404,
                                "",
                                null,
                                "",
                                new ApiHttpResponse<>(
                                    404, null, responseAsJson.getBytes(StandardCharsets.UTF_8))));
                      }
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  @Nonnull
  private ProjectApiRoot buildClientWithExceptionOnUpsert() {

    // Helps to count invocation of a request and used to decide execution or mocking response
    final AtomicInteger requestInvocationCounter = new AtomicInteger(0);
    final ProjectApiRoot testClient =
        ApiRootBuilder.of(
                request -> {
                  final String uri = request.getUri() != null ? request.getUri().toString() : "";
                  final ApiHttpMethod method = request.getMethod();
                  if (uri.contains("custom-objects") && ApiHttpMethod.POST.equals(method)) {
                    if (requestInvocationCounter.getAndIncrement() == 0) {
                      return CompletableFutureUtils.exceptionallyCompletedFuture(
                          createBadGatewayException());
                    }
                  }
                  return CTP_TARGET_CLIENT.getApiHttpClient().execute(request);
                })
            .withApiBaseUrl(CTP_TARGET_CLIENT.getApiHttpClient().getBaseUri())
            .build(CTP_TARGET_CLIENT.getProjectKey());
    return testClient;
  }

  private String getResponseJsonString(@Nonnull final Object response) {
    final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json;
    try {
      json = ow.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      // ignore the error
      json = null;
    }
    return json;
  }
}
