package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import com.commercetools.sync.services.UnresolvedTransitionsService;
import com.commercetools.sync.services.impl.UnresolvedTransitionsServiceImpl;
import com.commercetools.sync.states.StateSyncOptions;
import com.commercetools.sync.states.StateSyncOptionsBuilder;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.queries.CustomObjectByKeyGet;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateDraftBuilder;
import io.sphere.sdk.states.StateType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.commercetools.sync.integration.commons.utils.CustomObjectITUtils.deleteWaitingToBeResolvedTransitionsCustomObjects;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static io.sphere.sdk.utils.SphereInternalUtils.asSet;
import static java.util.Collections.singleton;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;

class UnresolvedTransitionsServiceImplIT {

    private static final String CUSTOM_OBJECT_CONTAINER_KEY =
        "commercetools-sync-java.UnresolvedTransitionsService.stateDrafts";
    private UnresolvedTransitionsService unresolvedTransitionsService;
    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    @AfterEach
    void tearDown() {
        deleteWaitingToBeResolvedTransitionsCustomObjects(CTP_TARGET_CLIENT, CUSTOM_OBJECT_CONTAINER_KEY);
    }

    @BeforeEach
    void setupTest() {
        deleteWaitingToBeResolvedTransitionsCustomObjects(CTP_TARGET_CLIENT, CUSTOM_OBJECT_CONTAINER_KEY);

        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
        warningCallBackMessages = new ArrayList<>();

        final StateSyncOptions stateSyncOptions = StateSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorCallBackMessages.add(exception.getMessage());
                errorCallBackExceptions.add(exception.getCause());
            })
            .warningCallback((exception, newResource, oldResource) ->
                warningCallBackMessages.add(exception.getMessage()))
            .build();

        unresolvedTransitionsService = new UnresolvedTransitionsServiceImpl(stateSyncOptions);
    }

    @Test
    void saveFetchAndDelete_WithoutExceptions_shouldWorkCorrectly() {
        // preparation
        final Set<Reference<State>> newTransitions = new HashSet<>(
            Arrays.asList(State.referenceOfId("id1"), State.referenceOfId("id2")));

        final StateDraft stateDraft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE)
                                                  .transitions(newTransitions)
                                                  .build();

        final WaitingToBeResolvedTransitions stateDraftWithUnresolvedTransitions =
            new WaitingToBeResolvedTransitions(stateDraft, asSet("id1", "id2"));

        // test
        final Optional<WaitingToBeResolvedTransitions> result = unresolvedTransitionsService
            .save(stateDraftWithUnresolvedTransitions)
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(result).hasValueSatisfying(WaitingToBeResolvedTransitions ->
            assertThat(WaitingToBeResolvedTransitions.getStateDraft()).isEqualTo(stateDraft));

        // test
        final Set<WaitingToBeResolvedTransitions> waitingDrafts = unresolvedTransitionsService
            .fetch(singleton(stateDraft.getKey()))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(waitingDrafts).containsExactly(stateDraftWithUnresolvedTransitions);

        // test
        final Optional<WaitingToBeResolvedTransitions> deletionResult = unresolvedTransitionsService
            .delete(stateDraft.getKey())
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(deletionResult).hasValueSatisfying(WaitingToBeResolvedTransitions ->
            assertThat(WaitingToBeResolvedTransitions.getStateDraft()).isEqualTo(stateDraft));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();


    }

    @Test
    void saveFetchAndDelete_WithKeyWithSpecialCharacter_shouldWorkCorrectly() {
        // preparation
        final Set<Reference<State>> newTransitions = new HashSet<>(
            Arrays.asList(State.referenceOfId("id1"), State.referenceOfId("id2")));

        final String key = "You’re having a key with special character®";
        final StateDraft stateDraft = StateDraftBuilder.of(key, StateType.LINE_ITEM_STATE)
                                                       .transitions(newTransitions)
                                                       .build();

        final WaitingToBeResolvedTransitions stateDraftWithUnresolvedTransitions =
            new WaitingToBeResolvedTransitions(stateDraft, asSet("id1", "id2"));

        // test
        final Optional<WaitingToBeResolvedTransitions> result = unresolvedTransitionsService
            .save(stateDraftWithUnresolvedTransitions)
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(result).hasValueSatisfying(WaitingToBeResolvedTransitions ->
            assertThat(WaitingToBeResolvedTransitions.getStateDraft()).isEqualTo(stateDraft));

        // test
        final CustomObjectByKeyGet<WaitingToBeResolvedTransitions> customObjectByKeyGet = CustomObjectByKeyGet
            .of(CUSTOM_OBJECT_CONTAINER_KEY, sha1Hex(stateDraft.getKey()), WaitingToBeResolvedTransitions.class);
        final CustomObject<WaitingToBeResolvedTransitions> createdCustomObject = CTP_TARGET_CLIENT
            .execute(customObjectByKeyGet)
            .toCompletableFuture()
            .join();
        // assertions
        assertThat(createdCustomObject.getKey()).isEqualTo(sha1Hex(stateDraft.getKey()));

        // test
        final Set<WaitingToBeResolvedTransitions> waitingDrafts = unresolvedTransitionsService
            .fetch(singleton(stateDraft.getKey()))
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(waitingDrafts).containsExactly(stateDraftWithUnresolvedTransitions);

        // test
        final Optional<WaitingToBeResolvedTransitions> deletionResult = unresolvedTransitionsService
            .delete(stateDraft.getKey())
            .toCompletableFuture()
            .join();

        // assertions
        assertThat(deletionResult).hasValueSatisfying(WaitingToBeResolvedTransitions ->
            assertThat(WaitingToBeResolvedTransitions.getStateDraft()).isEqualTo(stateDraft));

        assertThat(errorCallBackMessages).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void save_ExistingStateDraftWithoutException_overwritesOldCustomObjectValue() {
        // preparation
        final Set<Reference<State>> newTransitions = new HashSet<>(
            Arrays.asList(State.referenceOfId("id1"), State.referenceOfId("id2")));

        final StateDraft stateDraft = StateDraftBuilder.of("key", StateType.LINE_ITEM_STATE)
                                                       .transitions(newTransitions)
                                                       .build();

        final WaitingToBeResolvedTransitions stateDraftWithUnresolvedTransitions =
            new WaitingToBeResolvedTransitions(stateDraft, asSet("id1", "id2"));

        unresolvedTransitionsService
            .save(stateDraftWithUnresolvedTransitions)
            .toCompletableFuture()
            .join();

        final WaitingToBeResolvedTransitions newStateDraftWithUnresolvedTransitions =
            new WaitingToBeResolvedTransitions(stateDraft, asSet("id1_123", "id2_123"));

        // test
        final Optional<WaitingToBeResolvedTransitions> latestResult = unresolvedTransitionsService
            .save(newStateDraftWithUnresolvedTransitions)
            .toCompletableFuture()
            .join();


        // assertions
        assertThat(latestResult).hasValueSatisfying(WaitingToBeResolvedTransitions -> {
            assertThat(WaitingToBeResolvedTransitions.getStateDraft())
                .isEqualTo(stateDraft);
            assertThat(WaitingToBeResolvedTransitions.getMissingTransitionStateKeys())
                .isEqualTo(newStateDraftWithUnresolvedTransitions.getMissingTransitionStateKeys());
        });

        final CustomObjectByKeyGet<WaitingToBeResolvedTransitions> customObjectByKeyGet = CustomObjectByKeyGet
            .of(CUSTOM_OBJECT_CONTAINER_KEY, sha1Hex(stateDraft.getKey()), WaitingToBeResolvedTransitions.class);
        final CustomObject<WaitingToBeResolvedTransitions> createdCustomObject = CTP_TARGET_CLIENT
            .execute(customObjectByKeyGet)
            .toCompletableFuture()
            .join();

        assertThat(createdCustomObject.getValue()).isEqualTo(newStateDraftWithUnresolvedTransitions);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(warningCallBackMessages).isEmpty();
    }
}
