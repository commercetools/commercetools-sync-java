package com.commercetools.sync.sdk2.states.utils;

import static com.commercetools.sync.sdk2.states.utils.StateUpdateActionUtils.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateAddRolesActionBuilder;
import com.commercetools.api.models.state.StateChangeInitialActionBuilder;
import com.commercetools.api.models.state.StateChangeTypeActionBuilder;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateDraftBuilder;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateReferenceBuilder;
import com.commercetools.api.models.state.StateRemoveRolesActionBuilder;
import com.commercetools.api.models.state.StateResourceIdentifier;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateRoleEnum;
import com.commercetools.api.models.state.StateSetDescriptionActionBuilder;
import com.commercetools.api.models.state.StateSetNameActionBuilder;
import com.commercetools.api.models.state.StateSetTransitionsAction;
import com.commercetools.api.models.state.StateSetTransitionsActionBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.api.models.state.StateUpdateAction;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateUpdateActionUtilsTest {

  private static final String KEY = "state-1";

  private State state;
  private StateDraft newSameStateDraft;
  private StateDraft newDifferentStateDraft;

  @BeforeEach
  void setup() {
    final StateTypeEnum type = StateTypeEnum.LINE_ITEM_STATE;
    final LocalizedString name = LocalizedString.of(Locale.GERMANY, "name");
    final LocalizedString description = LocalizedString.of(Locale.GERMANY, "description");
    final List<StateRoleEnum> roles = List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS);

    state = mock(State.class);
    when(state.getKey()).thenReturn(KEY);
    when(state.getType()).thenReturn(type);
    when(state.getName()).thenReturn(name);
    when(state.getDescription()).thenReturn(description);
    when(state.getInitial()).thenReturn(true);
    when(state.getRoles()).thenReturn(roles);

    newSameStateDraft =
        StateDraftBuilder.of()
            .key(KEY)
            .type(type)
            .name(name)
            .description(description)
            .roles(roles)
            .initial(true)
            .transitions(List.of())
            .build();

    newDifferentStateDraft =
        StateDraftBuilder.of()
            .key("key2")
            .type(StateTypeEnum.PRODUCT_STATE)
            .name(LocalizedString.of(Locale.GERMANY, "new name"))
            .description(LocalizedString.of(Locale.GERMANY, "new desc"))
            .roles(List.of(StateRoleEnum.RETURN))
            .initial(false)
            .build();
  }

  @Test
  void buildChangeTypeAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<StateUpdateAction> result = buildChangeTypeAction(state, newDifferentStateDraft);

    assertThat(result)
        .contains(StateChangeTypeActionBuilder.of().type(newDifferentStateDraft.getType()).build());
  }

  @Test
  void buildChangeTypeAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<StateUpdateAction> result = buildChangeTypeAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetNameAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<StateUpdateAction> result = buildSetNameAction(state, newDifferentStateDraft);

    assertThat(result)
        .contains(StateSetNameActionBuilder.of().name(newDifferentStateDraft.getName()).build());
  }

  @Test
  void buildSetNameAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<StateUpdateAction> result = buildSetNameAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetDescriptionAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<StateUpdateAction> result =
        buildSetDescriptionAction(state, newDifferentStateDraft);

    assertThat(result)
        .contains(
            StateSetDescriptionActionBuilder.of()
                .description(newDifferentStateDraft.getDescription())
                .build());
  }

  @Test
  void buildSetDescriptionAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<StateUpdateAction> result = buildSetDescriptionAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildChangeInitialAction_WithDifferentValues_ShouldReturnAction() {
    final Optional<StateUpdateAction> result =
        buildChangeInitialAction(state, newDifferentStateDraft);

    assertThat(result)
        .contains(
            StateChangeInitialActionBuilder.of()
                .initial(newDifferentStateDraft.getInitial())
                .build());
  }

  @Test
  void buildChangeInitialAction_WithSameValues_ShouldReturnEmptyOptional() {
    final Optional<StateUpdateAction> result = buildChangeInitialAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildRolesUpdateActions_WithSameValues_ShouldNotBuildAction() {
    final List<StateUpdateAction> result = buildRolesUpdateActions(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildRolesUpdateActions_WithDifferentValues_ShouldReturnActionWithRightOrder() {
    final List<StateUpdateAction> result = buildRolesUpdateActions(state, newDifferentStateDraft);

    assertThat(result)
        .containsExactly(
            StateRemoveRolesActionBuilder.of().roles(state.getRoles()).build(),
            StateAddRolesActionBuilder.of().roles(newDifferentStateDraft.getRoles()).build());
  }

  @Test
  void buildRolesUpdateActions_WithNewValues_ShouldReturnAction() {
    StateDraft newState =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles(List.of(StateRoleEnum.RETURN, StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final List<StateUpdateAction> result = buildRolesUpdateActions(state, newState);

    assertThat(result)
        .containsExactly(
            StateAddRolesActionBuilder.of().roles(List.of(StateRoleEnum.RETURN)).build());
  }

  @Test
  void buildRolesUpdateActions_WithRemovedRoles_ShouldReturnOnlyRemoveAction() {
    final List<StateRoleEnum> roles =
        List.of(StateRoleEnum.RETURN, StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS);

    State oldState = mock(State.class);
    when(oldState.getKey()).thenReturn(KEY);
    when(oldState.getRoles()).thenReturn(roles);

    StateDraft newState =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles(List.of(StateRoleEnum.REVIEW_INCLUDED_IN_STATISTICS))
            .build();

    final List<StateUpdateAction> result = buildRolesUpdateActions(oldState, newState);

    assertThat(result)
        .containsExactly(
            StateRemoveRolesActionBuilder.of().roles(List.of(StateRoleEnum.RETURN)).build());
  }

  @Test
  void buildRolesUpdateActions_WithNullRoles_ShouldNotReturnAction() {
    StateDraft newState =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.PRODUCT_STATE)
            .roles((List<StateRoleEnum>) null)
            .build();

    State oldState = mock(State.class);
    when(oldState.getKey()).thenReturn(KEY);
    when(oldState.getRoles()).thenReturn(null);

    final List<StateUpdateAction> result = buildRolesUpdateActions(oldState, newState);

    assertThat(result).isEmpty();
  }

  @Test
  void buildRolesUpdateActions_WithEmptyRoles_ShouldNotReturnAction() {
    StateDraft newState =
        StateDraftBuilder.of().key(KEY).type(StateTypeEnum.PRODUCT_STATE).roles(List.of()).build();

    State oldState = mock(State.class);
    when(oldState.getKey()).thenReturn(KEY);
    when(oldState.getRoles()).thenReturn(List.of());

    final List<StateUpdateAction> result = buildRolesUpdateActions(oldState, newState);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithEmptyValues_ShouldReturnEmptyOptional() {
    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, newSameStateDraft);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithNullValuesInList_ShouldReturnEmptyOptional() {
    final List<StateReference> oldTransitions = List.of();
    when(state.getTransitions()).thenReturn(oldTransitions);

    final StateDraft stateDraft =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions((StateResourceIdentifier) null)
            .build();

    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, stateDraft);
    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithEmptyNewTransitionsValues_ShouldReturnAction() {
    final List<StateReference> oldTransitions =
        List.of(StateReferenceBuilder.of().id("id").build());
    when(state.getTransitions()).thenReturn(oldTransitions);

    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, newSameStateDraft);

    assertThat(result)
        .contains(StateSetTransitionsActionBuilder.of().transitions(List.of()).build());
  }

  @Test
  void buildSetTransitionsAction_WithSameValues_ShouldReturnEmptyOptional() {
    final List<StateReference> oldTransitions =
        List.of(
            StateReferenceBuilder.of().id("state-key-1").build(),
            StateReferenceBuilder.of().id("state-key-2").build());
    final List<StateResourceIdentifier> newTransitions =
        List.of(
            StateResourceIdentifierBuilder.of().id("state-key-1").build(),
            StateResourceIdentifierBuilder.of().id("state-key-2").build());

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, newDifferent);

    assertThat(result).isEmpty();
  }

  @Test
  void buildSetTransitionsAction_WithAllDifferentValues_ShouldReturnAction() {
    final List<StateReference> oldTransitions =
        List.of(
            StateReferenceBuilder.of().id("old-state-1").build(),
            StateReferenceBuilder.of().id("old-state-2").build());

    final List<StateResourceIdentifier> newTransitions =
        List.of(
            StateResourceIdentifierBuilder.of().id("new-state-1").build(),
            StateResourceIdentifierBuilder.of().id("new-state-2").build());

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, newDifferent);
    assertThat(result.get()).isInstanceOf(StateSetTransitionsAction.class);
    assertThat(((StateSetTransitionsAction) result.get()).getTransitions())
        .contains(
            StateResourceIdentifierBuilder.of().id("new-state-1").build(),
            StateResourceIdentifierBuilder.of().id("new-state-2").build());
  }

  @Test
  void buildSetTransitionsAction_WithAnyDifferentValues_ShouldReturnAction() {
    final List<StateReference> oldTransitions =
        List.of(
            StateReferenceBuilder.of().id("old-state-1").build(),
            StateReferenceBuilder.of().id("old-state-2").build());

    final List<StateResourceIdentifier> newTransitions =
        List.of(
            StateResourceIdentifierBuilder.of().id("old-state-1").build(),
            StateResourceIdentifierBuilder.of().id("new-state-2").build());

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, newDifferent);
    assertThat(result.get()).isInstanceOf(StateSetTransitionsAction.class);
    assertThat(((StateSetTransitionsAction) result.get()).getTransitions())
        .contains(
            StateResourceIdentifierBuilder.of().id("old-state-1").build(),
            StateResourceIdentifierBuilder.of().id("new-state-2").build());
  }

  @Test
  void buildSetTransitionsAction_WitNullTransitions_ShouldReturnAction() {
    final List<StateReference> oldTransitions =
        List.of(
            StateReferenceBuilder.of().id("old-state-1").build(),
            StateReferenceBuilder.of().id("old-state-2").build());

    final List<StateResourceIdentifier> newTransitions =
        asList(null, StateResourceIdentifierBuilder.of().id("new-state-2").build());

    when(state.getTransitions()).thenReturn(oldTransitions);
    final StateDraft newDifferent =
        StateDraftBuilder.of()
            .key(KEY)
            .type(StateTypeEnum.LINE_ITEM_STATE)
            .transitions(newTransitions)
            .build();

    final Optional<StateUpdateAction> result = buildSetTransitionsAction(state, newDifferent);
    final List<StateResourceIdentifier> expectedTransitions =
        List.of(StateResourceIdentifierBuilder.of().id("new-state-2").build());
    assertThat(result)
        .contains(StateSetTransitionsActionBuilder.of().transitions(expectedTransitions).build());
  }
}
