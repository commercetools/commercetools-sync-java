package com.commercetools.sync.states;

import com.commercetools.sync.services.StateService;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.services.impl.StateServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.states.utils.StateSyncUtils.buildActions;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetTransitionsAction;
import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class StateSync extends BaseSync<StateDraft, StateSyncStatistics, StateSyncOptions> {

    private static final String CTP_STATE_UPDATE_FAILED = "Failed to update state with key: '%s'. Reason: %s";

    private final StateService stateService;

    public StateSync(@Nonnull final StateSyncOptions stateSyncOptions) {
        this(stateSyncOptions, new StateServiceImpl(stateSyncOptions));
    }

    /**
     * Takes a {@link StateSyncOptions} and a {@link StateSync} instances to instantiate
     * a new {@link StateSync} instance that could be used to sync state drafts in the CTP project specified
     * in the injected {@link StateSyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param stateSyncOptions the container of all the options of the sync process including the CTP project
     *                         client and/or configuration and other sync-specific options.
     * @param stateService     the type service which is responsible for fetching/caching the Types from the CTP
     *                         project.
     */
    StateSync(@Nonnull final StateSyncOptions stateSyncOptions,
              @Nonnull final StateService stateService) {
        super(new StateSyncStatistics(), stateSyncOptions);
        this.stateService = stateService;
    }

    @Override
    protected CompletionStage<StateSyncStatistics> process(@Nonnull final List<StateDraft> resourceDrafts) {
        List<List<StateDraft>> batches = batchElements(resourceDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, completedFuture(statistics));
    }

    @Override
    protected CompletionStage<StateSyncStatistics> processBatch(@Nonnull final List<StateDraft> batch) {
        final Set<StateDraft> validStateDrafts = batch.stream()
            .filter(this::validateDraft)
            .collect(toSet());

        if (validStateDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        } else {
            final Set<String> keys = validStateDrafts.stream().map(StateDraft::getKey).collect(toSet());
            return stateService
                .fetchMatchingStatesByKeys(keys)
                .handle(ImmutablePair::new)
                .thenCompose(fetchResponse -> {
                    Set<State> fetchedStates = fetchResponse.getKey();
                    final Throwable exception = fetchResponse.getValue();

                    if (exception != null) {
                        final String errorMessage = format("Failed to fetch existing states with keys: '%s'.",
                            keys);
                        handleError(errorMessage, exception, keys.size());
                        return completedFuture(null);
                    } else {
                        return syncBatch(fetchedStates, validStateDrafts);
                    }
                })
                .thenApply(ignored -> {
                    statistics.incrementProcessed(batch.size());
                    return statistics;
                });
        }
    }

    /**
     * Checks if a draft is valid for further processing. If so, then returns {@code true}. Otherwise handles an error
     * and returns {@code false}. A valid draft is a {@link StateDraft} object that is not {@code null} and its
     * key is not empty.
     *
     * @param draft nullable draft
     * @return boolean that indicate if given {@code draft} is valid for sync
     */
    private boolean validateDraft(@Nullable final StateDraft draft) {
        if (draft == null) {
            handleError("Failed to process null state draft.", null, 1);
        } else if (isBlank(draft.getKey())) {
            handleError("Failed to process state draft without key.", null, 1);
        } else {
            return true;
        }

        return false;
    }

    /**
     * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this method calls the
     * optional error callback specified in the {@code syncOptions} and updates the {@code statistics} instance by
     * incrementing the total number of failed states to sync.
     *
     * @param errorMessage The error message describing the reason(s) of failure.
     * @param exception    The exception that called caused the failure, if any.
     * @param failedTimes  The number of times that the failed states counter is incremented.
     */
    private void handleError(@Nonnull final String errorMessage, @Nullable final Throwable exception,
                             final int failedTimes) {
        syncOptions.applyErrorCallback(errorMessage, exception);
        statistics.incrementFailed(failedTimes);
    }

    /**
     * Given a set of state drafts, attempts to sync the drafts with the existing states in the CTP
     * project. The state and the draft are considered to match if they have the same key. When there will be no error
     * it will attempt to sync the drafts transitions. {@code newStates} transitions have to be expanded.
     *
     * @param oldStates old states.
     * @param newStates drafts that need to be synced.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncBatch(@Nonnull final Set<State> oldStates,
                                            @Nonnull final Set<StateDraft> newStates) {
        final Map<String, State> oldStateMap = oldStates.stream().collect(toMap(State::getKey, identity()));

        return allOf(newStates
            .stream()
            .map(newState -> {
                final State oldState = oldStateMap.get(newState.getKey());
                return ofNullable(oldState)
                    .map(state -> buildActionsAndUpdate(oldState, newState))
                    .orElseGet(() -> applyCallbackAndCreate(newState));
            })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new))
            .thenCompose(v -> {
                if (statistics.getFailed().intValue() != 0) {
                    return completedFuture(null);
                }

                return syncTransitions(newStates);
            });
    }

    /**
     * Given a set of state drafts, attempts to sync the drafts' transitions with the existing states in the CTP
     * project. The state and the draft are considered to match if they have the same key.
     *
     * @param newStates newStates drafts containing transitions that need to be synced.
     * @return @return a {@link CompletionStage} which contains an empty result after execution of the update
     */
    @Nonnull
    private CompletionStage<Void> syncTransitions(@Nonnull final Set<StateDraft> newStates) {
        Set<String> transitionKeys = newStates.stream()
            .map(draft -> {
                Set<String> local = new HashSet<>();
                local.add(draft.getKey());
                if (draft.getTransitions() != null) {
                    draft.getTransitions().stream()
                        .filter(ref -> ref.getObj() != null)
                        .forEach(ref -> local.add(ref.getObj().getKey()));
                }
                return local;
            })
            .flatMap(Set::stream)
            .collect(toSet());

        return stateService
            .fetchMatchingStatesByKeysWithTransitions(transitionKeys)
            .thenCompose(response -> {
                Map<String, State> keyToState = response.stream().collect(toMap(State::getKey, state -> state));
                Map<String, String> keyToIdMap = response.stream().collect(toMap(State::getKey, State::getId));

                return allOf(newStates
                    .stream()
                    .map(newState -> {
                        State oldState = keyToState.get(newState.getKey());
                        return ofNullable(oldState)
                            .map(state -> buildTransitionsActionsAndUpdate(oldState, newState, keyToIdMap))
                            .orElseGet(() -> {
                                statistics.incrementFailed();
                                return completedFuture(null);
                            });
                    })
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new));
            });
    }

    /**
     * Given a state draft, this method applies the beforeCreateCallback and then issues a create request to the
     * CTP project to create the corresponding State.
     *
     * @param stateDraft the state draft to create the state from.
     * @return a {@link CompletionStage} which contains an empty result after execution of the create.
     */
    @Nonnull
    private CompletionStage<Optional<State>> applyCallbackAndCreate(@Nonnull final StateDraft stateDraft) {
        // we can't create state with transitions - they might be not there yet
        final StateDraft createStateDraft = StateDraftBuilder.of(stateDraft.getKey(), stateDraft.getType())
            .description(stateDraft.getDescription())
            .initial(stateDraft.isInitial())
            .name(stateDraft.getName())
            .roles(stateDraft.getRoles())
            .build();

        return syncOptions
            .applyBeforeCreateCallBack(createStateDraft)
            .map(draft -> stateService
                .createState(draft)
                .thenApply(stateOptional -> {
                    if (stateOptional.isPresent()) {
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                    return stateOptional;
                })
            )
            .orElse(completedFuture(Optional.empty()));
    }

    @Nonnull
    private CompletionStage<Optional<State>> buildActionsAndUpdate(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {
        return buildActionsAndUpdate(oldState, newState, buildActions(oldState, newState));
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Optional<State>> buildActionsAndUpdate(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState,
        @Nonnull final List<UpdateAction<State>> updateActions) {
        List<UpdateAction<State>> updateActionsAfterCallback =
            syncOptions.applyBeforeUpdateCallBack(updateActions, newState, oldState);

        if (!updateActionsAfterCallback.isEmpty()) {
            return updateState(oldState, newState, updateActionsAfterCallback);
        }

        return completedFuture(null);
    }

    @Nonnull
    private CompletionStage<Optional<State>> buildTransitionsActionsAndUpdate(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState,
        @Nonnull final Map<String, String> keyToId) {
        Optional<UpdateAction<State>> action = buildSetTransitionsAction(oldState, newState, keyToId,
            errorMessage -> handleError(errorMessage, null, 1));
        return buildActionsAndUpdate(oldState, newState,
            action.map(Collections::singletonList).orElse(Collections.emptyList()));
    }

    /**
     * Given an existing {@link State} and a new {@link StateDraft}, the method calculates all the
     * update actions required to synchronize the existing state to be the same as the new one. If there are
     * update actions found, a request is made to CTP to update the existing state, otherwise it doesn't issue a
     * request.
     *
     * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was carried
     * out successfully or not. If an exception was thrown on executing the request to CTP, the error handling method
     * is called.
     *
     * @param oldState existing state that could be updated.
     * @param newState draft containing data that could differ from data in {@code oldState}.
     * @return a {@link CompletionStage} which contains an empty result after execution of the update.
     */
    @Nonnull
    private CompletionStage<Optional<State>> updateState(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState,
        @Nonnull final List<UpdateAction<State>> updateActions) {

        return stateService
            .updateState(oldState, updateActions)
            .handle(ImmutablePair::new)
            .thenCompose(updateResponse -> {
                final State updatedState = updateResponse.getKey();
                final Throwable sphereException = updateResponse.getValue();

                if (sphereException != null) {
                    return executeSupplierIfConcurrentModificationException(
                        sphereException,
                        () -> fetchAndUpdate(oldState, newState),
                        () -> {
                            final String errorMessage =
                                format(CTP_STATE_UPDATE_FAILED, newState.getKey(),
                                    sphereException.getMessage());
                            handleError(errorMessage, sphereException, 1);
                            return completedFuture(Optional.empty());
                        });
                } else {
                    statistics.incrementUpdated();
                    return completedFuture(Optional.of(updatedState));
                }
            });
    }

    @Nonnull
    private CompletionStage<Optional<State>> fetchAndUpdate(
        @Nonnull final State oldState,
        @Nonnull final StateDraft newState) {
        String key = oldState.getKey();
        return stateService
            .fetchState(key)
            .handle(ImmutablePair::new)
            .thenCompose(fetchResponse -> {
                Optional<State> fetchedStateOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_STATE_UPDATE_FAILED, key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(errorMessage, exception, 1);
                    return completedFuture(null);
                }

                return fetchedStateOptional
                    .map(fetchedState -> buildActionsAndUpdate(fetchedState, newState))
                    .orElseGet(() -> {
                        final String errorMessage = format(CTP_STATE_UPDATE_FAILED, key,
                            "Not found when attempting to fetch while retrying "
                                + "after concurrency modification.");
                        handleError(errorMessage, null, 1);
                        return completedFuture(null);
                    });
            });
    }

}
