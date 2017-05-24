package com.commercetools.sync.commons.models;

public abstract class OldResourceBuilder<T, U extends OldResource> {
    protected T resource;

    /**
     * Creates new instance of {@code S} which extends {@link OldResource} enriched with all attributes provided to
     * {@code this} builder.
     *
     * @return new instance of S which extends {@link OldResource}
     */
    protected abstract U build();
}
