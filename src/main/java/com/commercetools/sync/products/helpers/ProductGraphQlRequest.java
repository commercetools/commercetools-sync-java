package com.commercetools.sync.products.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class ProductGraphQlRequest extends BaseGraphQlRequest {

    private static final String SEARCH_TERM = "products";
    private static final long LIMIT = 10;

    public ProductGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, SEARCH_TERM, ProductGraphQlResult.class, LIMIT);
    }
}
