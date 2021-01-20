package com.commercetools.sync.commons.models;

import io.sphere.sdk.models.WithKey;
import java.util.Set;
import javax.annotation.Nonnull;

public abstract class WaitingToBeResolved<T extends WithKey> {
  private Set<String> missingReferencedKeys;

  public WaitingToBeResolved() {}

  public Set<String> getMissingReferencedKeys() {
    return missingReferencedKeys;
  }

  public abstract void setWaitingDraft(@Nonnull final T draft);

  public abstract T fetchWaitingDraft();

  public void setMissingReferencedKeys(@Nonnull final Set keys) {
    missingReferencedKeys = keys;
  }
}
