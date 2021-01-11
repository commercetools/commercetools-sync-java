package com.commercetools.sync.commons.models;


import io.sphere.sdk.models.WithKey;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

public abstract class WaitingToBeResolved<T extends WithKey> {
    private Set<String> missingReferencedKeys;

    public WaitingToBeResolved() {
    }

    public Set<String> getMissingReferencedKeys() {
        return missingReferencedKeys;
    }

    public abstract void setWaitingDraft(@Nonnull final T draft);

    public abstract T getWaitingDraft();

    public void setMissingReferencedKeys(@Nonnull Set keys) {
        missingReferencedKeys = keys;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WaitingToBeResolved)) {
            return false;
        }
        final WaitingToBeResolved that = (WaitingToBeResolved) other;
        return Objects.equals(getWaitingDraft().getKey(), that.getWaitingDraft().getKey())
            && getMissingReferencedKeys().equals(that.getMissingReferencedKeys());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWaitingDraft().getKey(), getMissingReferencedKeys());
    }
}
