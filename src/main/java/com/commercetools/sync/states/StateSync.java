package com.commercetools.sync.states;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.states.utils.StateSyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.services.StateService;
import com.commercetools.sync.services.UnresolvedTransitionsService;
import com.commercetools.sync.services.impl.StateServiceImpl;
import com.commercetools.sync.services.impl.UnresolvedTransitionsServiceImpl;
import com.commercetools.sync.states.helpers.StateBatchValidator;
import com.commercetools.sync.states.helpers.StateReferenceResolver;
import com.commercetools.sync.states.helpers.StateSyncStatistics;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class StateSync extends BaseSync<StateDraft, StateSyncStatistics, StateSyncOptions> {

  private static final String CTP_STATE_FETCH_FAILED =
      "Failed to fetch existing states with keys: '%s'.";
  private static final String CTP_STATE_UPDATE_FAILED =
      "Failed to update state with key: '%s'. Reason: %s";

  private static final String FAILED_TO_PROCESS =
      "Failed to process the StateDraft with key: '%s'. Reason: %s";
  private static final String UNRESOLVED_TRANSITIONS_STORE_FETCH_FAILED =
      "Failed to fetch StateDrafts waiting to " + "be resolved with keys '%s'.";

  private final StateService stateService;
  private final StateReferenceResolver stateReferenceResolver;
  private final UnresolvedTransitionsService unresolvedTransitionsService;
  private final StateBatchValidator batchValidator;

  private ConcurrentHashMap.KeySetView<String, Boolean> readyToResolve;

  public StateSync(@Nonnull final StateSyncOptions stateSyncOptions) {
    this(stateSyncOptions, new StateServiceImpl(stateSyncOptions));
  }

  /**
   * Takes a {@link StateSyncOptions} and a {@link StateSync} instances to instantiate a new {@link
   * StateSync} instance that could be used to sync state drafts in the CTP project specified in the
   * injected {@link StateSyncOptions} instance.
   *
   * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and
   * passed to.
   *
   * @param stateSyncOptions the container of all the options of the sync process including the CTP
   *     project client and/or configuration and other sync-specific options.
   * @param stateService the type service which is responsible for fetching/caching the Types from
   *     the CTP project.
   */
  StateSync(
      @Nonnull final StateSyncOptions stateSyncOptions, @Nonnull final StateService stateService) {
    super(new StateSyncStatistics(), stateSyncOptions);
    this.stateService = stateService;
    this.stateReferenceResolver = new StateReferenceResolver(getSyncOptions(), stateService);
    this.unresolvedTransitionsService = new UnresolvedTransitionsServiceImpl(getSyncOptions());
    this.batchValidator = new StateBatchValidator(getSyncOptions(), getStatistics());
  }

  @Override
  protected CompletionStage<StateSyncStatistics> process(
      @Nonnull final List<StateDraft> resourceDrafts) {
    List<List<StateDraft>> batches = batchElements(resourceDrafts, syncOptions.getBatchSize());
    return syncBatches(batches, completedFuture(statistics));
  }

  @Override
  protected CompletionStage<StateSyncStatistics> processBatch(
      @Nonnull final List<StateDraft> batch) {
    readyToResolve = ConcurrentHashMap.newKeySet();

    final ImmutablePair<Set<StateDraft>, Set<String>> result =
        batchValidator.validateAndCollectReferencedKeys(batch);

    final Set<StateDraft> validDrafts = result.getLeft();
    if (validDrafts.isEmpty()) {
      statistics.incrementProcessed(batch.size());
      return CompletableFuture.completedFuture(statistics);
    }

    final Set<String> stateTransitionKeys = result.getRight();

    return stateService
        .cacheKeysToIds(stateTransitionKeys)
        .handle(ImmutablePair::new)
        .thenCompose(
            cachingResponse -> {
              final Throwable cachingException = cachingResponse.getValue();
              if (cachingException != null) {
                handleError(
                    new SyncException("Failed to build a cache of keys to ids.", cachingException),
                    null,
                    null,
                    null,
                    validDrafts.size());
                return CompletableFuture.completedFuture(null);
              }

              final Map<String, String> keyToIdCache = cachingResponse.getKey();
              return syncBatch(validDrafts, keyToIdCache);
            })
        .thenApply(
            ignored -> {
              statistics.incrementProcessed(batch.size());
              return statistics;
            });
  }

  /**
   * Given a {@link String} {@code errorMessage} and a {@link Throwable} {@code exception}, this
   * method calls the optional error callback specified in the {@code syncOptions} and updates the
   * {@code statistics} instance by incrementing the total number of failed states to sync.
   *
   * @param syncException The exception that caused the failure.
   * @param failedTimes The number of times that the failed states counter is incremented.
   */
  private void handleError(
      @Nonnull final SyncException syncException,
      @Nullable final State entry,
      @Nullable final StateDraft draft,
      @Nullable final List<UpdateAction<State>> updateActions,
      final int failedTimes) {
    syncOptions.applyErrorCallback(syncException, entry, draft, updateActions);
    ;
    statistics.incrementFailed(failedTimes);
  }

  @Nonnull
  private CompletionStage<Void> syncBatch(
      @Nonnull final Set<StateDraft> stateDrafts, @Nonnull final Map<String, String> keyToIdCache) {

    if (stateDrafts.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final Set<String> stateDraftKeys =
        stateDrafts.stream().map(StateDraft::getKey).collect(Collectors.toSet());

    return stateService
        .fetchMatchingStatesByKeysWithTransitions(stateDraftKeys)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Throwable fetchException = fetchResponse.getValue();
              if (fetchException != null) {
                final String errorMessage = format(CTP_STATE_FETCH_FAILED, stateDraftKeys);
                handleError(
                    new SyncException(errorMessage, fetchException),
                    null,
                    null,
                    null,
                    stateDraftKeys.size());
                return CompletableFuture.completedFuture(null);
              } else {
                final Set<State> matchingStates = fetchResponse.getKey();
                return syncOrKeepTrack(stateDrafts, matchingStates, keyToIdCache)
                    .thenCompose(aVoid -> resolveNowReadyReferences(keyToIdCache));
              }
            });
  }

  /**
   * Given a set of state drafts, for each new draft: if it doesn't have any state references which
   * are missing, it syncs the new draft. However, if it does have missing references, it keeps
   * track of it by persisting it.
   *
   * @param newStates drafts that need to be synced.
   * @param oldStates old states.
   * @param keyToIdCache the cache containing the mapping of all existing state keys to ids.
   * @return a {@link CompletionStage} which contains an empty result after execution of the update
   */
  @Nonnull
  private CompletionStage<Void> syncOrKeepTrack(
      @Nonnull final Set<StateDraft> newStates,
      @Nonnull final Set<State> oldStates,
      @Nonnull final Map<String, String> keyToIdCache) {

    return allOf(
        newStates.stream()
            .map(
                newDraft -> {
                  final Set<String> missingTransitionStateKeys =
                      getMissingTransitionStateKeys(newDraft, keyToIdCache);

                  if (!missingTransitionStateKeys.isEmpty()) {
                    return keepTrackOfMissingTransitionStates(newDraft, missingTransitionStateKeys);
                  } else {
                    return syncDraft(oldStates, newDraft);
                  }
                })
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  private Set<String> getMissingTransitionStateKeys(
      @Nonnull final StateDraft newState, @Nonnull final Map<String, String> keyToIdCache) {

    if (newState.getTransitions() == null || newState.getTransitions().isEmpty()) {
      return Collections.emptySet();
    }

    return newState.getTransitions().stream()
        .map(Reference::getId)
        .filter(key -> !keyToIdCache.containsKey(key))
        .collect(Collectors.toSet());
  }

  private CompletionStage<Optional<WaitingToBeResolvedTransitions>>
      keepTrackOfMissingTransitionStates(
          @Nonnull final StateDraft newState,
          @Nonnull final Set<String> missingTransitionParentStateKeys) {

    missingTransitionParentStateKeys.forEach(
        missingParentKey -> statistics.addMissingDependency(missingParentKey, newState.getKey()));

    return unresolvedTransitionsService.save(
        new WaitingToBeResolvedTransitions(newState, missingTransitionParentStateKeys));
  }

  @Nonnull
  private CompletionStage<Void> syncDraft(
      @Nonnull final Set<State> oldStates, @Nonnull final StateDraft newStateDraft) {

    final Map<String, State> oldStateMap =
        oldStates.stream().collect(toMap(State::getKey, identity()));

    return stateReferenceResolver
        .resolveReferences(newStateDraft)
        .thenCompose(
            resolvedDraft -> {
              final State oldState = oldStateMap.get(newStateDraft.getKey());

              return ofNullable(oldState)
                  .map(state -> buildActionsAndUpdate(oldState, resolvedDraft))
                  .orElseGet(() -> applyCallbackAndCreate(resolvedDraft));
            })
        .exceptionally(
            completionException -> {
              final String errorMessage =
                  format(
                      FAILED_TO_PROCESS, newStateDraft.getKey(), completionException.getMessage());
              handleError(
                  new SyncException(errorMessage, completionException),
                  null,
                  newStateDraft,
                  null,
                  1);
              return null;
            });
  }

  /**
   * Given a state draft, this method applies the beforeCreateCallback and then issues a create
   * request to the CTP project to create the corresponding State.
   *
   * @param stateDraft the state draft to create the state from.
   * @return a {@link CompletionStage} which contains an empty result after execution of the create.
   */
  @Nonnull
  private CompletionStage<Void> applyCallbackAndCreate(@Nonnull final StateDraft stateDraft) {
    return syncOptions
        .applyBeforeCreateCallback(stateDraft)
        .map(
            draft ->
                stateService
                    .createState(draft)
                    .thenAccept(
                        stateOptional -> {
                          if (stateOptional.isPresent()) {
                            readyToResolve.add(stateDraft.getKey());
                            statistics.incrementCreated();
                          } else {
                            statistics.incrementFailed();
                          }
                        }))
        .orElse(completedFuture(null));
  }

  @Nonnull
  private CompletionStage<Void> buildActionsAndUpdate(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {

    final List<UpdateAction<State>> updateActions = buildActions(oldState, newState);

    List<UpdateAction<State>> updateActionsAfterCallback =
        syncOptions.applyBeforeUpdateCallback(updateActions, newState, oldState);

    if (!updateActionsAfterCallback.isEmpty()) {
      return updateState(oldState, newState, updateActionsAfterCallback);
    }

    return completedFuture(null);
  }

  /**
   * Given an existing {@link State} and a new {@link StateDraft}, the method calculates all the
   * update actions required to synchronize the existing state to be the same as the new one. If
   * there are update actions found, a request is made to CTP to update the existing state,
   * otherwise it doesn't issue a request.
   *
   * <p>The {@code statistics} instance is updated accordingly to whether the CTP request was
   * carried out successfully or not. If an exception was thrown on executing the request to CTP,
   * the error handling method is called.
   *
   * @param oldState existing state that could be updated.
   * @param newState draft containing data that could differ from data in {@code oldState}.
   * @return a {@link CompletionStage} which contains an empty result after execution of the update.
   */
  @Nonnull
  private CompletionStage<Void> updateState(
      @Nonnull final State oldState,
      @Nonnull final StateDraft newState,
      @Nonnull final List<UpdateAction<State>> updateActions) {

    return stateService
        .updateState(oldState, updateActions)
        .handle(ImmutablePair::new)
        .thenCompose(
            updateResponse -> {
              final Throwable sphereException = updateResponse.getValue();

              if (sphereException != null) {
                return executeSupplierIfConcurrentModificationException(
                    sphereException,
                    () -> fetchAndUpdate(oldState, newState),
                    () -> {
                      final String errorMessage =
                          format(
                              CTP_STATE_UPDATE_FAILED,
                              newState.getKey(),
                              sphereException.getMessage());
                      handleError(
                          new SyncException(errorMessage, sphereException),
                          oldState,
                          newState,
                          updateActions,
                          1);
                      return completedFuture(null);
                    });
              } else {
                statistics.incrementUpdated();
                return completedFuture(null);
              }
            });
  }

  @Nonnull
  private CompletionStage<Void> fetchAndUpdate(
      @Nonnull final State oldState, @Nonnull final StateDraft newState) {
    String key = oldState.getKey();
    return stateService
        .fetchState(key)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              Optional<State> fetchedStateOptional = fetchResponse.getKey();
              final Throwable exception = fetchResponse.getValue();

              if (exception != null) {
                final String errorMessage =
                    format(
                        CTP_STATE_UPDATE_FAILED,
                        key,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                handleError(
                    new SyncException(errorMessage, exception), oldState, newState, null, 1);
                return completedFuture(null);
              }

              return fetchedStateOptional
                  .map(fetchedState -> buildActionsAndUpdate(fetchedState, newState))
                  .orElseGet(
                      () -> {
                        final String errorMessage =
                            format(
                                CTP_STATE_UPDATE_FAILED,
                                key,
                                "Not found when attempting to fetch while retrying "
                                    + "after concurrency modification.");
                        handleError(new SyncException(errorMessage), oldState, newState, null, 1);
                        return completedFuture(null);
                      });
            });
  }

  @Nonnull
  private CompletionStage<Void> resolveNowReadyReferences(
      @Nonnull final Map<String, String> keyToIdCache) {

    // We delete anyways the keys from the statistics before we attempt resolution, because even if
    // resolution fails
    // the states that failed to be synced would be counted as failed.

    final Set<String> referencingDraftKeys =
        readyToResolve.stream()
            .map(statistics::removeAndGetReferencingKeys)
            .filter(Objects::nonNull)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (referencingDraftKeys.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    final Set<StateDraft> readyToSync = new HashSet<>();
    final Set<WaitingToBeResolvedTransitions> waitingDraftsToBeUpdated = new HashSet<>();

    return unresolvedTransitionsService
        .fetch(referencingDraftKeys)
        .handle(ImmutablePair::new)
        .thenCompose(
            fetchResponse -> {
              final Set<WaitingToBeResolvedTransitions> waitingDrafts = fetchResponse.getKey();
              final Throwable fetchException = fetchResponse.getValue();

              if (fetchException != null) {
                final String errorMessage =
                    format(UNRESOLVED_TRANSITIONS_STORE_FETCH_FAILED, referencingDraftKeys);
                handleError(
                    new SyncException(errorMessage, fetchException),
                    null,
                    null,
                    null,
                    referencingDraftKeys.size());
                return CompletableFuture.completedFuture(null);
              }

              waitingDrafts.forEach(
                  waitingDraft -> {
                    final Set<String> missingTransitionStateKeys =
                        waitingDraft.getMissingTransitionStateKeys();
                    missingTransitionStateKeys.removeAll(readyToResolve);

                    if (missingTransitionStateKeys.isEmpty()) {
                      readyToSync.add(waitingDraft.getStateDraft());
                    } else {
                      waitingDraftsToBeUpdated.add(waitingDraft);
                    }
                  });

              return updateWaitingDrafts(waitingDraftsToBeUpdated)
                  .thenCompose(aVoid -> syncBatch(readyToSync, keyToIdCache))
                  .thenCompose(aVoid -> removeFromWaiting(readyToSync));
            });
  }

  @Nonnull
  private CompletableFuture<Void> updateWaitingDrafts(
      @Nonnull final Set<WaitingToBeResolvedTransitions> waitingDraftsToBeUpdated) {

    return allOf(
        waitingDraftsToBeUpdated.stream()
            .map(unresolvedTransitionsService::save)
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }

  @Nonnull
  private CompletableFuture<Void> removeFromWaiting(@Nonnull final Set<StateDraft> drafts) {
    return allOf(
        drafts.stream()
            .map(StateDraft::getKey)
            .map(unresolvedTransitionsService::delete)
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
  }
}
