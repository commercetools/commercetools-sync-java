package com.commercetools.sync.states;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StateSyncOptionsBuilderTest {

  private static final ProjectApiRoot CTP_CLIENT = mock(ProjectApiRoot.class);
  private final StateSyncOptionsBuilder stateSyncOptionsBuilder =
      StateSyncOptionsBuilder.of(CTP_CLIENT);

  @Test
  void of_WithClient_ShouldCreateStateSyncOptionsBuilder() {
    StateSyncOptionsBuilder builder = StateSyncOptionsBuilder.of(CTP_CLIENT);

    assertThat(builder).isNotNull();
  }

  @Test
  void getThis_ShouldReturnBuilderInstance() {
    StateSyncOptionsBuilder instance = stateSyncOptionsBuilder.getThis();

    assertThat(instance).isNotNull();
    assertThat(instance).isInstanceOf(StateSyncOptionsBuilder.class);
  }

  @Test
  void build_WithClient_ShouldBuildSyncOptions() {
    StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

    assertThat(stateSyncOptions).isNotNull();
    assertAll(
        () -> Assertions.assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNull(),
        () -> Assertions.assertThat(stateSyncOptions.getBeforeCreateCallback()).isNull(),
        () -> Assertions.assertThat(stateSyncOptions.getErrorCallback()).isNull(),
        () -> Assertions.assertThat(stateSyncOptions.getWarningCallback()).isNull(),
        () -> Assertions.assertThat(stateSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT),
        () ->
            Assertions.assertThat(stateSyncOptions.getBatchSize())
                .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT),
        () -> Assertions.assertThat(stateSyncOptions.getCacheSize()).isEqualTo(10_000));
  }

  @Test
  void build_WithBeforeUpdateCallback_ShouldSetBeforeUpdateCallback() {
    final TriFunction<List<StateUpdateAction>, StateDraft, State, List<StateUpdateAction>>
        beforeUpdateCallback = (updateActions, newState, oldState) -> emptyList();
    stateSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

    StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

    Assertions.assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNotNull();
  }

  @Test
  void build_WithBeforeCreateCallback_ShouldSetBeforeCreateCallback() {
    stateSyncOptionsBuilder.beforeCreateCallback((newState) -> null);

    StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

    Assertions.assertThat(stateSyncOptions.getBeforeCreateCallback()).isNotNull();
  }

  @Test
  void build_WithErrorCallback_ShouldSetErrorCallback() {
    final QuadConsumer<
            SyncException, Optional<StateDraft>, Optional<State>, List<StateUpdateAction>>
        mockErrorCallback = (exception, newDraft, old, actions) -> {};
    stateSyncOptionsBuilder.errorCallback(mockErrorCallback);

    StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

    Assertions.assertThat(stateSyncOptions.getErrorCallback()).isNotNull();
  }

  @Test
  void build_WithWarningCallback_ShouldSetWarningCallback() {
    final TriConsumer<SyncException, Optional<StateDraft>, Optional<State>> mockWarningCallback =
        (exception, newDraft, old) -> {};
    stateSyncOptionsBuilder.warningCallback(mockWarningCallback);

    StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

    Assertions.assertThat(stateSyncOptions.getWarningCallback()).isNotNull();
  }

  @Test
  void build_WithBatchSize_ShouldSetBatchSize() {
    StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).batchSize(10).build();

    Assertions.assertThat(stateSyncOptions.getBatchSize()).isEqualTo(10);
  }

  @Test
  void build_WithInvalidBatchSize_ShouldBuildSyncOptions() {
    StateSyncOptions stateSyncOptionsWithZeroBatchSize =
        StateSyncOptionsBuilder.of(CTP_CLIENT).batchSize(0).build();

    Assertions.assertThat(stateSyncOptionsWithZeroBatchSize.getBatchSize())
        .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

    StateSyncOptions stateSyncOptionsWithNegativeBatchSize =
        StateSyncOptionsBuilder.of(CTP_CLIENT).batchSize(-100).build();

    Assertions.assertThat(stateSyncOptionsWithNegativeBatchSize.getBatchSize())
        .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
  }

  @Test
  void build_WithCacheSize_ShouldSetCacheSize() {
    StateSyncOptions stateSyncOptions =
        StateSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(10).build();

    Assertions.assertThat(stateSyncOptions.getCacheSize()).isEqualTo(10);
  }

  @Test
  void build_WithZeroOrNegativeCacheSize_ShouldBuildSyncOptions() {
    StateSyncOptions stateSyncOptionsWithZeroCacheSize =
        StateSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(0).build();

    Assertions.assertThat(stateSyncOptionsWithZeroCacheSize.getCacheSize()).isEqualTo(10_000);

    StateSyncOptions stateSyncOptionsWithNegativeCacheSize =
        StateSyncOptionsBuilder.of(CTP_CLIENT).cacheSize(-100).build();

    Assertions.assertThat(stateSyncOptionsWithNegativeCacheSize.getCacheSize()).isEqualTo(10_000);
  }
}
