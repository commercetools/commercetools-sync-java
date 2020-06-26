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
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StateUpdateActionUtilsTest {

    private static final String KEY = "state-1";

    private State state;
    private StateDraft newSameStateDraft;
    private StateDraft newDifferentStateDraft;

    @BeforeEach
    void setup() {
        final StateType type = StateType.LINE_ITEM_STATE;
        final LocalizedString name = LocalizedString.of(Locale.GERMANY, "name");
        final LocalizedString description = LocalizedString.of(Locale.GERMANY, "description");
        final Set<StateRole> roles = new HashSet<>(singletonList(StateRole.REVIEW_INCLUDED_IN_STATISTICS));

        state = mock(State.class);
        when(state.getKey()).thenReturn(KEY);
        when(state.getType()).thenReturn(type);
        when(state.getName()).thenReturn(name);
        when(state.getDescription()).thenReturn(description);
        when(state.isInitial()).thenReturn(true);
        when(state.getRoles()).thenReturn(roles);

        newSameStateDraft = StateDraft.of(KEY, type)
            .withName(name)
            .withDescription(description)
            .withRoles(roles)
            .withInitial(true);

        newDifferentStateDraft = StateDraft.of("key2", StateType.PRODUCT_STATE)
            .withName(LocalizedString.of(Locale.GERMANY, "new name"))
            .withDescription(LocalizedString.of(Locale.GERMANY, "new desc"))
            .withRoles(new HashSet<>(singletonList(StateRole.RETURN)))
            .withInitial(false);
    }

    @Test
    void buildChangeTypeAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<State>> result = buildChangeTypeAction(state, newDifferentStateDraft);

        assertThat(result).contains(ChangeType.of(newDifferentStateDraft.getType()));
    }

    @Test
    void buildChangeTypeAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<State>> result = buildChangeTypeAction(state, newSameStateDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetNameAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<State>> result = buildSetNameAction(state, newDifferentStateDraft);

        assertThat(result).contains(SetName.of(newDifferentStateDraft.getName()));
    }

    @Test
    void buildSetNameAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<State>> result = buildSetNameAction(state, newSameStateDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<State>> result = buildSetDescriptionAction(state, newDifferentStateDraft);

        assertThat(result).contains(SetDescription.of(newDifferentStateDraft.getDescription()));
    }

    @Test
    void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<State>> result = buildSetDescriptionAction(state, newSameStateDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildChangeInitialAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<State>> result = buildChangeInitialAction(state, newDifferentStateDraft);

        assertThat(result).contains(ChangeInitial.of(newDifferentStateDraft.isInitial()));
    }

    @Test
    void buildChangeInitialAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<State>> result = buildChangeInitialAction(state, newSameStateDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildRolesUpdateActions_WithSameValues_ShouldNotBuildAction() {
        final List<UpdateAction<State>> result = buildRolesUpdateActions(state, newSameStateDraft);

        assertThat(result).isEmpty();
    }

    @Test
    void buildRolesUpdateActions_WithDifferentValues_ShouldReturnActionWithRightOrder() {
        final List<UpdateAction<State>> result = buildRolesUpdateActions(state, newDifferentStateDraft);

        assertThat(result).containsExactly(
            RemoveRoles.of(state.getRoles()),
            AddRoles.of(newDifferentStateDraft.getRoles()));
    }

    @Test
    void buildRolesUpdateActions_WithNewValues_ShouldReturnAction() {
        StateDraft newState = StateDraft.of(KEY, StateType.PRODUCT_STATE)
            .withRoles(new HashSet<>(asList(StateRole.RETURN, StateRole.REVIEW_INCLUDED_IN_STATISTICS)));

        final List<UpdateAction<State>> result = buildRolesUpdateActions(state, newState);

        assertThat(result).containsExactly(AddRoles.of(new HashSet<>(singletonList(StateRole.RETURN))));
    }

    @Test
    void buildRolesUpdateActions_WithRemovedRoles_ShouldReturnOnlyRemoveAction() {
        final Set<StateRole> roles = new HashSet<>(asList(StateRole.RETURN, StateRole.REVIEW_INCLUDED_IN_STATISTICS));

        State oldState = mock(State.class);
        when(oldState.getKey()).thenReturn(KEY);
        when(oldState.getRoles()).thenReturn(roles);

        StateDraft newState = StateDraft.of(KEY, StateType.PRODUCT_STATE)
            .withRoles(new HashSet<>(singletonList(StateRole.REVIEW_INCLUDED_IN_STATISTICS)));

        final List<UpdateAction<State>> result = buildRolesUpdateActions(oldState, newState);

        assertThat(result).containsExactly(RemoveRoles.of(new HashSet<>(singletonList(StateRole.RETURN))));
    }

    @Test
    void buildSetTransitionsAction_WithEmptyValues_ShouldReturnEmptyOptional() {
        final Map<String, String> keyToId = new HashMap<>();
        final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newSameStateDraft, keyToId,
            msg -> {
            });

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetTransitionsAction_WithEmptyNewTransitionsValues_ShouldReturnAction() {
        final Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")));
        when(state.getTransitions()).thenReturn(oldTransitions);

        final Map<String, String> keyToId = new HashMap<>();
        final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newSameStateDraft, keyToId,
            msg -> {
            });

        assertThat(result).contains(SetTransitions.of(emptySet()));
    }

    @Test
    void buildSetTransitionsAction_WithUnknownTransition_ShouldReturnEmptyOptionalAndTriggerCallback() {
        final Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")));
        final Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("new-id")));

        when(state.getTransitions()).thenReturn(oldTransitions);
        final StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

        final AtomicReference<String> callback = new AtomicReference<>(null);

        final Map<String, String> keyToId = new HashMap<>();
        final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent, keyToId,
            callback::set);

        assertAll(
            () -> assertThat(result).isEmpty(),
            () -> assertThat(callback).hasValue(format("Failed to build transition action for state '%s' because "
                + "not all of states id were available", newDifferent.getKey()))
        );
    }

    @Test
    void buildSetTransitionsAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")
            .filled(state)));
        final Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id")
            .filled(state)));

        when(state.getTransitions()).thenReturn(oldTransitions);
        final StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

        final AtomicReference<String> callback = new AtomicReference<>(null);

        final Map<String, String> keyToId = new HashMap<>();
        keyToId.put(KEY, "id");

        final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent, keyToId,
            callback::set);

        assertAll(
            () -> assertThat(result).isEmpty(),
            () -> assertThat(callback.get()).isNull()
        );
    }

    @Test
    void buildSetTransitionsAction_WithDifferentValues_ShouldReturnAction() {
        final Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(State.referenceOfId("id")));
        final Set<Reference<State>> newTransitions = new HashSet<>(singletonList(State.referenceOfId("id")
            .filled(state)));

        when(state.getTransitions()).thenReturn(oldTransitions);
        final StateDraft newDifferent = StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

        final AtomicReference<String> callback = new AtomicReference<>(null);

        final Map<String, String> keyToId = new HashMap<>();
        keyToId.put(KEY, "id");

        final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent, keyToId,
            callback::set);

        assertAll(
            () -> assertThat(result).contains(SetTransitions.of(new HashSet<>(singletonList(
                State.referenceOfId("id"))))),
            () -> assertThat(callback.get()).isNull()
        );
    }

}
