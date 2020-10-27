package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class ProductGraphQlRequest extends BaseGraphQlRequest<ProductGraphQlRequest, ProductGraphQlResult> {

    private static final String SEARCH_TERM = "products";

    public ProductGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, SEARCH_TERM, ProductGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected ProductGraphQlRequest getThis() {
        return this;
    }
}
