package com.commercetools.sync.states.utils;

import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildChangeInitialAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildChangeTypeAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildRolesUpdateActions;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetDescriptionAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetNameAction;
import static com.commercetools.sync.states.utils.StateUpdateActionUtils.buildSetTransitionsAction;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    final Set<StateRole> roles =
        new HashSet<>(singletonList(StateRole.REVIEW_INCLUDED_IN_STATISTICS));

    state = mock(State.class);
    when(state.getKey()).thenReturn(KEY);
    when(state.getType()).thenReturn(type);
    when(state.getName()).thenReturn(name);
    when(state.getDescription()).thenReturn(description);
    when(state.isInitial()).thenReturn(true);
    when(state.getRoles()).thenReturn(roles);

    newSameStateDraft =
        StateDraft.of(KEY, type)
            .withName(name)
            .withDescription(description)
            .withRoles(roles)
            .withInitial(true);

    newDifferentStateDraft =
        StateDraft.of("key2", StateType.PRODUCT_STATE)
            .withName(LocalizedString.of(Locale.GERMANY, "new name"))
            .withDescription(LocalizedString.of(Locale.GERMANY, "new desc"))
            .withRoles(new HashSet<>(singletonList(StateRole.RETURN)))
            .withInitial(false);
  }

  @Test
  void buildChangeTypeAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<State>> result =
        buildChangeTypeAction(state, newDifferentStateDraft);

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
    final Optional<UpdateAction<State>> result =
        buildSetDescriptionAction(state, newDifferentStateDraft);

    assertThat(result).contains(SetDescription.of(newDifferentStateDraft.getDescription()));
  }

  @Test
  void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<State>> result =
        buildSetDescriptionAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeInitialAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<UpdateAction<State>> result =
        buildChangeInitialAction(state, newDifferentStateDraft);

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

    assertThat(result)
        .containsExactly(
            RemoveRoles.of(state.getRoles()), AddRoles.of(newDifferentStateDraft.getRoles()));
  }

  @Test
  void buildRolesUpdateActions_WithNewValues_ShouldReturnAction() {
    StateDraft newState =
        StateDraft.of(KEY, StateType.PRODUCT_STATE)
            .withRoles(
                new HashSet<>(asList(StateRole.RETURN, StateRole.REVIEW_INCLUDED_IN_STATISTICS)));

    final List<UpdateAction<State>> result = buildRolesUpdateActions(state, newState);

    assertThat(result).containsExactly(AddRoles.of(new HashSet<>(singletonList(StateRole.RETURN))));
  }

  @Test
  void buildRolesUpdateActions_WithRemovedRoles_ShouldReturnOnlyRemoveAction() {
    final Set<StateRole> roles =
        new HashSet<>(asList(StateRole.RETURN, StateRole.REVIEW_INCLUDED_IN_STATISTICS));

    State oldState = mock(State.class);
    when(oldState.getKey()).thenReturn(KEY);
    when(oldState.getRoles()).thenReturn(roles);

    StateDraft newState =
        StateDraft.of(KEY, StateType.PRODUCT_STATE)
            .withRoles(new HashSet<>(singletonList(StateRole.REVIEW_INCLUDED_IN_STATISTICS)));

    final List<UpdateAction<State>> result = buildRolesUpdateActions(oldState, newState);

    assertThat(result)
        .containsExactly(RemoveRoles.of(new HashSet<>(singletonList(StateRole.RETURN))));
  }

  @Test
  void buildRolesUpdateActions_WithNullRoles_ShouldNotReturnAction() {
    StateDraft newState =
        StateDraft.of(KEY, StateType.PRODUCT_STATE).withRoles((Set<StateRole>) null);

    State oldState = mock(State.class);
    when(oldState.getKey()).thenReturn(KEY);
    when(oldState.getRoles()).thenReturn(null);

    final List<UpdateAction<State>> result = buildRolesUpdateActions(oldState, newState);

    assertThat(result).isEmpty();
  }

  @Test
  void buildRolesUpdateActions_WithEmptyRoles_ShouldNotReturnAction() {
    StateDraft newState =
        StateDraft.of(KEY, StateType.PRODUCT_STATE).withRoles(Collections.emptySet());

    State oldState = mock(State.class);
    when(oldState.getKey()).thenReturn(KEY);
    when(oldState.getRoles()).thenReturn(Collections.emptySet());

    final List<UpdateAction<State>> result = buildRolesUpdateActions(oldState, newState);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithEmptyValues_ShouldReturnEmptyOptional() {
    final Optional<UpdateAction<State>> result =
        buildSetTransitionsAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithNullValuesInList_ShouldReturnEmptyOptional() {
    final Set<Reference<State>> oldTransitions = new HashSet<>(singletonList(null));
    when(state.getTransitions()).thenReturn(oldTransitions);

    final StateDraft stateDraft =
        StateDraft.of(KEY, StateType.LINE_ITEM_STATE)
            .withTransitions(new HashSet<>(singletonList(null)));

    final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, stateDraft);
    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithEmptyNewTransitionsValues_ShouldReturnAction() {
    final Set<Reference<State>> oldTransitions =
        new HashSet<>(singletonList(State.referenceOfId("id")));
    when(state.getTransitions()).thenReturn(oldTransitions);

    final Optional<UpdateAction<State>> result =
        buildSetTransitionsAction(state, newSameStateDraft);

    assertThat(result).contains(SetTransitions.of(emptySet()));
  }

  @Test
  void buildSetTransitionsAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Set<Reference<State>> oldTransitions =
        new HashSet<>(
            asList(State.referenceOfId("state-key-1"), State.referenceOfId("state-key-2")));
    final Set<Reference<State>> newTransitions =
        new HashSet<>(
            asList(State.referenceOfId("state-key-1"), State.referenceOfId("state-key-2")));

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

    final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithAllDifferentValues_ShouldReturnAction() {
    final Set<Reference<State>> oldTransitions =
        new HashSet<>(
            asList(State.referenceOfId("old-state-1"), State.referenceOfId("old-state-2")));

    final Set<Reference<State>> newTransitions =
        new HashSet<>(
            asList(State.referenceOfId("new-state-1"), State.referenceOfId("new-state-2")));

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

    final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent);
    HashSet<Reference<State>> expectedTransitions =
        new HashSet<>(
            asList(State.referenceOfId("new-state-1"), State.referenceOfId("new-state-2")));
    assertThat(result).contains(SetTransitions.of(expectedTransitions));
  }

  @Test
  void buildSetTransitionsAction_WithAnyDifferentValues_ShouldReturnAction() {
    final Set<Reference<State>> oldTransitions =
        new HashSet<>(
            asList(State.referenceOfId("old-state-1"), State.referenceOfId("old-state-2")));

    final Set<Reference<State>> newTransitions =
        new HashSet<>(
            asList(State.referenceOfId("old-state-1"), State.referenceOfId("new-state-2")));

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

    final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent);
    HashSet<Reference<State>> expectedTransitions =
        new HashSet<>(
            asList(State.referenceOfId("old-state-1"), State.referenceOfId("new-state-2")));
    assertThat(result).contains(SetTransitions.of(expectedTransitions));
  }

  @Test
  void buildSetTransitionsAction_WitNullTransitions_ShouldReturnAction() {
    final Set<Reference<State>> oldTransitions =
        new HashSet<>(
            asList(State.referenceOfId("old-state-1"), State.referenceOfId("old-state-2")));

    final Set<Reference<State>> newTransitions =
        new HashSet<>(asList(null, State.referenceOfId("new-state-2")));

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraft.of(KEY, StateType.LINE_ITEM_STATE).withTransitions(newTransitions);

    final Optional<UpdateAction<State>> result = buildSetTransitionsAction(state, newDifferent);
    HashSet<Reference<State>> expectedTransitions =
        new HashSet<>(singletonList(State.referenceOfId("new-state-2")));
    assertThat(result).contains(SetTransitions.of(expectedTransitions));
  }
}
