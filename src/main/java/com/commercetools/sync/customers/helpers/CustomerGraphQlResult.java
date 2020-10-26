package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public CustomerGraphQlResult(@JsonProperty("customers") final BaseGraphQlResult customers) {
        super(customers.getResults());
    }
}
