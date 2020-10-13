package com.commercetools.sync.customerGroup.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerGroupGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public CustomerGroupGraphQlResult(@JsonProperty("customerGroups") final BaseGraphQlResult customerGroups) {
        super(customerGroups.getResults());
    }
}
