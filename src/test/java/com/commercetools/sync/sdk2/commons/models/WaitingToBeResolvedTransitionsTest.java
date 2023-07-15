package com.commercetools.sync.sdk2.commons.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.state.StateDraft;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WaitingToBeResolvedTransitionsTest {

  @Test
  void setProductDraft_WithNonNullProductDraft_ShouldSetProductDraft() {
    // preparation
    final StateDraft stateDraft = mock(StateDraft.class);
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions();

    // test
    waitingToBeResolvedTransition.setStateDraft(stateDraft);

    // assertions
    assertThat(waitingToBeResolvedTransition.getStateDraft()).isEqualTo(stateDraft);
  }

  @Test
  void setMissingReferencedProductKeys_WithNonNullSet_ShouldSetTheSet() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions();

    // test
    waitingToBeResolvedTransition.setMissingTransitionStateKeys(Set.of());

    // assertions
    assertThat(waitingToBeResolvedTransition.getMissingTransitionStateKeys()).isEqualTo(Set.of());
  }

  @Test
  void equals_WithDiffType_ShouldReturnFalse() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());
    final Object other = new Object();

    // test
    boolean result = waitingToBeResolvedTransition.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithEqualObjects_ShouldReturnTrue() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());

    // test
    boolean result = waitingToBeResolvedTransition.equals(other);

    // assertions
    assertTrue(result);
  }

  @Test
  void equals_WithDifferentMissingRefKeys_ShouldReturnFalse() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), Set.of("foo"));

    // test
    boolean result = waitingToBeResolvedTransition.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithDifferentProductDraft_ShouldReturnFalse() {
    // preparation
    final StateDraft stateDraft = mock(StateDraft.class);
    final StateDraft stateDraft1 = mock(StateDraft.class);
    when(stateDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(stateDraft, new HashSet<>());
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(stateDraft1, new HashSet<>());

    // test
    boolean result = waitingToBeResolvedTransition.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void equals_WithCompletelyDifferentFieldValues_ShouldReturnFalse() {
    // preparation
    final StateDraft stateDraft = mock(StateDraft.class);
    final StateDraft stateDraft1 = mock(StateDraft.class);
    when(stateDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(stateDraft, Set.of("foo"));
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(stateDraft1, Set.of("bar"));

    // test
    boolean result = waitingToBeResolvedTransition.equals(other);

    // assertions
    assertFalse(result);
  }

  @Test
  void hashCode_withSameInstances_ShouldBeEquals() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = waitingToBeResolvedTransition.hashCode();

    // assertions
    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withSameProductKeyAndSameRefSet_ShouldBeEquals() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertEquals(hash1, hash2);
  }

  @Test
  void hashCode_withDifferentProductKeyAndSameRefSet_ShouldNotBeEquals() {
    // preparation
    final StateDraft stateDraft = mock(StateDraft.class);
    final StateDraft stateDraft1 = mock(StateDraft.class);
    when(stateDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(stateDraft, new HashSet<>());
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(stateDraft1, new HashSet<>());

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withSameProductKeyAndDiffRefSet_ShouldNotBeEquals() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), Set.of("foo"));

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }

  @Test
  void hashCode_withCompletelyDifferentFields_ShouldNotBeEquals() {
    // preparation
    final StateDraft stateDraft = mock(StateDraft.class);
    final StateDraft stateDraft1 = mock(StateDraft.class);
    when(stateDraft1.getKey()).thenReturn("foo");

    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(stateDraft, Set.of("foo"));
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(stateDraft1, Set.of("bar"));

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }
}
