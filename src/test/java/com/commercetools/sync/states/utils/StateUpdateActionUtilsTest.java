package com.commercetools.sync.states.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.StateRole;
import io.sphere.sdk.states.StateType;
import io.sphere.sdk.states.commands.updateactions.AddRoles;
import io.sphere.sdk.states.commands.updateactions.ChangeInitial;
import io.sphere.sdk.states.commands.updateactions.ChangeType;
import io.sphere.sdk.states.commands.updateactions.RemoveRoles;
import io.sphere.sdk.states.commands.updateactions.SetDescription;
import io.sphere.sdk.states.commands.updateactions.SetName;
import io.sphere.sdk.states.commands.updateactions.SetTransitions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildChangeInitialAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildChangeTypeAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildRolesUpdateActions;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetDescriptionAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetNameAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetTransitionsAction;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StateUpdateActionUtilsTest {

    private static String KEY = "key1";

    private State old;
    private StateDraft newSame;
    private StateDraft newDifferent;

    @BeforeEach
    void setup() {
        final StateType type = StateType.LINE_ITEM_STATE;
        final LocalizedString name = LocalizedString.of(Locale.GERMANY, "name");
        final LocalizedString description = LocalizedString.of(Locale.GERMANY, "description");

        old = mock(State.class);
        when(old.getKey()).thenReturn(KEY);
        when(old.getType()).thenReturn(type);
        when(old.getName()).thenReturn(name);
        when(old.getDescription()).thenReturn(description);
        when(old.isInitial()).thenReturn(true);

        newSame = StateDraft.of(KEY, type)
            .withName(name)
            .withDescription(description)
            .withInitial(true);

        newDifferent = StateDraft.of("key2", StateType.PRODUCT_STATE)
            .withName(LocalizedString.of(Locale.GERMANY, "whatever"))
            .withDescription(LocalizedString.of(Locale.GERMANY, "changed"))
            .withInitial(false);
    }

    @Test
    void buildChangeTypeAction_WithDifferentValues_ShouldReturnAction() {
        Optional<UpdateAction<State>> result = buildChangeTypeAction(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should contain action of `ChangeType`")
                .containsInstanceOf(ChangeType.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(ChangeType.of(newDifferent.getType()))
        );
    }

    @Test
    void buildChangeTypeAction_WithSameValues_ShouldReturnEmptyOptional() {
        Optional<UpdateAction<State>> result = buildChangeTypeAction(old, newSame);

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildSetNameAction_WithDifferentValues_ShouldReturnAction() {
        Optional<UpdateAction<State>> result = buildSetNameAction(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should contain action of `SetName`")
                .containsInstanceOf(SetName.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(SetName.of(newDifferent.getName()))
        );
    }

    @Test
    void buildSetNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        Optional<UpdateAction<State>> result = buildSetNameAction(old, newSame);

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        Optional<UpdateAction<State>> result = buildSetDescriptionAction(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should contain action of `SetDescription`")
                .containsInstanceOf(SetDescription.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(SetDescription.of(newDifferent.getDescription()))
        );
    }

    @Test
    void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        Optional<UpdateAction<State>> result = buildSetDescriptionAction(old, newSame);

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildChangeInitialAction_WithDifferentValues_ShouldReturnAction() {
        Optional<UpdateAction<State>> result = buildChangeInitialAction(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should contain action of `ChangeInitial`")
                .containsInstanceOf(ChangeInitial.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(ChangeInitial.of(newDifferent.isInitial()))
        );
    }

    @Test
    void buildChangeInitialAction_WithSameValues_ShouldReturnEmptyOptional() {
        Optional<UpdateAction<State>> result = buildChangeInitialAction(old, newSame);

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildRolesUpdateActions_WithSameValues_ShouldNotBuildAction() {
        List<UpdateAction<State>> result = buildRolesUpdateActions(old, newSame);

        assertThat(result).as("There should be no actions created").isEmpty();
    }

    @Test
    void buildRolesUpdateActions_WithDifferentValues_ShouldReturnAction() {
        Set<StateRole> oldRoles = new HashSet<>(singletonList(StateRole.RETURN));
        Set<StateRole> newRoles = new HashSet<>(singletonList(StateRole.REVIEW_INCLUDED_IN_STATISTICS));

        when(old.getRoles()).thenReturn(oldRoles);
        StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withRoles(newRoles);

        List<UpdateAction<State>> result = buildRolesUpdateActions(old, newDifferent);

        assertAll(
            () -> assertThat(result).as("Should add new role")
                .contains(AddRoles.of(newRoles)),
            () -> assertThat(result).as("Should remove old role")
                .contains(RemoveRoles.of(oldRoles))
        );
    }

    @Test
    void buildSetTransitionsAction_WithEmptyValues_ShouldReturnEmptyOptional() {
        Map<String, String> keyToId = new HashMap<>();
        Optional<UpdateAction<State>> result = buildSetTransitionsAction(old, newSame, keyToId, msg -> {
        });

        assertThat(result).as("There should be no action created").isEmpty();
    }

    @Test
    void buildSetTransitionsAction_WithEmptyNewTransitionsValues_ShouldReturnAction() {
        Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")));
        when(old.getTransitions()).thenReturn(oldTransitions);

        Map<String, String> keyToId = new HashMap<>();
        Optional<UpdateAction<State>> result = buildSetTransitionsAction(old, newSame, keyToId, msg -> {
        });

        assertAll(
            () -> assertThat(result).as("Should contain action of `SetTransitions`")
                .containsInstanceOf(SetTransitions.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(SetTransitions.of(emptySet()))
        );
    }

    @Test
    void buildSetTransitionsAction_WithUnknownTransition_ShouldReturnEmptyOptionalAndTriggerCallback() {
        Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")));
        Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("new-id")));

        when(old.getTransitions()).thenReturn(oldTransitions);
        StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

        AtomicReference<String> callback = new AtomicReference<>(null);

        Map<String, String> keyToId = new HashMap<>();
        Optional<UpdateAction<State>> result = buildSetTransitionsAction(old, newDifferent, keyToId, callback::set);

        assertAll(
            () -> assertThat(result).as("There should be no action created").isEmpty(),
            () -> assertThat(callback).as("Error callback should be called")
                .hasValue(format("Failed to build transition action for state '%s' because "
                    + "not all of states id were available", newDifferent.getKey()))
        );
    }

    @Test
    void buildSetTransitionsAction_WithSameValues_ShouldReturnEmptyOptional() {
        Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id").filled(old)));
        Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id").filled(old)));

        when(old.getTransitions()).thenReturn(oldTransitions);
        StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

        AtomicReference<String> callback = new AtomicReference<>(null);

        Map<String, String> keyToId = new HashMap<>();
        keyToId.put(KEY, "id");

        Optional<UpdateAction<State>> result = buildSetTransitionsAction(old, newDifferent, keyToId, callback::set);

        assertAll(
            () -> assertThat(result).as("There should be no action created").isEmpty(),
            () -> assertThat(callback.get()).as("Error callback should not be called").isNull()
        );
    }

    @Test
    void buildSetTransitionsAction_WithDifferentValues_ShouldReturnAction() {
        Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")));
        Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id").filled(old)));

        when(old.getTransitions()).thenReturn(oldTransitions);
        StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

        AtomicReference<String> callback = new AtomicReference<>(null);

        Map<String, String> keyToId = new HashMap<>();
        keyToId.put(KEY, "id");

        Optional<UpdateAction<State>> result = buildSetTransitionsAction(old, newDifferent, keyToId, callback::set);

        assertAll(
            () -> assertThat(result).as("Should contain action of `SetTransitions`")
                .containsInstanceOf(SetTransitions.class),
            () -> assertThat(result).as("Should change to proper value")
                .contains(SetTransitions.of(new HashSet<>(singletonList(State.referenceOfId("id"))))),
            () -> assertThat(callback.get()).as("Error callback should not be called").isNull()
        );
    }

}
