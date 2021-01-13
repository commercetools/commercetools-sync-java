package com.commercetools.sync.commons.models;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.states.StateDraft;
import java.util.HashSet;
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
    waitingToBeResolvedTransition.setMissingTransitionStateKeys(emptySet());

    // assertions
    assertThat(waitingToBeResolvedTransition.getMissingTransitionStateKeys()).isEqualTo(emptySet());
  }

  @Test
  void equals_WithSameRef_ShouldReturnTrue() {
    // preparation
    final WaitingToBeResolvedTransitions waitingToBeResolvedTransition =
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), new HashSet<>());
    final WaitingToBeResolvedTransitions other = waitingToBeResolvedTransition;

    // test
    boolean result = waitingToBeResolvedTransition.equals(other);

    // assertions
    assertTrue(result);
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
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), singleton("foo"));

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
        new WaitingToBeResolvedTransitions(stateDraft, singleton("foo"));
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(stateDraft1, singleton("bar"));

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
    final WaitingToBeResolvedTransitions other = waitingToBeResolvedTransition;

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = other.hashCode();

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
        new WaitingToBeResolvedTransitions(mock(StateDraft.class), singleton("foo"));

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
        new WaitingToBeResolvedTransitions(stateDraft, singleton("foo"));
    final WaitingToBeResolvedTransitions other =
        new WaitingToBeResolvedTransitions(stateDraft1, singleton("bar"));

    // test
    final int hash1 = waitingToBeResolvedTransition.hashCode();
    final int hash2 = other.hashCode();

    // assertions
    assertNotEquals(hash1, hash2);
  }
}
