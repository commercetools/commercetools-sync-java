package com.commercetools.sync.integration.services.impl;

import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ByProjectKeyCustomObjectsGet;
import com.commercetools.api.client.ByProjectKeyCustomObjectsRequestBuilder;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.sync.integration.commons.utils.CustomObjectITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.sdk2.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.sdk2.services.CustomObjectService;
import com.commercetools.sync.sdk2.services.impl.CustomObjectServiceImpl;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
class CustomObjectServiceImplIT {
  private CustomObjectService customObjectService;
  private static final String OLD_CUSTOM_OBJECT_KEY = "old_custom_object_key";
  private static final String OLD_CUSTOM_OBJECT_CONTAINER = "old_custom_object_container";
  private static final ObjectNode OLD_CUSTOM_OBJECT_VALUE =
      JsonNodeFactory.instance
          .objectNode()
          .put("old_custom_object_attribute", "old_custom_object_value");

  private static final String NEW_CUSTOM_OBJECT_KEY = "new_custom_object_key";
  private static final String NEW_CUSTOM_OBJECT_CONTAINER = "new_custom_object_container";
  private static final ObjectNode NEW_CUSTOM_OBJECT_VALUE =
      JsonNodeFactory.instance
          .objectNode()
          .put("new_custom_object_attribute", "new_custom_object_value");

  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Deletes customObjects from the target CTP project, then it populates the project with test
   * data.
   */
  @BeforeEach
  void setup() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();

    CustomObjectITUtils.deleteCustomObject(TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY, OLD_CUSTOM_OBJECT_CONTAINER);
    CustomObjectITUtils.deleteCustomObject(TestClientUtils.CTP_TARGET_CLIENT, NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_CONTAINER);
    CustomObjectITUtils.createCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY,
        OLD_CUSTOM_OBJECT_CONTAINER,
        OLD_CUSTOM_OBJECT_VALUE);

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();
    customObjectService = new CustomObjectServiceImpl(customObjectSyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    CustomObjectITUtils.deleteCustomObject(TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY, OLD_CUSTOM_OBJECT_CONTAINER);
    CustomObjectITUtils.deleteCustomObject(TestClientUtils.CTP_TARGET_CLIENT, NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_CONTAINER);
  }

  @Test
  void fetchCachedCustomObjectId_WithNonExistingCustomObject_ShouldReturnEmptyCustomObject() {
    CustomObjectCompositeIdentifier compositeIdentifier =
        CustomObjectCompositeIdentifier.of("non-existing-key", "non-existing-container");
    final Optional<String> customObject =
        customObjectService
            .fetchCachedCustomObjectId(compositeIdentifier)
            .toCompletableFuture()
            .join();
    assertThat(customObject).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCustomObjects_WithNonExistingKeysAndContainers_ShouldReturnEmptySet() {
    final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers = new HashSet<>();
    customObjectCompositeIdentifiers.add(
        CustomObjectCompositeIdentifier.of(
            OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_1"));
    customObjectCompositeIdentifiers.add(
        CustomObjectCompositeIdentifier.of(
            OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_2"));

    final Set<CustomObject> matchingCustomObjects =
        customObjectService
            .fetchMatchingCustomObjects(customObjectCompositeIdentifiers)
            .toCompletableFuture()
            .join();

    assertThat(matchingCustomObjects).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void
      fetchMatchingCustomObjects_WithDifferentExistingCombinationOfKeysAndContainers_ShouldReturnEmptySet() {

    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_2");
    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_2");

    CustomObjectITUtils.createCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_1",
        OLD_CUSTOM_OBJECT_CONTAINER + "_1",
        OLD_CUSTOM_OBJECT_VALUE);
    CustomObjectITUtils.createCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_1",
        OLD_CUSTOM_OBJECT_CONTAINER + "_2",
        OLD_CUSTOM_OBJECT_VALUE);
    CustomObjectITUtils.createCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_2",
        OLD_CUSTOM_OBJECT_CONTAINER + "_1",
        OLD_CUSTOM_OBJECT_VALUE);
    CustomObjectITUtils.createCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_2",
        OLD_CUSTOM_OBJECT_CONTAINER + "_2",
        OLD_CUSTOM_OBJECT_VALUE);

    final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers = new HashSet<>();
    customObjectCompositeIdentifiers.add(
        CustomObjectCompositeIdentifier.of(
            OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_1"));
    customObjectCompositeIdentifiers.add(
        CustomObjectCompositeIdentifier.of(
            OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_2"));
    customObjectCompositeIdentifiers.add(
        CustomObjectCompositeIdentifier.of(
            OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_1"));

    final Set<CustomObject> matchingCustomObjects =
        customObjectService
            .fetchMatchingCustomObjects(customObjectCompositeIdentifiers)
            .toCompletableFuture()
            .join();

    assertThat(matchingCustomObjects).size().isEqualTo(3);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_2");
    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    CustomObjectITUtils.deleteCustomObject(
        TestClientUtils.CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_2");
  }

  @Test
  void fetchMatchingCustomObjectsByCompositeIdentifiers_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final ProjectApiRoot spyClient = Mockito.spy(TestClientUtils.CTP_TARGET_CLIENT);
    when(spyClient.customObjects()).thenReturn(mock(ByProjectKeyCustomObjectsRequestBuilder.class));
    final ByProjectKeyCustomObjectsGet getMock = mock(ByProjectKeyCustomObjectsGet.class);
    when(spyClient.customObjects().get()).thenReturn(getMock);
    when(getMock.withWhere(any(String.class))).thenReturn(getMock);
    when(getMock.withPredicateVar(any(String.class), any())).thenReturn(getMock);
    when(getMock.withLimit(any(Integer.class))).thenReturn(getMock);
    when(getMock.withWithTotal(any(Boolean.class))).thenReturn(getMock);
    when(getMock.execute())
        .thenReturn(exceptionallyCompletedFuture(new BadGatewayException(500, "", null, "", null)))
        .thenCallRealMethod();

    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectService spyCustomObjectService = new CustomObjectServiceImpl(spyOptions);

    final Set<CustomObjectCompositeIdentifier> customObjectCompositeIdentifiers = new HashSet<>();
    customObjectCompositeIdentifiers.add(
        CustomObjectCompositeIdentifier.of(OLD_CUSTOM_OBJECT_KEY, OLD_CUSTOM_OBJECT_CONTAINER));

    // test and assert
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(spyCustomObjectService.fetchMatchingCustomObjects(customObjectCompositeIdentifiers))
        .failsWithin(10, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
  }

  @Test
  void
      fetchCustomObject_WithNonExistingCustomObjectKeyAndContainer_ShouldReturnEmptyCustomObject() {
    final CustomObject customObjectOptional =
        TestClientUtils.CTP_TARGET_CLIENT
            .customObjects()
            .withContainerAndKey(OLD_CUSTOM_OBJECT_CONTAINER, OLD_CUSTOM_OBJECT_KEY)
            .get()
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .join();
    assertThat(customObjectOptional).isNotNull();

    final Optional<CustomObject> fetchedCustomObjectOptional =
        customObjectService
            .fetchCustomObject(
                CustomObjectCompositeIdentifier.of("non-existing-key", "non-existing-container"))
            .toCompletableFuture()
            .join();
    assertThat(fetchedCustomObjectOptional).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void upsertCustomObject_WithValidCustomObject_ShouldCreateCustomObjectAndCacheId() {
    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(NEW_CUSTOM_OBJECT_CONTAINER)
            .key(NEW_CUSTOM_OBJECT_KEY)
            .value(NEW_CUSTOM_OBJECT_VALUE)
            .build();

    final ProjectApiRoot spyClient = Mockito.spy(TestClientUtils.CTP_TARGET_CLIENT);
    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectService spyCustomObjectService = new CustomObjectServiceImpl(spyOptions);

    final Optional<CustomObject> createdCustomObject =
        spyCustomObjectService
            .upsertCustomObject(newCustomObjectDraft)
            .toCompletableFuture()
            .join();

    final CustomObject fetchedOptional =
        TestClientUtils.CTP_TARGET_CLIENT
            .customObjects()
            .withContainerAndKey(NEW_CUSTOM_OBJECT_CONTAINER, NEW_CUSTOM_OBJECT_KEY)
            .get()
            .execute()
            .thenApply(customObjectApiHttpResponse -> customObjectApiHttpResponse.getBody())
            .toCompletableFuture()
            .join();

    assertThat(createdCustomObject)
        .hasValueSatisfying(
            created -> {
              assertThat(created.getKey()).isEqualTo(fetchedOptional.getKey());
              assertThat(created.getContainer()).isEqualTo(fetchedOptional.getContainer());
              assertThat(created.getId()).isEqualTo(fetchedOptional.getId());
              assertThat(created.getValue()).isEqualTo(fetchedOptional.getValue());
            });

    final ByProjectKeyCustomObjectsRequestBuilder mock1 =
        mock(ByProjectKeyCustomObjectsRequestBuilder.class);
    when(spyClient.customObjects()).thenReturn(mock1);
    final ByProjectKeyCustomObjectsGet mock2 = mock(ByProjectKeyCustomObjectsGet.class);
    when(mock1.get()).thenReturn(mock2);
    when(mock2.withWhere(any(String.class))).thenReturn(mock2);
    when(mock2.withPredicateVar(any(String.class), any(String.class))).thenReturn(mock2, mock2);
    final CompletableFuture<ApiHttpResponse<CustomObject>> mock3 = mock(CompletableFuture.class);
    final CompletableFuture<ApiHttpResponse<CustomObject>> spy = spy(mock3);

    // Assert that the created customObject is cached
    final Optional<String> customObjectId =
        spyCustomObjectService
            .fetchCachedCustomObjectId(
                CustomObjectCompositeIdentifier.of(
                    NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_CONTAINER))
            .toCompletableFuture()
            .join();
    assertThat(customObjectId).isPresent();
    verify(spy, times(0)).handle(any());
  }

  @Test
  void upsertCustomObject_WithDuplicateKeyAndContainerInCompositeIdentifier_ShouldUpdateValue() {
    // preparation
    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container(OLD_CUSTOM_OBJECT_CONTAINER)
            .key(OLD_CUSTOM_OBJECT_KEY)
            .value(NEW_CUSTOM_OBJECT_VALUE)
            .build();

    final Optional<CustomObject> result =
        customObjectService.upsertCustomObject(newCustomObjectDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isNotEmpty();
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
    final LinkedHashMap<String, String> value =
        (LinkedHashMap<String, String>) result.get().getValue();
    final String firstJsonField = NEW_CUSTOM_OBJECT_VALUE.fieldNames().next();
    assertThat(value.get(firstJsonField))
        .isEqualTo(NEW_CUSTOM_OBJECT_VALUE.get(firstJsonField).asText());
    assertThat(result.get().getContainer()).isEqualTo(OLD_CUSTOM_OBJECT_CONTAINER);
    assertThat(result.get().getKey()).isEqualTo(OLD_CUSTOM_OBJECT_KEY);
  }
}
