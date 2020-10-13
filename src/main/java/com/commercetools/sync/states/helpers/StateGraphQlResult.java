package com.commercetools.sync.states.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StateGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public StateGraphQlResult(@JsonProperty("states") final BaseGraphQlResult states) {
        super(states.getResults());
    }
}
