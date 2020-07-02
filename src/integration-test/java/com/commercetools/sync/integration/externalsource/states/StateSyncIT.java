package com.commercetools.sync.integration.externalsource.states;

import com.commercetools.sync.states.StateSync;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.StateCreateCommand;
import io.sphere.sdk.states.commands.StateUpdateCommand;
import io.sphere.sdk.states.queries.StateQuery;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.deleteStates;
import static com.commercetools.sync.integration.commons.utils.StateITUtils.getStateByKey;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class StateSyncIT {

    @BeforeEach
    void setup() {
        deleteStates(CTP_TARGET_CLIENT);

        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1-key", StateType.LINE_ITEM_STATE)
            .name(LocalizedString.ofEnglish("state-name"))
            .description(LocalizedString.ofEnglish("state-desc"))
            .roles(Collections.singleton(StateRole.RETURN))
            .initial(false)
            .build();

        final State state = executeBlocking(CTP_TARGET_CLIENT.execute(StateCreateCommand.of(stateDraft)));
    }

    @AfterAll
    static void tearDown() {
        deleteStates(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_withNewState_shouldCreateState() {
        final StateDraft stateDraft = StateDraftBuilder
            .of("new-state", StateType.REVIEW_STATE)
            .roles(Collections.singleton(
                StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(1, 1, 0, 0, 0);
    }

    @Test
    void sync_WithUpdatedState_ShouldUpdateState() {
        // preparation
        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1-key", StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        // assertion
        assertThat(stateSyncStatistics).hasValues(1, 0, 1, 0, 0);

        final Optional<State> oldStateAfter = getStateByKey(CTP_TARGET_CLIENT, "state-1-key");

        Assertions.assertThat(oldStateAfter).hasValueSatisfying(state -> {
            Assertions.assertThat(state.getType()).isEqualTo(StateType.REVIEW_STATE);
            Assertions.assertThat(state.getName()).isEqualTo(ofEnglish("state-name-updated"));
            Assertions.assertThat(state.getDescription()).isEqualTo(ofEnglish("state-desc-updated"));
            Assertions.assertThat(state.getRoles())
                      .isEqualTo(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS));
            Assertions.assertThat(state.isInitial()).isEqualTo(true);
        });
    }

    @Test
    void sync_withEqualState_shouldNotUpdateState() {
        StateDraft stateDraft = StateDraftBuilder
            .of("state-1-key", StateType.LINE_ITEM_STATE)
            .name(ofEnglish("state-name"))
            .description(ofEnglish("state-desc"))
            .roles(Collections.singleton(StateRole.RETURN))
            .initial(false)
            .build();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(singletonList(stateDraft))
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(1, 0, 0, 0, 0);
    }

    @Test
    void sync_withChangedStateButConcurrentModificationException_shouldRetryAndUpdateState() {
        // preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdate();

        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final StateSyncOptions spyOptions = StateSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, throwable) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(throwable);
            })
            .warningCallback(warningCallBackMessages::add)
            .build();

        final StateSync stateSync = new StateSync(spyOptions);

        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1-key", StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncStatistics syncStatistics =
            executeBlocking(stateSync.sync(singletonList(stateDraft)));

        assertThat(syncStatistics).hasValues(1, 0, 1, 0, 0);
        Assertions.assertThat(errorCallBackExceptions).isEmpty();
        Assertions.assertThat(errorCallBackMessages).isEmpty();
        Assertions.assertThat(warningCallBackMessages).isEmpty();
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdate() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
        when(spyClient.execute(updateCommand))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
        // preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry();

        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final StateSyncOptions spyOptions = StateSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, throwable) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(throwable);
            })
            .warningCallback(warningCallBackMessages::add)
            .build();

        final StateSync stateSync = new StateSync(spyOptions);

        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1-key", StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncStatistics syncStatistics =
            executeBlocking(stateSync.sync(singletonList(stateDraft)));

        // Test and assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);

        Assertions.assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(BadGatewayException.class);
        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
            format("Failed to update state with key: '%s'. Reason: Failed to fetch from CTP while retrying "
                + "after concurrency modification.", stateDraft.getKey()));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndFailedFetchOnRetry() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final StateUpdateCommand updateCommand = any(StateUpdateCommand.class);
        when(spyClient.execute(updateCommand))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final StateQuery stateQuery = any(StateQuery.class);
        when(spyClient.execute(stateQuery))
            .thenCallRealMethod() // cache state keys
            .thenCallRealMethod() // Call real fetch on fetching matching states
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()));

        return spyClient;
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        // preparation
        final SphereClient spyClient = buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry();

        List<String> errorCallBackMessages = new ArrayList<>();
        List<String> warningCallBackMessages = new ArrayList<>();
        List<Throwable> errorCallBackExceptions = new ArrayList<>();
        final StateSyncOptions spyOptions = StateSyncOptionsBuilder
            .of(spyClient)
            .errorCallback((errorMessage, throwable) -> {
                errorCallBackMessages.add(errorMessage);
                errorCallBackExceptions.add(throwable);
            })
            .warningCallback(warningCallBackMessages::add)
            .build();

        final StateSync stateSync = new StateSync(spyOptions);

        final StateDraft stateDraft = StateDraftBuilder
            .of("state-1-key", StateType.REVIEW_STATE)
            .name(ofEnglish("state-name-updated"))
            .description(ofEnglish("state-desc-updated"))
            .roles(Collections.singleton(StateRole.REVIEW_INCLUDED_IN_STATISTICS))
            .initial(true)
            .build();

        final StateSyncStatistics syncStatistics =
            executeBlocking(stateSync.sync(singletonList(stateDraft)));

        // Test and assertion
        assertThat(syncStatistics).hasValues(1, 0, 0, 1, 0);
        Assertions.assertThat(errorCallBackMessages).hasSize(1);
        Assertions.assertThat(errorCallBackExceptions).hasSize(1);

        Assertions.assertThat(errorCallBackMessages.get(0)).contains(
            format("Failed to update state with key: '%s'. Reason: Not found when attempting to fetch while"
                + " retrying after concurrency modification.", stateDraft.getKey()));
    }

    @Nonnull
    private SphereClient buildClientWithConcurrentModificationUpdateAndNotFoundFetchOnRetry() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);

        final StateUpdateCommand stateUpdateCommand = any(StateUpdateCommand.class);
        when(spyClient.execute(stateUpdateCommand))
            .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new ConcurrentModificationException()))
            .thenCallRealMethod();

        final StateQuery stateQuery = any(StateQuery.class);

        when(spyClient.execute(stateQuery))
            .thenCallRealMethod() // cache state keys
            .thenCallRealMethod() // Call real fetch on fetching matching states
            .thenReturn(CompletableFuture.completedFuture(PagedQueryResult.empty()));

        return spyClient;
    }

    @Test
    void sync_WithSeveralBatches_ShouldReturnProperStatistics() {
        // 2 batches
        final List<StateDraft> stateDrafts = IntStream
            .range(0, 10)
            .mapToObj(i -> StateDraft
                .of("key" + i, StateType.REVIEW_STATE)
                .withName(ofEnglish("name" + i)))
            .collect(Collectors.toList());

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .batchSize(5)
            .build();

        final StateSync stateSync = new StateSync(stateSyncOptions);

        // test
        final StateSyncStatistics stateSyncStatistics = stateSync
            .sync(stateDrafts)
            .toCompletableFuture()
            .join();

        assertThat(stateSyncStatistics).hasValues(10, 10, 0, 0, 0);
    }

}
