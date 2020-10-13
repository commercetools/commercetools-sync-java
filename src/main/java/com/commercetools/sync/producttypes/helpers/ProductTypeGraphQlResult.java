package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductTypeGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public ProductTypeGraphQlResult(@JsonProperty("productTypes") final BaseGraphQlResult productTypes) {
        super(productTypes.getResults());
    }
}
