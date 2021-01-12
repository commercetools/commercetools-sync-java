package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.Set;

public final class WaitingProductsToBeResolved<T extends ProductDraft> extends WaitingToBeResolved<T> {
    private T waitingDraft;


    // Needed for the 'com.fasterxml.jackson' deserialization, for example, when fetching
    // from CTP custom objects.
    public WaitingProductsToBeResolved() {
    }

    public WaitingProductsToBeResolved(@Nonnull final T draft, @Nonnull final Set<String> missingReferencedKeys) {
        this.waitingDraft = draft;
        this.setMissingReferencedKeys(missingReferencedKeys);
    }

    public void setMissingReferencedKeys(@Nonnull final Set keys) {
        super.setMissingReferencedKeys(keys);
    }

    @Override
    public void setWaitingDraft(@Nonnull final ProductDraft draft) {
        waitingDraft = (T) draft;
    }

    @Override
    public T getWaitingDraft() {
        return waitingDraft;
    }

}