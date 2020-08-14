package com.commercetools.sync.states;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

class StateSyncOptionsBuilderTest {

    private static final SphereClient CTP_CLIENT = mock(SphereClient.class);
    private StateSyncOptionsBuilder stateSyncOptionsBuilder = StateSyncOptionsBuilder.of(CTP_CLIENT);

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
            () -> assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNull(),
            () -> assertThat(stateSyncOptions.getBeforeCreateCallback()).isNull(),
            () -> assertThat(stateSyncOptions.getErrorCallback()).isNull(),
            () -> assertThat(stateSyncOptions.getWarningCallback()).isNull(),
            () -> assertThat(stateSyncOptions.getCtpClient()).isEqualTo(CTP_CLIENT),
            () -> assertThat(stateSyncOptions.getBatchSize()).isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT)
        );
    }

    @Test
    void build_WithBeforeUpdateCallback_ShouldSetBeforeUpdateCallback() {
        final TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>>
            beforeUpdateCallback = (updateActions, newState, oldState) -> emptyList();
        stateSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getBeforeUpdateCallback()).isNotNull();
    }

    @Test
    void build_WithBeforeCreateCallback_ShouldSetBeforeCreateCallback() {
        stateSyncOptionsBuilder.beforeCreateCallback((newState) -> null);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getBeforeCreateCallback()).isNotNull();
    }

    @Test
    void build_WithErrorCallback_ShouldSetErrorCallback() {
        final QuadConsumer<SyncException, Optional<StateDraft>, Optional<State>,
            List<UpdateAction<State>>> mockErrorCallback = (exception, newDraft, old, actions) -> { };
        stateSyncOptionsBuilder.errorCallback(mockErrorCallback);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getErrorCallback()).isNotNull();
    }

    @Test
    void build_WithWarningCallback_ShouldSetWarningCallback() {
        final TriConsumer<SyncException, Optional<StateDraft>, Optional<State>> mockWarningCallback =
            (exception, newDraft, old) -> { };
        stateSyncOptionsBuilder.warningCallback(mockWarningCallback);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getWarningCallback()).isNotNull();
    }


    @Test
    void build_WithBatchSize_ShouldSetBatchSize() {
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(10)
            .build();

        assertThat(stateSyncOptions.getBatchSize()).isEqualTo(10);
    }

    @Test
    void build_WithInvalidBatchSize_ShouldBuildSyncOptions() {
        StateSyncOptions stateSyncOptionsWithZeroBatchSize = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(0)
            .build();

        assertThat(stateSyncOptionsWithZeroBatchSize.getBatchSize())
            .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        StateSyncOptions stateSyncOptionsWithNegativeBatchSize = StateSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();

        assertThat(stateSyncOptionsWithNegativeBatchSize.getBatchSize())
            .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

}
