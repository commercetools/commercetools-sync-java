package com.commercetools.sync.producttypes.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class ProductTypeGraphQlRequest extends BaseGraphQlRequest<ProductTypeGraphQlResult> {

    private static final String ENDPOINT = "productTypes";
    private static final long LIMIT = 10;

    public ProductTypeGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, ProductTypeGraphQlResult.class, LIMIT);
    }
}