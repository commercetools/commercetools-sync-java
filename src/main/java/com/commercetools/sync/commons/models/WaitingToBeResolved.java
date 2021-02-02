package com.commercetools.sync.commons.models;

import io.sphere.sdk.models.WithKey;

public abstract class WaitingToBeResolved<T extends WithKey> {

  public abstract String getKey();

  @Override
  public abstract boolean equals(final Object other);

  @Override
  public abstract int hashCode();
}
