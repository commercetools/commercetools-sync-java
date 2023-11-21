package com.commercetools.sync.states.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateDraft;
import com.commercetools.api.models.state.StateReference;
import com.commercetools.api.models.state.StateReferenceBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class StateReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  private static final String STATE_REFERENCE_KEY = "state-key";
  private static final String STATE_KEY = "mock-state-key";

  @Test
  void
      mapToStateDrafts_WithAllUnexpandedReferences_ShouldReturnResourceIdentifiersWithReplacedKeys() {
    // preparation
    final List<State> mockStates = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      final State referencedState = getStateMock(STATE_REFERENCE_KEY);
      final String stateId = referencedState.getId();
      final StateReference unexpandedStateReference =
          StateReferenceBuilder.of().id(stateId).build();

      final State mockState = getStateMock(STATE_KEY);
      when(mockState.getTransitions()).thenReturn(List.of(unexpandedStateReference));
      referenceIdToKeyCache.add(stateId, STATE_REFERENCE_KEY);

      mockStates.add(mockState);
    }

    // test
    final List<StateDraft> resourceIdentifiersReplacedDrafts =
        StateReferenceResolutionUtils.mapToStateDrafts(mockStates, referenceIdToKeyCache);

    // assertion
    resourceIdentifiersReplacedDrafts.forEach(
        stateDraft -> {
          stateDraft
              .getTransitions()
              .forEach(
                  stateResourceIdentifiers -> {
                    assertThat(stateResourceIdentifiers.getId())
                        .isEqualTo(stateResourceIdentifiers.getId());
                    assertThat(stateResourceIdentifiers.getKey()).isEqualTo(STATE_REFERENCE_KEY);
                  });
        });
  }

  @Test
  void mapToStateDrafts_WithAllNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
    // preparation
    final List<State> mockStates = new ArrayList<>();

    for (int i = 0; i < 3; i++) {
      final State referencedState = getStateMock(STATE_REFERENCE_KEY);
      final StateReference nonExpandedStateReference =
          StateReferenceBuilder.of().id(referencedState.getId()).build();

      final State mockState = getStateMock(STATE_KEY);
      when(mockState.getTransitions()).thenReturn(List.of(nonExpandedStateReference));

      mockStates.add(mockState);
    }

    // test
    final List<StateDraft> referenceReplacedDrafts =
        StateReferenceResolutionUtils.mapToStateDrafts(mockStates, referenceIdToKeyCache);

    // assertion
    referenceReplacedDrafts.forEach(
        stateDraft -> {
          stateDraft
              .getTransitions()
              .forEach(
                  stateReference -> {
                    assertThat(stateReference.getId()).isNotEqualTo(STATE_REFERENCE_KEY);
                  });
        });
  }

  @Test
  void mapToStateDrafts_WithEmptyReferences_ShouldNotFail() {
    // preparation
    final State mockState = getStateMock(STATE_KEY);
    when(mockState.getTransitions()).thenReturn(List.of());

    // test
    final List<StateDraft> referenceReplacedDrafts =
        StateReferenceResolutionUtils.mapToStateDrafts(List.of(mockState), referenceIdToKeyCache);

    assertThat(referenceReplacedDrafts.get(0).getTransitions()).isEqualTo(List.of());
  }

  @Test
  void mapToStateDrafts_WithNullReferences_ShouldNotFail() {
    // preparation
    final State mockState = getStateMock(STATE_KEY);
    when(mockState.getTransitions()).thenReturn(null);

    // test
    final List<StateDraft> referenceReplacedDrafts =
        StateReferenceResolutionUtils.mapToStateDrafts(List.of(mockState), referenceIdToKeyCache);

    assertThat(referenceReplacedDrafts.get(0).getTransitions()).isEqualTo(null);
  }

  @Test
  void mapToStateDrafts_WithMissingRequiredFields_ShouldNotFailAndReturnEmptyDraft() {
    // preparation
    final State mockState = mock(State.class);
    when(mockState.getTransitions()).thenReturn(null);

    // test
    final List<StateDraft> referenceReplacedDrafts =
        StateReferenceResolutionUtils.mapToStateDrafts(List.of(mockState), referenceIdToKeyCache);

    assertThat(referenceReplacedDrafts.get(0)).isEqualTo(StateDraft.of());
  }

  @Nonnull
  private static State getStateMock(@Nonnull final String key) {
    final State state = mock(State.class);
    when(state.getKey()).thenReturn(key);
    when(state.getId()).thenReturn(UUID.randomUUID().toString());
    when(state.getType()).thenReturn(StateTypeEnum.PRODUCT_STATE);
    return state;
  }
}
