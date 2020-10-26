package com.commercetools.sync.customers.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class CustomerGraphQlRequest extends BaseGraphQlRequest<CustomerGraphQlResult> {
        private static final String ENDPOINT = "customers";
        private static final long LIMIT = 10;

        public CustomerGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
            super(keysToSearch, ENDPOINT, CustomerGraphQlResult.class, LIMIT);
        }
}
