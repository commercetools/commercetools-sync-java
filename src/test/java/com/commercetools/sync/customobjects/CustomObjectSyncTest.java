package com.commercetools.sync.customobjects;

import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.services.CustomObjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.models.SphereException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
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

        final CustomObjectSyncOptions syncOptions = CustomObjectSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception.getCause());
            })
            .build();

        final CustomObjectService mockCustomObjectService = mock(CustomObjectService.class);

        when(mockCustomObjectService.fetchMatchingCustomObjects(anySet()))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        final CustomObjectSync customObjectSync = new CustomObjectSync(syncOptions, mockCustomObjectService);

        // test
        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
            .sync(singletonList(newCustomObjectDraft))
            .toCompletableFuture().join();

        // assertions
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
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet())).thenReturn(completedFuture(emptySet()));
        when(customObjectService.upsertCustomObject(any())).thenReturn(completedFuture(Optional.empty()));

        final CustomObjectSyncOptions spyCustomObjectSyncOptions = spy(customObjectSyncOptions);

        // test
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
        verify(spyCustomObjectSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldCallBeforeCreateCallback_ShouldNotCallBeforeUpdateCallback() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final CustomObject<JsonNode> mockedExistingCustomObject = mock(CustomObject.class);
        when(mockedExistingCustomObject.getKey()).thenReturn(newCustomObjectDraft.getKey());

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
            .thenReturn(completedFuture(singleton(mockedExistingCustomObject)));
        when(customObjectService.upsertCustomObject(any()))
            .thenReturn(completedFuture(Optional.of(mockedExistingCustomObject)));

        final CustomObjectSyncOptions spyCustomObjectSyncOptions = spy(customObjectSyncOptions);

        // test
        new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
            .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);
        verify(spyCustomObjectSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithSameIndentifiersAndDifferentValues_ShoudUpdateSucessfully() {
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

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        final CustomObjectSyncOptions spyCustomObjectSyncOptions = spy(customObjectSyncOptions);

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();


        assertThat(syncStatistics.getProcessed().get()).isEqualTo(1);
        assertThat(syncStatistics.getUpdated().get()).isEqualTo(1);
    }

    @Test
    void sync_WithDifferentIndentifiers_ShoudUpdateSucessfully() {
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

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet()))
                .thenReturn(completedFuture(existingCustomObjectSet));
        when(customObjectService.upsertCustomObject(any()))
                .thenReturn(completedFuture(Optional.of(updatedCustomObject)));

        final CustomObjectSyncOptions spyCustomObjectSyncOptions = spy(customObjectSyncOptions);

        // test
        CustomObjectSyncStatistics syncStatistics =
                new CustomObjectSync(spyCustomObjectSyncOptions, customObjectService)
                        .sync(singletonList(newCustomObjectDraft)).toCompletableFuture().join();

        // assertion
        verify(spyCustomObjectSyncOptions).applyBeforeCreateCallback(newCustomObjectDraft);

        assertThat(syncStatistics.getProcessed().get()).isEqualTo(1);
        assertThat(syncStatistics.getCreated().get()).isEqualTo(1);
    }

    @Test
    void sync_WitEmptyValidDrafts_ShouldFailed() {
        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(customObjectService.fetchMatchingCustomObjects(anySet())).thenReturn(completedFuture(emptySet()));
        when(customObjectService.upsertCustomObject(any())).thenReturn(completedFuture(Optional.empty()));

        final CustomObjectSyncOptions spyCustomObjectSyncOptions = spy(customObjectSyncOptions);

        // test
        CustomObjectSyncStatistics syncStatistics = new CustomObjectSync(
            spyCustomObjectSyncOptions, customObjectService).sync(
            singletonList(null)).toCompletableFuture().join();

        // assertion
        assertThat(syncStatistics.getProcessed().get()).isEqualTo(1);
        assertThat(syncStatistics.getCreated().get()).isEqualTo(0);
        assertThat(syncStatistics.getFailed().get()).isEqualTo(1);

    }
}
