package com.commercetools.sync.states;

import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

class StateSyncOptionsBuilderTest {

    private static SphereClient CTP_CLIENT = mock(SphereClient.class);
    private StateSyncOptionsBuilder stateSyncOptionsBuilder = StateSyncOptionsBuilder.of(CTP_CLIENT);

    @Test
    void of_WithClient_ShouldCreateStateSyncOptionsBuilder() {
        StateSyncOptionsBuilder builder = StateSyncOptionsBuilder.of(CTP_CLIENT);

        assertThat(builder).as("Builder should be created").isNotNull();
    }

    @Test
    void getThis_ShouldReturnBuilderInstance() {
        StateSyncOptionsBuilder instance = stateSyncOptionsBuilder.getThis();

        assertThat(instance).as("Builder should not be null").isNotNull();
        assertThat(instance).as("Builder is instance of 'StateSyncOptionsBuilder'")
            .isInstanceOf(StateSyncOptionsBuilder.class);
    }

    @Test
    void build_WithClient_ShouldBuildSyncOptions() {
        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions).as("Created instance should not be null").isNotNull();
        assertAll(
            () -> assertThat(stateSyncOptions.getBeforeUpdateCallback())
                .as("'beforeUpdateCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getBeforeCreateCallback())
                .as("'beforeCreateCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getErrorCallBack())
                .as("'errorCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getWarningCallBack())
                .as("'warningCallback' should be null").isNull(),
            () -> assertThat(stateSyncOptions.getCtpClient())
                .as("'ctpClient' should be equal to prepared one").isEqualTo(CTP_CLIENT),
            () -> assertThat(stateSyncOptions.getBatchSize())
                .as("'batchSize' should be equal to default value")
                .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT)
        );
    }

    @Test
    void build_WithBeforeUpdateCallback_ShouldSetBeforeUpdateCallback() {
        TriFunction<List<UpdateAction<State>>, StateDraft, State, List<UpdateAction<State>>>
            beforeUpdateCallback = (updateActions, newState, oldState) -> emptyList();
        stateSyncOptionsBuilder.beforeUpdateCallback(beforeUpdateCallback);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getBeforeUpdateCallback())
            .as("'beforeUpdateCallback' should not be null").isNotNull();
    }

    @Test
    void build_WithBeforeCreateCallback_ShouldSetBeforeCreateCallback() {
        stateSyncOptionsBuilder.beforeCreateCallback((newState) -> null);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getBeforeCreateCallback())
            .as("'beforeCreateCallback' should be null").isNotNull();
    }

    @Test
    void build_WithErrorCallback_ShouldSetErrorCallback() {
        BiConsumer<String, Throwable> mockErrorCallBack = (errorMessage, errorException) -> {
        };
        stateSyncOptionsBuilder.errorCallback(mockErrorCallBack);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getErrorCallBack()).as("'errorCallback' should be null").isNotNull();
    }

    @Test
    void build_WithWarningCallback_ShouldSetWarningCallback() {
        Consumer<String> mockWarningCallBack = (warningMessage) -> {
        };
        stateSyncOptionsBuilder.warningCallback(mockWarningCallBack);

        StateSyncOptions stateSyncOptions = stateSyncOptionsBuilder.build();

        assertThat(stateSyncOptions.getWarningCallBack()).as("'warningCallback' should be null").isNotNull();
    }


    @Test
    void build_WithBatchSize_ShouldSetBatchSize() {
        StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(10)
            .build();

        assertThat(stateSyncOptions.getBatchSize()).as("'batchSize' should be equal to prepared value")
            .isEqualTo(10);
    }

    @Test
    void build_WithInvalidBatchSize_ShouldBuildSyncOptions() {
        StateSyncOptions stateSyncOptionsWithZeroBatchSize = StateSyncOptionsBuilder.of(CTP_CLIENT)
            .batchSize(0)
            .build();

        assertThat(stateSyncOptionsWithZeroBatchSize.getBatchSize())
            .as("'batchSize' should be equal to default value")
            .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT);

        StateSyncOptions stateSyncOptionsWithNegativeBatchSize = StateSyncOptionsBuilder
            .of(CTP_CLIENT)
            .batchSize(-100)
            .build();

        assertThat(stateSyncOptionsWithNegativeBatchSize.getBatchSize())
            .as("'batchSize' should be equal to default value")
            .isEqualTo(StateSyncOptionsBuilder.BATCH_SIZE_DEFAULT);
    }

}
