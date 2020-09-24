package com.commercetools.sync.customobjects;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.services.CustomObjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.models.SphereException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
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
                + "'[{key='someKey', container='someContainer'}]'.");

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
}
