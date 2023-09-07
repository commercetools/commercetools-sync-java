package com.commercetools.sync.customobjects;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.customobjects.models.NoopResourceUpdateAction;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class CustomObjectSyncOptionsBuilderTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private final CustomObjectSyncOptionsBuilder customObjectSyncOptionsBuilder =
      CustomObjectSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateCustomObjectSyncOptionsBuilder() {
    final CustomObjectSyncOptionsBuilder builder = CustomObjectSyncOptionsBuilder.of(CTP_CLIENT);
    assertThat(builder).isNotNull();
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    final CustomObjectSyncOptions customObjectSyncOptions = customObjectSyncOptionsBuilder.build();
    assertThat(customObjectSyncOptions).isNotNull();
    Assertions.assertThat(customObjectSyncOptions.getBeforeUpdateCallback()).isNull();
    Assertions.assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNull();
    Assertions.assertThat(customObjectSyncOptions.getErrorCallback()).isNull();
    Assertions.assertThat(customObjectSyncOptions.getWarningCallback()).isNull();
    Assertions.assertThat(customObjectSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT);
    Assertions.assertThat(customObjectSyncOptions.getBatchSize())
        .isEqualTo(CustomObjectSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    Assertions.assertThat(customObjectSyncOptions.getCacheSize()).isEqualTo(10_000);
  }

  @Test
  void beforeCreateCallback_WithFilterAsCallback_ShouldSetCallback() {
    customObjectSyncOptionsBuilder.beforeCreateCallback((newCustomObject) -> null);
    final CustomObjectSyncOptions customObjectSyncOptions = customObjectSyncOptionsBuilder.build();
    Assertions.assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void errorCallBack_WithCallBack_ShouldSetCallBack() {
    final QuadConsumer<
            SyncException,
            Optional<CustomObjectDraft>,
            Optional<CustomObject>,
            List<NoopResourceUpdateAction>>
        mockErrorCallBack = (exception, newResource, oldResource, updateActions) -> {};
    customObjectSyncOptionsBuilder.errorCallback(mockErrorCallBack);

    final CustomObjectSyncOptions customObjectSyncOptions = customObjectSyncOptionsBuilder.build();
    Assertions.assertThat(customObjectSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void warningCallBack_WithCallBack_ShouldSetCallBack() {
    final TriConsumer<SyncException, Optional<CustomObjectDraft>, Optional<CustomObject>>
        mockWarningCallBack = (exception, newResource, oldResource) -> {};
    customObjectSyncOptionsBuilder.warningCallback(mockWarningCallBack);
    final CustomObjectSyncOptions cutomObjectSyncOptions = customObjectSyncOptionsBuilder.build();
    Assertions.assertThat(cutomObjectSyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void getThis_ShouldReturnCorrectInstance() {
    final CustomObjectSyncOptionsBuilder instance = customObjectSyncOptionsBuilder.getThis();
    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(CustomObjectSyncOptionsBuilder.class);
    assertThat(instance).isEqualTo(customObjectSyncOptionsBuilder);
  }

  @Test
  void customObjectSyncOptionsBuilderSetters_ShouldBeCallableAfterBaseSyncOptionsBuildSetters() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(30)
            .beforeCreateCallback((newCustomObject) -> null)
            .beforeUpdateCallback((updateActions, newCustomObject, oldCustomObject) -> emptyList())
            .build();
    assertThat(customObjectSyncOptions).isNotNull();
  }

  @Test
  void batchSize_WithPositiveValue_ShouldSetBatchSize() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();
    Assertions.assertThat(customObjectSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void batchSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CustomObjectSyncOptions customObjectSyncOptionsWithZeroBatchSize =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();
    Assertions.assertThat(customObjectSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(CustomObjectSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    final CustomObjectSyncOptions customObjectSyncOptionsWithNegativeBatchSize =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();
    Assertions.assertThat(customObjectSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(CustomObjectSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void
      applyBeforeUpdateCallBack_WithNullReturnCallbackAndEmptyUpdateActions_ShouldReturnEmptyList() {
    final TriFunction<
            List<NoopResourceUpdateAction>,
            CustomObjectDraft,
            CustomObject,
            List<NoopResourceUpdateAction>>
        beforeUpdateCallback = (updateActions, newCustomObject, oldCustomObject) -> null;

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT)
            .beforeUpdateCallback(beforeUpdateCallback)
            .build();
    Assertions.assertThat(customObjectSyncOptions.getBeforeUpdateCallback()).isNotNull();

    final List<NoopResourceUpdateAction> updateActions = Collections.emptyList();

    final List<NoopResourceUpdateAction> filteredList =
        customObjectSyncOptions.applyBeforeUpdateCallback(
            updateActions, mock(CustomObjectDraft.class), mock(CustomObject.class));
    assertThat(filteredList).isEqualTo(updateActions);
    assertThat(filteredList).isEmpty();
  }

  @Test
  void applyBeforeCreateCallBack_WithCallback_ShouldReturnFilteredDraft() {
    final Function<CustomObjectDraft, CustomObjectDraft> draftFunction =
        customObjectDraft ->
            CustomObjectDraftBuilder.of()
                .container(customObjectDraft.getContainer() + "_filteredContainer")
                .key(customObjectDraft.getKey() + "_filteredKey")
                .value(customObjectDraft.getValue())
                .build();

    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    Assertions.assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNotNull();
    final CustomObjectDraft resourceDraft = mock(CustomObjectDraft.class);
    when(resourceDraft.getKey()).thenReturn("myKey");
    when(resourceDraft.getContainer()).thenReturn("myContainer");
    when(resourceDraft.getValue()).thenReturn("myValue");
    final Optional<CustomObjectDraft> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);
    assertThat(filteredDraft).isNotEmpty();
    assertThat(filteredDraft.get().getKey()).isEqualTo("myKey_filteredKey");
    assertThat(filteredDraft.get().getContainer()).isEqualTo("myContainer_filteredContainer");
  }

  @Test
  void applyBeforeCreateCallBack_WithNullCallback_ShouldReturnIdenticalDraftInOptional() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).build();
    Assertions.assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNull();
    final CustomObjectDraft resourceDraft = mock(CustomObjectDraft.class);
    final Optional<CustomObjectDraft> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);
    assertThat(filteredDraft).containsSame(resourceDraft);
  }

  @Test
  void applyBeforeCreateCallBack_WithCallbackReturningNull_ShouldReturnEmptyOptional() {
    final Function<CustomObjectDraft, CustomObjectDraft> draftFunction = customObjectDraft -> null;
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).beforeCreateCallback(draftFunction).build();
    Assertions.assertThat(customObjectSyncOptions.getBeforeCreateCallback()).isNotNull();
    final CustomObjectDraft resourceDraft = mock(CustomObjectDraft.class);
    final Optional<CustomObjectDraft> filteredDraft =
        customObjectSyncOptions.applyBeforeCreateCallback(resourceDraft);
    assertThat(filteredDraft).isEmpty();
  }

  @Test
  void cacheSize_WithPositiveValue_ShouldSetCacheSize() {
    final CustomObjectSyncOptions customObjectSyncOptions =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();
    Assertions.assertThat(customObjectSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void cacheSize_WithZeroOrNegativeValue_ShouldFallBackToDefaultValue() {
    final CustomObjectSyncOptions customObjectSyncOptionsWithZeroCacheSize =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();
    Assertions.assertThat(customObjectSyncOptionsWithZeroCacheSize.getCacheSize())
        .isEqualTo(10_000);

    final CustomObjectSyncOptions customObjectSyncOptionsWithNegativeCacheSize =
        CustomObjectSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();
    Assertions.assertThat(customObjectSyncOptionsWithNegativeCacheSize.getCacheSize())
        .isEqualTo(10_000);
  }
}
