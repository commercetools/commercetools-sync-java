package com.commercetools.sync.commons.models;

import io.sphere.sdk.states.StateDraft;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

public final class WaitingToBeResolvedTransitions {
  private StateDraft stateDraft;
  private Set<String> missingTransitionStateKeys;

  /**
   * Represents a statedraft that is waiting for some state references, which are on this stateDraft
   * as transitions, to be resolved.
   *
   * @param stateDraft state draft which has irresolvable references as transitions.
   * @param missingTransitionStateKeys state keys of irresolvable transition states.
   */
  public WaitingToBeResolvedTransitions(
      @Nonnull final StateDraft stateDraft, @Nonnull final Set<String> missingTransitionStateKeys) {
    this.stateDraft = stateDraft;
    this.missingTransitionStateKeys = missingTransitionStateKeys;
  }

  // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
  // from CTP custom objects.
  public WaitingToBeResolvedTransitions() {}

  @Nonnull
  public StateDraft getStateDraft() {
    return stateDraft;
  }

  @Nonnull
  public Set<String> getMissingTransitionStateKeys() {
    return missingTransitionStateKeys;
  }

  public void setStateDraft(@Nonnull final StateDraft stateDraft) {
    this.stateDraft = stateDraft;
  }

  public void setMissingTransitionStateKeys(@Nonnull final Set<String> missingTransitionStateKeys) {
    this.missingTransitionStateKeys = missingTransitionStateKeys;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof WaitingToBeResolvedTransitions)) {
      return false;
    }
    final WaitingToBeResolvedTransitions that = (WaitingToBeResolvedTransitions) other;
    return Objects.equals(getStateDraft().getKey(), that.getStateDraft().getKey())
        && getMissingTransitionStateKeys().equals(that.getMissingTransitionStateKeys());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStateDraft().getKey(), getMissingTransitionStateKeys());
  }
}
