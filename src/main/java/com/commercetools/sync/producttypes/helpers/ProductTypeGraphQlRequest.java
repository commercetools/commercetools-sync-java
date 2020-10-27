package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class ProductTypeGraphQlRequest extends BaseGraphQlRequest<ProductTypeGraphQlRequest, ProductTypeGraphQlResult> {

    private static final String ENDPOINT = "productTypes";

    public ProductTypeGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, ProductTypeGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected ProductTypeGraphQlRequest getThis() {
        return this;
    }
}