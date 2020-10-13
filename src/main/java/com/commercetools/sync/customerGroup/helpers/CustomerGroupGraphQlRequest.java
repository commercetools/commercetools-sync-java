package com.commercetools.sync.customerGroup.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class CustomerGroupGraphQlRequest extends BaseGraphQlRequest<CustomerGroupGraphQlResult> {
    private static final String ENDPOINT = "customerGroups";
    private static final long LIMIT = 10;

    public CustomerGroupGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, CustomerGroupGraphQlResult.class, LIMIT);
    }
}
