package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public CategoryGraphQlResult(@JsonProperty("categories") final BaseGraphQlResult categories) {
        super(categories.getResults());
    }
}
