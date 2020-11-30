package com.commercetools.sync.customobjects;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.services.CustomObjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.models.SphereException;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CustomObjectSyncTest {

    private CustomObjectDraft<JsonNode> newCustomObjectDraft;

    @BeforeEach
    void setup() {
        newCustomObjectDraft = CustomObjectDraft
            .ofUnversionedUpsert("someContainer", "someKey",
                JsonNodeFactory.instance.objectNode().put("json-field", "json-value"));
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

        when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        final CustomObjectSync customObjectSync =
                new CustomObjectSync(spyCustomObjectSyncOptions, mockCustomObjectService);

        // test
        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture().join();

        // assertion
        assertThat(errorMessages)
            .hasSize(1).singleElement().asString()
            .isEqualTo("Failed to fetch existing custom objects with keys: "
                + "'[someContainer|someKey]'.");

        assertThat(exceptions)
            .hasSize(1).singleElement().isInstanceOfSatisfying(Throwable.class, throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback_ShouldNotCallBeforeUpdateCallback() {
        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(emptyList(),emptyList());

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet())).thenReturn(completedFuture(emptySet()));
        when(customObjectService.upsertCustomObject(any())).thenReturn(completedFuture(Optional.empty()));

        // test
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
        verify(spyCustomObjectSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldCallBeforeCreateCallback_ShouldNotCallBeforeUpdateCallback() {
        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(emptyList(), emptyList());

        final CustomObject<JsonNode> mockedExistingCustomObject = mock(CustomObject.class);
        when(mockedExistingCustomObject.getKey()).thenReturn(newCustomObjectDraft.getKey());
        when(mockedExistingCustomObject.getContainer()).thenReturn("differentContainer");

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
            .thenReturn(completedFuture(singleton(mockedExistingCustomObject)));
        when(customObjectService.upsertCustomObject(any()))
            .thenReturn(completedFuture(Optional.of(mockedExistingCustomObject)));

        // test
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
        verify(spyCustomObjectSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithSameIdentifiersAndDifferentValues_ShouldUpdateSuccessfully() {
        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(emptyList(), emptyList());

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("someContainer");
        when(existingCustomObject.getKey()).thenReturn("someKey");
        when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0)
        );
    }

    @Test
    void sync_WithSameIdentifiersAndIdenticalValues_ShouldProcessedAndNotUpdated() {
        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(emptyList(), emptyList());

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("someContainer");
        when(existingCustomObject.getKey()).thenReturn("someKey");
        when(existingCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0)
        );
    }


    @Test
    void sync_UpdateWithConcurrentModificationExceptionAndRetryWithFetchException_ShouldIncrementFailed() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("someContainer");
        when(existingCustomObject.getKey()).thenReturn("someKey");
        when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(supplyAsync(() -> { throw new ConcurrentModificationException(); }));
        when(customObjectService.fetchCustomObject(any(CustomObjectCompositeIdentifier.class)))
                .thenReturn(supplyAsync(() -> {
                    throw new SphereException();
                }));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();
        // assertion
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(1)
        );
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement()
            .isEqualTo(
                    format("Failed to update custom object with key: '%s'. Reason: %s",
                            CustomObjectCompositeIdentifier.of(newCustomObjectDraft).toString(),
                            "Failed to fetch from CTP while retrying after concurrency modification.")
            );

        verify(customObjectService).fetchCustomObject(any(CustomObjectCompositeIdentifier.class));
        verify(customObjectService).upsertCustomObject(any());
        verify(customObjectService).fetchMatchingCustomObjects(any());
    }

    @Test
    void sync_UpdateWithSphereExceptionAndRetryWithFetchException_ShouldIncrementFailed() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("someContainer");
        when(existingCustomObject.getKey()).thenReturn("someKey");
        when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(supplyAsync(() -> { throw new SphereException(); }));
        when(customObjectService.fetchCustomObject(any(CustomObjectCompositeIdentifier.class)))
                .thenReturn(supplyAsync(() -> {
                    throw new SphereException();
                }));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(1)
        );
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement()
            .isEqualTo(
                    format("Failed to update custom object with key: '%s'. Reason: %s",
                            CustomObjectCompositeIdentifier.of(newCustomObjectDraft).toString(),
                            exceptions.get(0).getMessage())
            );
        verify(customObjectService).upsertCustomObject(any());
        verify(customObjectService).fetchMatchingCustomObjects(any());
    }

    @Test
    void sync_WithDifferentIdentifiers_ShouldCreateSuccessfully() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("otherContainer");
        when(existingCustomObject.getKey()).thenReturn("otherKey");
        when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        assertThat(exceptions).hasSize(0);
        assertThat(errorMessages).hasSize(0);
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0)
        );
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
    }

    @Test
    void sync_WithSameKeysAndDifferentContainers_ShouldCreateSuccessfully() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("otherContainer");
        when(existingCustomObject.getKey()).thenReturn("someKey");
        when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        assertThat(exceptions).hasSize(0);
        assertThat(errorMessages).hasSize(0);
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0)
        );
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
    }

    @Test
    void sync_WithDifferentKeysAndSameContainers_ShouldCreateSuccessfully() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObject<JsonNode> existingCustomObject = mock(CustomObject.class);
        when(existingCustomObject.getContainer()).thenReturn("someContainer");
        when(existingCustomObject.getKey()).thenReturn("otherKey");
        when(existingCustomObject.getValue()).thenReturn(JsonNodeFactory.instance.numberNode(2020));

        final CustomObject<JsonNode> updatedCustomObject = mock(CustomObject.class);
        when(updatedCustomObject.getContainer()).thenReturn("someContainer");
        when(updatedCustomObject.getKey()).thenReturn("someKey");
        when(updatedCustomObject.getValue()).thenReturn(newCustomObjectDraft.getValue());

        final Set<CustomObject<JsonNode>> existingCustomObjectSet = new HashSet<CustomObject<JsonNode>>();
        existingCustomObjectSet.add(existingCustomObject);

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        assertThat(exceptions).hasSize(0);
        assertThat(errorMessages).hasSize(0);
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(0)
        );
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
    }

    @Test
    void sync_WitEmptyValidDrafts_ShouldFailed() {
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomObjectSyncOptions spyCustomObjectSyncOptions =
                initCustomObjectSyncOptions(errorMessages, exceptions);

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet())).thenReturn(completedFuture(emptySet()));
        when(customObjectService.upsertCustomObject(any())).thenReturn(completedFuture(Optional.empty()));

        // test
        CustomObjectSyncStatistics syncStatistics = new CustomObjectSync(
            spyCustomObjectSyncOptions, customObjectService).sync(
            singletonList(null)).toCompletableFuture().join();

        // assertion
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages).hasSize(1);
        assertAll(
            () -> assertThat(syncStatistics.getProcessed().get()).isEqualTo(1),
            () -> assertThat(syncStatistics.getCreated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getUpdated().get()).isEqualTo(0),
            () -> assertThat(syncStatistics.getFailed().get()).isEqualTo(1)
        );
    }

    @Nonnull
    private CustomObjectSyncOptions initCustomObjectSyncOptions(
            @Nonnull final List<String> errorMessages,
            @Nonnull final List<Throwable> exceptions) {
        return spy(CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .build());
    }

    @Test
    void sync_withChangedCustomObjectAndConcurrentModificationException_shouldRetryAndUpdateCustomObject() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();
        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
                "key1", newCustomObjectValue);

        final CustomObjectService customObjectService =
                buildMockCustomObjectServiceWithSuccessfulUpdateOnRetry(customObjectDraft);

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions, customObjectService);

        // Test
        final CustomObjectSyncStatistics statistics = customObjectSync.sync(singletonList(customObjectDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 1, 0);
        assertThat(exceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    void sync_withChangedCustomObjectWithBadGatewayExceptionInsideUpdateRetry_shouldFailToUpdate() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();
        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
                "key1", newCustomObjectValue);

        final CustomObjectService customObjectService =
                buildMockCustomObjectServiceWithFailedFetchOnRetry(customObjectDraft);

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions, customObjectService);

        // Test
        final CustomObjectSyncStatistics statistics = customObjectSync.sync(singletonList(customObjectDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);
        Assertions.assertThat(errorMessages).hasSize(1);
        Assertions.assertThat(exceptions).hasSize(1);
        Assertions.assertThat(errorMessages.get(0)).contains(
                format("Failed to update custom object with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                        + "after concurrency modification.", CustomObjectCompositeIdentifier.of(customObjectDraft)));
    }

    @Test
    void sync_withConcurrentModificationExceptionAndUnexpectedDelete_shouldFailToReFetchAndUpdate() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();
        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");

        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    exceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
                "key1", newCustomObjectValue);

        final CustomObjectService customObjectService =
                buildMockCustomObjectServiceWithNotFoundFetchOnRetry(customObjectDraft);

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions, customObjectService);

        // Test
        final CustomObjectSyncStatistics statistics = customObjectSync.sync(singletonList(customObjectDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 0, 1);
        Assertions.assertThat(errorMessages).hasSize(1);
        Assertions.assertThat(exceptions).hasSize(1);

        Assertions.assertThat(errorMessages.get(0)).contains(
            format("Failed to update custom object with key: '%s'. Reason: Not found when attempting to fetch while"
                + " retrying after concurrency modification.", CustomObjectCompositeIdentifier.of(customObjectDraft)));
    }

    @Nonnull
    private CustomObjectService buildMockCustomObjectServiceWithSuccessfulUpdateOnRetry(
            @Nonnull final CustomObjectDraft customObjectDraft) {

        final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

        final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
        when(mockCustomObject.getKey()).thenReturn("key1");
        when(mockCustomObject.getContainer()).thenReturn("container1");
        when(mockCustomObject.getValue())
                .thenReturn(JsonNodeFactory.instance.objectNode().put("type", "object"));

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(customObjectDraft.getKey(), UUID.randomUUID().toString());

        when(mockCustomObjectService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(singleton(mockCustomObject)));
        when(mockCustomObjectService.fetchCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(mockCustomObject)));
        when(mockCustomObjectService.upsertCustomObject(any()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(Optional.of(mockCustomObject)));
        return mockCustomObjectService;
    }

    @Test
    void sync_withNewCustomObjectAndBadRequest_shouldNotCreateButHandleError() {

        List<String> errorCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();
        final CustomObjectSyncOptions spyOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .warningCallback((exception, oldResource, newResource)
                    -> warningCallBackMessages.add(exception.getMessage()))
                .build();

        final ObjectNode newCustomObjectValue = JsonNodeFactory.instance.objectNode().put("name", "value2");
        final CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container1",
                "key1", newCustomObjectValue);
        final CustomObjectDraft<JsonNode> newCustomObjectDraft = CustomObjectDraft.ofUnversionedUpsert("container2",
                "key2", newCustomObjectValue);


        final CustomObjectService customObjectService =
                buildMockCustomObjectServiceWithBadRequestOnCreation(customObjectDraft);

        final CustomObjectSync customObjectSync = new CustomObjectSync(spyOptions, customObjectService);

        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
                .sync(Collections.singletonList(newCustomObjectDraft))
                .toCompletableFuture().join();

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        Assertions.assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadRequestException.class);
        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
                format("Failed to create custom object with key: '%s'.",
                        CustomObjectCompositeIdentifier.of(newCustomObjectDraft)));
    }

    @Nonnull
    private CustomObjectService buildMockCustomObjectServiceWithFailedFetchOnRetry(
            @Nonnull final CustomObjectDraft customObjectDraft) {

        final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

        final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
        when(mockCustomObject.getKey()).thenReturn("key1");
        when(mockCustomObject.getContainer()).thenReturn("container1");
        when(mockCustomObject.getValue())
                .thenReturn(JsonNodeFactory.instance.objectNode().put("type", "object"));

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(customObjectDraft.getKey(), UUID.randomUUID().toString());

        when(mockCustomObjectService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(singleton(mockCustomObject)));
        when(mockCustomObjectService.fetchCustomObject(any()))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));
        when(mockCustomObjectService.upsertCustomObject(any()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(Optional.of(mockCustomObject)));
        return mockCustomObjectService;
    }

    @Nonnull
    private CustomObjectService buildMockCustomObjectServiceWithNotFoundFetchOnRetry(
            @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {

        final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

        final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
        when(mockCustomObject.getKey()).thenReturn("key1");
        when(mockCustomObject.getContainer()).thenReturn("container1");
        when(mockCustomObject.getValue())
                .thenReturn(JsonNodeFactory.instance.objectNode().put("type", "object"));

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(customObjectDraft.getKey(), UUID.randomUUID().toString());

        when(mockCustomObjectService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(singleton(mockCustomObject)));
        when(mockCustomObjectService.fetchCustomObject(any()))
                .thenReturn(completedFuture(Optional.empty()));
        when(mockCustomObjectService.upsertCustomObject(any()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(Optional.of(mockCustomObject)));
        return mockCustomObjectService;
    }

    @Nonnull
    private CustomObjectService buildMockCustomObjectServiceWithBadRequestOnCreation(
            @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {

        final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

        final CustomObject<JsonNode> mockCustomObject = mock(CustomObject.class);
        when(mockCustomObject.getKey()).thenReturn("key1");
        when(mockCustomObject.getContainer()).thenReturn("container1");
        when(mockCustomObject.getValue())
                .thenReturn(JsonNodeFactory.instance.objectNode().put("type", "object"));

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(customObjectDraft.getKey(), UUID.randomUUID().toString());

        when(mockCustomObjectService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(emptySet()));
        when(mockCustomObjectService.fetchCustomObject(any()))
                .thenReturn(completedFuture(Optional.empty()));
        when(mockCustomObjectService.upsertCustomObject(any()))
                .thenReturn(exceptionallyCompletedFuture(
                        new CompletionException(new BadRequestException("bad request"))))
                .thenReturn(completedFuture(Optional.of(mockCustomObject)));
        return mockCustomObjectService;
    }
}
