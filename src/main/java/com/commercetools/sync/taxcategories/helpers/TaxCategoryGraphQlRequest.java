package com.commercetools.sync.taxcategories.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class TaxCategoryGraphQlRequest extends BaseGraphQlRequest<TaxCategoryGraphQlRequest, TaxCategoryGraphQlResult> {

    private static final String ENDPOINT = "taxCategories";

    public TaxCategoryGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, TaxCategoryGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected TaxCategoryGraphQlRequest getThis() {
        return this;
    }
}