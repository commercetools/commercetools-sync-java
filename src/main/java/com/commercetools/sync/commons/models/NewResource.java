package com.commercetools.sync.commons.models;


import javax.annotation.Nonnull;

public class NewResource<T> {
    private T resourceDraft;

    protected NewResource(@Nonnull final T resourceDraft) {
        this.resourceDraft = resourceDraft;
    }

    public T getResourceDraft(){
        return resourceDraft;
    }
}
