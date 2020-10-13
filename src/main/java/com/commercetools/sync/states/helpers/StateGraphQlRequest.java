package com.commercetools.sync.states.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class StateGraphQlRequest extends BaseGraphQlRequest<StateGraphQlResult> {

    private static final String ENDPOINT = "states";
    private static final long LIMIT = 10;

    public StateGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, StateGraphQlResult.class, LIMIT);
    }
}
