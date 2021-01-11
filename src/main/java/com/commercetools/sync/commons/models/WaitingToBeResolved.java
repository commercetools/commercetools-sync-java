package com.commercetools.sync.commons.models;


import io.sphere.sdk.models.WithKey;

import javax.annotation.Nonnull;
import java.util.Set;

public interface WaitingToBeResolved<T extends WithKey> {

    // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
    // from CTP custom objects.

    public Set<String> getMissingReferencedKeys();

    public void setWaitingDraft(@Nonnull final T draft);

    public void setMissingReferencedKeys(@Nonnull final Set<String> missingReferencedKeys);

    public T getWaitingDraft();

    public Set<String> getMissingReferencedProductKeys();

    @Override
    public boolean equals(final Object other);

    @Override
    public int hashCode();
}
