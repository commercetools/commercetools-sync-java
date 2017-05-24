package com.commercetools.sync.commons.models;

import javax.annotation.Nonnull;

public class OldResource<T> {
    private T resource;

    protected OldResource(@Nonnull final T resource) {
        this.resource = resource;
    }

    public T getResource() {
        return resource;
    }
}
