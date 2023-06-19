package com.commercetools.sync.sdk2.customobjects;

import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.sdk2.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.sdk2.services.CustomObjectService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadRequestException;
import io.vrap.rmf.base.client.error.ConcurrentModificationException;
import java.util.*;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class CustomObjectSyncTest {

  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

    when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BadRequestException(
                      500, "", null, "", new ApiHttpResponse<>(500, null, null));
                }));

    final CustomObjectSync customObjectSync =
        new CustomObjectSync(spyCustomObjectSyncOptions, mockCustomObjectService);

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value("someValue")
            .build();

    // test
    final CustomObjectSyncStatistics customObjectSyncStatistics =
        customObjectSync.sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

    // assertion
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement()
        .asString()
        .isEqualTo(
            "Failed to fetch existing custom objects with keys: " + "'[someContainer|someKey]'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement()
        .isInstanceOfSatisfying(
            Throwable.class,
            throwable -> {
              assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
              assertThat(throwable).hasCauseExactlyInstanceOf(BadRequestException.class);
            });

    assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void
      sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback_ShouldNotCallBeforeUpdateCallback() {
    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(emptyList(), emptyList());

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(emptySet()));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.empty()));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"))
            .build();

    // test
    new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
        .sync(singletonList(newCustomObjectDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
    verify(spyCustomObjectSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void
      sync_WithOnlyDraftsToUpdate_ShouldCallBeforeCreateCallback_ShouldNotCallBeforeUpdateCallback() {
    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(emptyList(), emptyList());

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"))
            .build();

    final CustomObject mockedExistingCustomObject = mock(CustomObject.class);
    when(mockedExistingCustomObject.getKey()).thenReturn(newCustomObjectDraft.getKey());
    when(mockedExistingCustomObject.getContainer()).thenReturn("differentContainer");

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingCustomObject)));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.of(mockedExistingCustomObject)));

    // test
    new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
        .sync(singletonList(newCustomObjectDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
    verify(spyCustomObjectSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithSameIdentifiersAndDifferentValues_ShouldUpdateSuccessfully() {
    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(emptyList(), emptyList());

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("someContainer");
    when(existingCustomObject.getKey()).thenReturn("someKey");
    when(existingCustomObject.getValue()).thenReturn(Integer.valueOf(2020));

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue())
        .thenReturn(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"));

    final Set<CustomObject> existingCustomObjectSet = new HashSet<>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"))
            .build();

    // test
    final CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();

    // assertion
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0));
  }

  @Test
  void sync_WithSameIdentifiersAndIdenticalValues_ShouldProcessedAndNotUpdated() {
    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(emptyList(), emptyList());

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"))
            .build();

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("someContainer");
    when(existingCustomObject.getKey()).thenReturn("someKey");
    when(existingCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

    final Set<CustomObject> existingCustomObjectSet = new HashSet<>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();

    // assertion
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0));
  }

  @Test
  void
      sync_UpdateWithConcurrentModificationExceptionAndRetryWithFetchException_ShouldIncrementFailed() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("someContainer");
    when(existingCustomObject.getKey()).thenReturn("someKey");
    when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

    final Set<CustomObject> existingCustomObjectSet = new HashSet<>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue())
        .thenReturn(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"));

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new ConcurrentModificationException(
                      409, "", null, "", new ApiHttpResponse<>(409, null, null));
                }));
    when(customObjectService.fetchCustomObject(any(CustomObjectCompositeIdentifier.class)))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BadRequestException(
                      500, "", null, "", new ApiHttpResponse<>(500, null, null));
                }));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"))
            .build();

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();
    // assertion
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(1));
    assertThat(exceptions).hasSize(1);
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement()
        .isEqualTo(
            format(
                "Failed to update custom object with key: '%s'. Reason: %s",
                CustomObjectCompositeIdentifier.of(newCustomObjectDraft).toString(),
                "Failed to fetch from CTP while retrying after concurrency modification."));

    verify(customObjectService).fetchCustomObject(any(CustomObjectCompositeIdentifier.class));
    verify(customObjectService).upsertCustomObject(any());
    verify(customObjectService).fetchMatchingCustomObjects(any());
  }

  @Test
  void sync_UpdateWithBadRequestExceptionAndRetryWithFetchException_ShouldIncrementFailed() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("someContainer");
    when(existingCustomObject.getKey()).thenReturn("someKey");
    when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

    final Set<CustomObject> existingCustomObjectSet = new HashSet<CustomObject>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue())
        .thenReturn(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"));

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BadRequestException(
                      500, "", null, "", new ApiHttpResponse<>(500, null, null));
                }));
    when(customObjectService.fetchCustomObject(any(CustomObjectCompositeIdentifier.class)))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BadRequestException(
                      500, "", null, "", new ApiHttpResponse<>(500, null, null));
                }));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.objectNode().put("json-field", "json-value"))
            .build();

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();

    // assertion
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(1));
    assertThat(exceptions).hasSize(1);
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement()
        .isEqualTo(
            format(
                "Failed to update custom object with key: '%s'. Reason: %s",
                CustomObjectCompositeIdentifier.of(newCustomObjectDraft).toString(),
                exceptions.get(0).getMessage()));
    verify(customObjectService).upsertCustomObject(any());
    verify(customObjectService).fetchMatchingCustomObjects(any());
  }

  @Test
  void sync_WithDifferentIdentifiers_ShouldCreateSuccessfully() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("otherContainer");
    when(existingCustomObject.getKey()).thenReturn("otherKey");
    when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

    final Set<CustomObject> existingCustomObjectSet = new HashSet<>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of()
            .container("someContainer")
            .key("someKey")
            .value(JsonNodeFactory.instance.numberNode(2020))
            .build();

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(exceptions).hasSize(0);
    assertThat(errorMessages).hasSize(0);
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0));
    verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
  }

  @Test
  void sync_WithSameKeysAndDifferentContainers_ShouldCreateSuccessfully() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("otherContainer");
    when(existingCustomObject.getKey()).thenReturn("someKey");
    when(existingCustomObject.getValue()).thenReturn(2020);

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue()).thenReturn(Integer.valueOf(2020));

    final Set<CustomObject> existingCustomObjectSet = new HashSet<>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of().container("someContainer").key("someKey").value(2020).build();

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(exceptions).hasSize(0);
    assertThat(errorMessages).hasSize(0);
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0));
    verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
  }

  @Test
  void sync_WithDifferentKeysAndSameContainers_ShouldCreateSuccessfully() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObject existingCustomObject = mock(CustomObject.class);
    when(existingCustomObject.getContainer()).thenReturn("someContainer");
    when(existingCustomObject.getKey()).thenReturn("otherKey");
    when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

    final CustomObject updatedCustomObject = mock(CustomObject.class);
    when(updatedCustomObject.getContainer()).thenReturn("someContainer");
    when(updatedCustomObject.getKey()).thenReturn("someKey");
    when(updatedCustomObject.getValue()).thenReturn(2020);

    final Set<CustomObject> existingCustomObjectSet = new HashSet<CustomObject>();
    existingCustomObjectSet.add(existingCustomObject);

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(existingCustomObjectSet));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

    final CustomObjectDraft newCustomObjectDraft =
        CustomObjectDraftBuilder.of().container("someContainer").key("someKey").value(2020).build();

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(exceptions).hasSize(0);
    assertThat(errorMessages).hasSize(0);
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0));
    verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
  }

  @Test
  void sync_WitEmptyValidDrafts_ShouldFailed() {
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final CustomObjectSyncOptions spyCustomObjectSyncOptions =
        initCustomObjectSyncOptions(errorMessages, exceptions);

    final CustomObjectService customObjectService = mock(CustomObjectService.class);
    when(customObjectService.fetchMatchingCustomObjects(anySet()))
        .thenReturn(completedFuture(emptySet()));
    when(customObjectService.upsertCustomObject(any()))
        .thenReturn(completedFuture(Optional.empty()));

    // test
    CustomObjectSyncStatistics syncStatistics =
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(null))
            .toCompletableFuture()
            .join();

    // assertion
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.get(0)).isNull();
    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0)).isEqualTo("CustomObjectDraft is null.");
    assertAll(
        () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
        () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
        () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(1));
  }

  @Nonnull
  private CustomObjectSyncOptions initCustomObjectSyncOptions(
      @Nonnull final List<String> errorMessages, @Nonnull final List<Throwable> exceptions) {
    return spy(
        CustomObjectSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build());
  }
}
