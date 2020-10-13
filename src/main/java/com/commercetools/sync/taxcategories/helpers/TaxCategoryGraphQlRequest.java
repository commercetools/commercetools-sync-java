package com.commercetools.sync.taxcategories.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class TaxCategoryGraphQlRequest extends BaseGraphQlRequest<TaxCategoryGraphQlResult> {

    private static final String ENDPOINT = "taxCategories";
    private static final long LIMIT = 10;

    public TaxCategoryGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, TaxCategoryGraphQlResult.class, LIMIT);
    }
}