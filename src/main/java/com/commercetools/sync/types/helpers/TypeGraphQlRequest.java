package com.commercetools.sync.types.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class TypeGraphQlRequest extends BaseGraphQlRequest<TypeGraphQlRequest, TypeGraphQlResult> {

    private static final String ENDPOINT = "types";

    public TypeGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, TypeGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected TypeGraphQlRequest getThis() {
        return this;
    }
}