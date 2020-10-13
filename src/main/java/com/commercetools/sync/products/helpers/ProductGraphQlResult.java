package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public ProductGraphQlResult(@JsonProperty("products") final BaseGraphQlResult products) {
        super(products.getResults());
    }
}
