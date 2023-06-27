package com.commercetools.sync.sdk2.types;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.sync.sdk2.services.TypeService;
import com.commercetools.sync.sdk2.types.helpers.TypeSyncStatistics;
import io.vrap.rmf.base.client.error.BaseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

class TypeSyncTest {
  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key("foo")
            .name(ofEnglish("name"))
            .resourceTypeIds(ResourceTypeId.CATEGORY)
            .description(ofEnglish("desc"))
            .fieldDefinitions(emptyList())
            .build();
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final TypeSyncOptions syncOptions =
        TypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final TypeService mockTypeService = mock(TypeService.class);

    when(mockTypeService.fetchMatchingTypesByKeys(singleton(newTypeDraft.getKey())))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BaseException();
                }));

    final TypeSync typeSync = new TypeSync(syncOptions, mockTypeService);

    // test
    final TypeSyncStatistics typeSyncStatistics =
        typeSync.sync(singletonList(newTypeDraft)).toCompletableFuture().join();

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to fetch existing types with keys: '[foo]'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(BaseException.class);

    assertThat(typeSyncStatistics).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key("newType")
            .name(ofEnglish("typeName"))
            .resourceTypeIds(ResourceTypeId.CHANNEL)
            .build();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final TypeService typeService = mock(TypeService.class);
    when(typeService.fetchMatchingTypesByKeys(anySet())).thenReturn(completedFuture(emptySet()));
    when(typeService.createType(any())).thenReturn(completedFuture(Optional.empty()));

    final TypeSyncOptions spyTypeSyncOptions = spy(typeSyncOptions);

    // test
    new TypeSync(spyTypeSyncOptions, typeService)
        .sync(singletonList(newTypeDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyTypeSyncOptions).applyBeforeCreateCallback(newTypeDraft);
    verify(spyTypeSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final TypeDraft newTypeDraft =
        TypeDraftBuilder.of()
            .key("newType")
            .name(ofEnglish("typeName"))
            .resourceTypeIds(ResourceTypeId.CHANNEL)
            .build();

    final TypeSyncOptions typeSyncOptions =
        TypeSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    final Type mockedExistingType = mock(Type.class);
    when(mockedExistingType.getKey()).thenReturn(newTypeDraft.getKey());

    final TypeService typeService = mock(TypeService.class);
    when(typeService.fetchMatchingTypesByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockedExistingType)));
    when(typeService.updateType(any(), any())).thenReturn(completedFuture(mockedExistingType));

    final TypeSyncOptions spyTypeSyncOptions = spy(typeSyncOptions);

    // test
    new TypeSync(spyTypeSyncOptions, typeService)
        .sync(singletonList(newTypeDraft))
        .toCompletableFuture()
        .join();

    // assertion
    verify(spyTypeSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyTypeSyncOptions, never()).applyBeforeCreateCallback(newTypeDraft);
  }
}
