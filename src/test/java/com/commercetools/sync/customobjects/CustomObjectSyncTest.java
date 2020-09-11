package com.commercetools.sync.customobjects;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.customobjects.helpers.CustomObjectSyncStatistics;
import com.commercetools.sync.services.CustomObjectService;
import com.commercetools.sync.types.TypeSync;
import com.commercetools.sync.types.TypeSyncOptions;
import com.commercetools.sync.types.TypeSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
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
    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation


        final CustomObjectDraft<JsonNode> newCustomObjectDraft = CustomObjectDraft
                .ofUnversionedUpsert("someName", "someKey",
                        JsonNodeFactory.instance.objectNode().put( "json-field", "json-value"));
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
        final CustomObjectCompositeIdentifier customObjectCompositeIdentifier = CustomObjectCompositeIdentifier.of("someKey", "someName");

        when(mockCustomObjectService.fetchMatchingCustomObjects(singleton(customObjectCompositeIdentifier)))
               .thenReturn(supplyAsync(() -> { throw new SphereException();}));

        final CustomObjectSync customObjectSync = new CustomObjectSync(syncOptions, mockCustomObjectService);

        // test
        final CustomObjectSyncStatistics customObjectSyncStatistics = customObjectSync
                .sync(singletonList(newCustomObjectDraft))
                .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(message ->
                        assertThat(message).isEqualTo("Failed to fetch existing customObjects with keys: '[{key='someKey', container='someName'}]'.")
                );

        assertThat(exceptions)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(throwable -> {
                    assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                    assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
                });

        assertThat(customObjectSyncStatistics).hasValues(1, 0, 0, 1);
    }
/*

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final CustomObjectDraft<JsonNode> newCustomObjectDraft = CustomObjectDraft.ofUnversionedUpsert("someName", "someKey", JsonNodeFactory.instance.objectNode().put( "json-field", "json-value"));

        final CustomObjectSyncOptions customObjectSyncOptions = CustomObjectSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        final CustomObjectService customObjectService = mock(CustomObjectService.class);
        when(typeService.fetchMatchingTypesByKeys(anySet())).thenReturn(completedFuture(emptySet()));
        when(typeService.createType(any())).thenReturn(completedFuture(Optional.empty()));

        final TypeSyncOptions spyTypeSyncOptions = spy(typeSyncOptions);

        // test
        new TypeSync(spyTypeSyncOptions, typeService)
                .sync(singletonList(newTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyTypeSyncOptions).applyBeforeCreateCallback(newTypeDraft);
        verify(spyTypeSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
    }

    @Test
    void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
        // preparation
        final TypeDraft newTypeDraft = TypeDraftBuilder
                .of("newType", ofEnglish( "typeName"), ResourceTypeIdsSetBuilder.of().addChannels())
                .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .build();

        final Type mockedExistingType = mock(Type.class);
        when(mockedExistingType.getKey()).thenReturn(newTypeDraft.getKey());

        final TypeService typeService = mock(TypeService.class);
        when(typeService.fetchMatchingTypesByKeys(anySet())).thenReturn(completedFuture(singleton(mockedExistingType)));
        when(typeService.updateType(any(), any())).thenReturn(completedFuture(mockedExistingType));

        final TypeSyncOptions spyTypeSyncOptions = spy(typeSyncOptions);

        // test
        new TypeSync(spyTypeSyncOptions, typeService)
                .sync(singletonList(newTypeDraft)).toCompletableFuture().join();

        // assertion
        verify(spyTypeSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
        verify(spyTypeSyncOptions, never()).applyBeforeCreateCallback(newTypeDraft);
    }
*/
}
