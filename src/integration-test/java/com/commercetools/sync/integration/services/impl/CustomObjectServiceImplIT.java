package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.createCustomObject;
import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteCustomObject;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.CustomObjectSyncOptionsBuilder;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.services.impl.CustomObjectServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    deleteCustomObject(CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY, OLD_CUSTOM_OBJECT_CONTAINER);
    deleteCustomObject(CTP_TARGET_CLIENT, NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_CONTAINER);
    createCustomObject(
        CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY,
        OLD_CUSTOM_OBJECT_CONTAINER,
        OLD_CUSTOM_OBJECT_VALUE);

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
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
    deleteCustomObject(CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY, OLD_CUSTOM_OBJECT_CONTAINER);
    deleteCustomObject(CTP_TARGET_CLIENT, NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_CONTAINER);
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

    final Set<CustomObject<JsonNode>> matchingCustomObjects =
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

    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_2");
    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_2");

    createCustomObject(
        CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_1",
        OLD_CUSTOM_OBJECT_CONTAINER + "_1",
        OLD_CUSTOM_OBJECT_VALUE);
    createCustomObject(
        CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_1",
        OLD_CUSTOM_OBJECT_CONTAINER + "_2",
        OLD_CUSTOM_OBJECT_VALUE);
    createCustomObject(
        CTP_TARGET_CLIENT,
        OLD_CUSTOM_OBJECT_KEY + "_2",
        OLD_CUSTOM_OBJECT_CONTAINER + "_1",
        OLD_CUSTOM_OBJECT_VALUE);
    createCustomObject(
        CTP_TARGET_CLIENT,
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

    final Set<CustomObject<JsonNode>> matchingCustomObjects =
        customObjectService
            .fetchMatchingCustomObjects(customObjectCompositeIdentifiers)
            .toCompletableFuture()
            .join();

    assertThat(matchingCustomObjects).size().isEqualTo(3);
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();

    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_1", OLD_CUSTOM_OBJECT_CONTAINER + "_2");
    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_1");
    deleteCustomObject(
        CTP_TARGET_CLIENT, OLD_CUSTOM_OBJECT_KEY + "_2", OLD_CUSTOM_OBJECT_CONTAINER + "_2");
  }

  @Test
  void fetchMatchingCustomObjectsByCompositeIdentifiers_WithBadGateWayExceptionAlways_ShouldFail() {
    // Mock sphere client to return BadGatewayException on any request.
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    when(spyClient.execute(any(CustomObjectQuery.class)))
        .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
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
    final Optional<CustomObject<JsonNode>> customObjectOptional =
        CTP_TARGET_CLIENT
            .execute(
                CustomObjectQuery.ofJsonNode()
                    .withPredicates(
                        customObjectQueryModel ->
                            customObjectQueryModel
                                .key()
                                .is(OLD_CUSTOM_OBJECT_KEY)
                                .and(
                                    customObjectQueryModel
                                        .container()
                                        .is(OLD_CUSTOM_OBJECT_CONTAINER))))
            .toCompletableFuture()
            .join()
            .head();
    assertThat(customObjectOptional).isNotNull();

    final Optional<CustomObject<JsonNode>> fetchedCustomObjectOptional =
        executeBlocking(
            customObjectService.fetchCustomObject(
                CustomObjectCompositeIdentifier.of("non-existing-key", "non-existing-container")));
    assertThat(fetchedCustomObjectOptional).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void upsertCustomObject_WithValidCustomObject_ShouldCreateCustomObjectAndCacheId() {
    final CustomObjectDraft<JsonNode> newCustomObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert(
            NEW_CUSTOM_OBJECT_CONTAINER, NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_VALUE);

    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final CustomObjectSyncOptions spyOptions =
        CustomObjectSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .build();

    final CustomObjectService spyCustomObjectService = new CustomObjectServiceImpl(spyOptions);

    final Optional<CustomObject<JsonNode>> createdCustomObject =
        spyCustomObjectService
            .upsertCustomObject(newCustomObjectDraft)
            .toCompletableFuture()
            .join();

    final Optional<CustomObject<JsonNode>> queriedOptional =
        CTP_TARGET_CLIENT
            .execute(
                CustomObjectQuery.ofJsonNode()
                    .withPredicates(
                        customObjectQueryModel ->
                            customObjectQueryModel
                                .container()
                                .is(NEW_CUSTOM_OBJECT_CONTAINER)
                                .and(customObjectQueryModel.key().is(NEW_CUSTOM_OBJECT_KEY))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(queriedOptional)
        .hasValueSatisfying(
            queried ->
                assertThat(createdCustomObject)
                    .hasValueSatisfying(
                        created -> {
                          assertThat(created.getKey()).isEqualTo(queried.getKey());
                          assertThat(created.getContainer()).isEqualTo(queried.getContainer());
                          assertThat(created.getId()).isEqualTo(queried.getId());
                          assertThat(created.getValue()).isEqualTo(queried.getValue());
                        }));

    // Assert that the created customObject is cached
    final Optional<String> customObjectId =
        spyCustomObjectService
            .fetchCachedCustomObjectId(
                CustomObjectCompositeIdentifier.of(
                    NEW_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_CONTAINER))
            .toCompletableFuture()
            .join();
    assertThat(customObjectId).isPresent();
    verify(spyClient, times(0)).execute(any(CustomObjectQuery.class));
  }

  @Test
  void upsertCustomObject_WithDuplicateKeyAndContainerInCompositeIdentifier_ShouldUpdateValue() {
    // preparation
    final CustomObjectDraft<JsonNode> newCustomObjectDraft =
        CustomObjectDraft.ofUnversionedUpsert(
            OLD_CUSTOM_OBJECT_CONTAINER, OLD_CUSTOM_OBJECT_KEY, NEW_CUSTOM_OBJECT_VALUE);

    final Optional<CustomObject<JsonNode>> result =
        customObjectService.upsertCustomObject(newCustomObjectDraft).toCompletableFuture().join();

    // assertion
    assertThat(result).isNotEmpty();
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
    assertThat(result.get().getValue()).isEqualTo(NEW_CUSTOM_OBJECT_VALUE);
    assertThat(result.get().getContainer()).isEqualTo(OLD_CUSTOM_OBJECT_CONTAINER);
    assertThat(result.get().getKey()).isEqualTo(OLD_CUSTOM_OBJECT_KEY);
  }
}
