package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

public final class WaitingProductsToBeResolved<T extends ProductDraft> implements WaitingToBeResolved<T> {
    private T waitingDraft;
    private Set<String> missingReferencedKeys;
    public WaitingProductsToBeResolved(){};

    /**
     * Represents a productDraft that is waiting for some product references, which are on this productDraft as
     * attributes, to be resolved.
     *
     * @param draft                 draft which has irresolvable references as attributes.
     * @param missingReferencedKeys product keys of irresolvable references.
     */
    public WaitingProductsToBeResolved(
        @Nonnull final T draft,
        @Nonnull final Set<String> missingReferencedKeys) {
        this.waitingDraft = draft;
        this.missingReferencedKeys = missingReferencedKeys;
    }


    @Override
    public Set<String> getMissingReferencedKeys() {
        return missingReferencedKeys;
    }

    @Override
    public void setWaitingDraft(@Nonnull final T draft) {
        waitingDraft = (T) draft;
    }

    @Override
    public T getWaitingDraft() {
        return waitingDraft;
    }

    @Override
    public Set<String> getMissingReferencedProductKeys() {
        return missingReferencedKeys;
    }


    @Override
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
        return Objects.equals(waitingDraft.getKey(), that.getWaitingDraft().getKey())
            && getMissingReferencedProductKeys().equals(that.getMissingReferencedProductKeys());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWaitingDraft().getKey(), getMissingReferencedProductKeys());
    }


}