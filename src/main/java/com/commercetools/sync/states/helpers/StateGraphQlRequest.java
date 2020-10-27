package com.commercetools.sync.states.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class StateGraphQlRequest extends BaseGraphQlRequest<StateGraphQlRequest, StateGraphQlResult> {

    private static final String ENDPOINT = "states";

    public StateGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, StateGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected StateGraphQlRequest getThis() {
        return this;
    }
}
