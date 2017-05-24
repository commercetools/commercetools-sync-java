package com.commercetools.sync.commons.models;

public abstract class NewResourceBuilder<T, U extends NewResource> {
    protected T resourceDraft;

    /**
     * Creates new instance of {@code S} which extends {@link NewResource} enriched with all attributes provided to
     * {@code this} builder.
     *
     * @return new instance of S which extends {@link NewResource}
     */
    protected abstract U build();
}
