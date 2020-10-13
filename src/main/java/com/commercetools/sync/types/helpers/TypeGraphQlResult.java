package com.commercetools.sync.types.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TypeGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public TypeGraphQlResult(@JsonProperty("types") final BaseGraphQlResult types) {
        super(types.getResults());
    }
}
