package com.commercetools.sync.taxcategories.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxCategoryGraphQlResult extends BaseGraphQlResult {

    @JsonCreator
    public TaxCategoryGraphQlResult(@JsonProperty("taxCategories") final BaseGraphQlResult taxCategories) {
        super(taxCategories.getResults());
    }
}
