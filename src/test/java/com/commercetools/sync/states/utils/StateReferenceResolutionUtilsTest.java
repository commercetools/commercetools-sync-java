package com.commercetools.sync.states.utils;

import static com.commercetools.sync.states.utils.StateReferenceResolutionUtils.buildStateQuery;
import static com.commercetools.sync.states.utils.StateReferenceResolutionUtils.mapToStateDrafts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.states.State;
import io.sphere.sdk.states.StateDraft;
import io.sphere.sdk.states.queries.StateQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class StateReferenceResolutionUtilsTest {

  @Test
  void mapToStateDrafts_WithAllExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
    // preparation
    final List<State> mockStates = new ArrayList<>();
    final String stateReferenceKey = "state-key";

    for (int i = 0; i < 3; i++) {
      final State mockState = mock(State.class);

      final State state = getStateMock(stateReferenceKey);
      final Reference<State> expandedStateReference =
          Reference.ofResourceTypeIdAndIdAndObj(State.referenceTypeId(), state.getId(), state);
      when(mockState.getTransitions()).thenReturn(Collections.singleton(expandedStateReference));

      mockStates.add(mockState);
    }

    // test
    final List<StateDraft> referenceReplacedDrafts = mapToStateDrafts(mockStates);

    // assertion
    referenceReplacedDrafts.forEach(
        stateDraft -> {
          stateDraft
              .getTransitions()
              .forEach(
                  stateReference -> {
                    assertThat(stateReference.getId()).isEqualTo(stateReferenceKey);
                    assertThat(stateReference.getObj()).isEqualTo(null);
                  });
        });
  }

  @Test
  void mapToStateDrafts_WithAllNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
    // preparation
    final List<State> mockStates = new ArrayList<>();
    final String stateReferenceKey = "state-key";

    for (int i = 0; i < 3; i++) {
      final State mockState = mock(State.class);

      final State state = getStateMock(stateReferenceKey);
      final Reference<State> nonExpandedStateReference = State.referenceOfId(state.getId());
      when(mockState.getTransitions()).thenReturn(Collections.singleton(nonExpandedStateReference));

      mockStates.add(mockState);
    }

    // test
    final List<StateDraft> referenceReplacedDrafts = mapToStateDrafts(mockStates);

    // assertion
    referenceReplacedDrafts.forEach(
        stateDraft -> {
          stateDraft
              .getTransitions()
              .forEach(
                  stateReference -> {
                    assertThat(stateReference.getId()).isNotEqualTo(stateReferenceKey);
                  });
        });
  }

  @Test
  void mapToStateDrafts_WithNullOrEmptyReferences_ShouldNotFail() {
    // preparation
    final State mockState1 = mock(State.class);
    when(mockState1.getTransitions()).thenReturn(null);

    final State mockState2 = mock(State.class);
    when(mockState2.getTransitions()).thenReturn(Collections.emptySet());

    // test
    final List<StateDraft> referenceReplacedDrafts =
        mapToStateDrafts(Arrays.asList(mockState1, mockState2));

    assertThat(referenceReplacedDrafts.get(0).getTransitions()).isEqualTo(Collections.emptySet());
    assertThat(referenceReplacedDrafts.get(1).getTransitions()).isEqualTo(Collections.emptySet());
  }

  @Test
  void buildStateQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final StateQuery stateQuery = buildStateQuery();
    assertThat(stateQuery.expansionPaths()).containsExactly(ExpansionPath.of("transitions[*]"));
  }

  @Nonnull
  private static State getStateMock(@Nonnull final String key) {
    final State state = mock(State.class);
    when(state.getKey()).thenReturn(key);
    when(state.getId()).thenReturn(UUID.randomUUID().toString());
    return state;
  }
}
