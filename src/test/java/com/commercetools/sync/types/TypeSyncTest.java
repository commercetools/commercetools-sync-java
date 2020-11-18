package com.commercetools.sync.types;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.types.helpers.TypeSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypeSyncTest {
    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        // preparation

        final TypeDraft newTypeDraft = TypeDraftBuilder.of(
            "foo",
            ofEnglish("name"),
            ResourceTypeIdsSetBuilder.of().addCategories().build())
                                                       .description(ofEnglish("desc"))
                                                       .fieldDefinitions(emptyList())
                                                       .build();
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeSyncOptions syncOptions = TypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception.getCause());
            })
            .build();


        final TypeService mockTypeService = mock(TypeService.class);

        when(mockTypeService.fetchMatchingTypesByKeys(singleton(newTypeDraft.getKey())))
            .thenReturn(supplyAsync(() -> { throw new SphereException(); }));

        final TypeSync typeSync = new TypeSync(syncOptions, mockTypeService);

        // test
        final TypeSyncStatistics typeSyncStatistics = typeSync
            .sync(singletonList(newTypeDraft))
            .toCompletableFuture().join();

        // assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement().satisfies(message ->
                assertThat(message).isEqualTo("Failed to fetch existing types with keys: '[foo]'.")
            );

        assertThat(exceptions)
            .hasSize(1)
            .singleElement().satisfies(throwable -> {
                assertThat(throwable).isExactlyInstanceOf(CompletionException.class);
                assertThat(throwable).hasCauseExactlyInstanceOf(SphereException.class);
            });

        assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
    }

    @Test
    void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
        // preparation
        final TypeDraft newTypeDraft = TypeDraftBuilder
            .of("newType", ofEnglish( "typeName"), ResourceTypeIdsSetBuilder.of().addChannels())
            .build();

        final TypeSyncOptions typeSyncOptions = TypeSyncOptionsBuilder
            .of(mock(SphereClient.class))
            .build();

        final TypeService typeService = mock(TypeService.class);
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

    @Test
    void syncDrafts_WithConcurrentModificationException_ShouldRetryToUpdateNewProductTypeWithSuccess() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final List<String> warningCallBackMessages = new ArrayList<>();

        final TypeDraft typeDraft = TypeDraftBuilder
                .of("key_1", LocalizedString.ofEnglish("name_1"), ResourceTypeIdsSetBuilder.of().addChannels())
                .build();

        final TypeService typeService =
                buildMockTypeServiceWithSuccessfulUpdateOnRetry(typeDraft);

        final TypeSyncOptions syncOptions =
                TypeSyncOptionsBuilder.of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .warningCallback((exception, oldResource, newResource)
                            -> warningCallBackMessages.add(exception.getMessage()))
                        .build();

        final TypeSync typeSync = new TypeSync(syncOptions, typeService);

        // Test
        final TypeSyncStatistics statistics = typeSync.sync(singletonList(typeDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        AssertionsForStatistics.assertThat(statistics).hasValues(1, 0, 1, 0);
        assertThat(exceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }

    @Test
    void syncDrafts_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeDraft typeDraft = TypeDraftBuilder
                .of("key_1", LocalizedString.ofEnglish("name_1"), ResourceTypeIdsSetBuilder.of().addChannels())
                .build();

        final TypeService typeService =
                buildMockTypeServiceWithFailedFetchOnRetry(typeDraft);

        final TypeSyncOptions syncOptions =
                TypeSyncOptionsBuilder.of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .build();

        final TypeSync typeSync = new TypeSync(syncOptions, typeService);

        // Test
        final TypeSyncStatistics statistics = typeSync.sync(singletonList(typeDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update type with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                        + "after concurrency modification.", typeDraft.getKey()));

    }

    @Test
    void syncDrafts_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        // Preparation
        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final TypeDraft typeDraft = TypeDraftBuilder
                .of("key_1", LocalizedString.ofEnglish("name_1"), ResourceTypeIdsSetBuilder.of().addChannels())
                .build();

        final TypeService typeService =
                buildMockTypeServiceWithNotFoundFetchOnRetry(typeDraft);

        final TypeSyncOptions syncOptions =
                TypeSyncOptionsBuilder.of(mock(SphereClient.class))
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorMessages.add(exception.getMessage());
                            exceptions.add(exception.getCause());
                        })
                        .build();

        final TypeSync typeSync = new TypeSync(syncOptions, typeService);

        // Test
        final TypeSyncStatistics statistics = typeSync.sync(singletonList(typeDraft))
                .toCompletableFuture()
                .join();

        // Assertion
        assertThat(statistics).hasValues(1, 0, 0, 1);
        assertThat(errorMessages).hasSize(1);
        assertThat(exceptions).hasSize(1);
        assertThat(errorMessages.get(0)).contains(
                format("Failed to update type with key: '%s'. Reason: Not found when attempting to fetch while "
                        + "retrying after concurrency modification.", typeDraft.getKey()));
    }

    @Nonnull
    private TypeService buildMockTypeServiceWithSuccessfulUpdateOnRetry(
            @Nonnull final TypeDraft typeDraft) {

        final TypeService mockTypeService = mock(TypeService.class);

        final Type mockType = mock(Type.class);
        when(mockType.getKey()).thenReturn("key_1");

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(typeDraft.getKey(), UUID.randomUUID().toString());

        when(mockTypeService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockTypeService.fetchMatchingTypesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockType)));
        when(mockTypeService.fetchType(any())).thenReturn(completedFuture(Optional.of(mockType)));
        when(mockTypeService.updateType(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockType));
        return mockTypeService;
    }

    @Nonnull
    private TypeService buildMockTypeServiceWithFailedFetchOnRetry(
            @Nonnull final TypeDraft typeDraft) {

        final TypeService mockTypeService = mock(TypeService.class);

        final Type mockType = mock(Type.class);
        when(mockType.getKey()).thenReturn("key_1");

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(typeDraft.getKey(), UUID.randomUUID().toString());

        when(mockTypeService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockTypeService.fetchMatchingTypesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockType)));
        when(mockTypeService.fetchType(any()))
                .thenReturn(exceptionallyCompletedFuture(new BadGatewayException()));
        when(mockTypeService.updateType(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockType));
        return mockTypeService;
    }

    @Nonnull
    private TypeService buildMockTypeServiceWithNotFoundFetchOnRetry(
            @Nonnull final TypeDraft typeDraft) {

        final TypeService mockTypeService = mock(TypeService.class);

        final Type mockType = mock(Type.class);
        when(mockType.getKey()).thenReturn("key_1");

        final Map<String, String> keyToIds = new HashMap<>();
        keyToIds.put(typeDraft.getKey(), UUID.randomUUID().toString());

        when(mockTypeService.cacheKeysToIds(anySet())).thenReturn(completedFuture(keyToIds));
        when(mockTypeService.fetchMatchingTypesByKeys(anySet()))
                .thenReturn(completedFuture(singleton(mockType)));
        when(mockTypeService.fetchType(any())).thenReturn(completedFuture(Optional.empty()));
        when(mockTypeService.updateType(any(), anyList()))
                .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
                .thenReturn(completedFuture(mockType));
        return mockTypeService;
    }
}
