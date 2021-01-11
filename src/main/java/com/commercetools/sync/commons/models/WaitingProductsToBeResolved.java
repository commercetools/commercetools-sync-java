package com.commercetools.sync.commons.models;

import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.Set;

public final class WaitingProductsToBeResolved<T extends ProductDraft> extends WaitingToBeResolved<T> {
    private T waitingDraft;


    public WaitingProductsToBeResolved() {
    }

     public WaitingProductsToBeResolved(
        @Nonnull final T draft,
        @Nonnull final Set<String> missingReferencedKeys) {
        this.waitingDraft = draft;
        this.setMissingReferencedKeys(missingReferencedKeys);
    }


    @Override
    public void setWaitingDraft(@Nonnull final T draft) {
        waitingDraft = (T) draft;
    }

    @Override
    public T getWaitingDraft() {
        return waitingDraft;
    }

}