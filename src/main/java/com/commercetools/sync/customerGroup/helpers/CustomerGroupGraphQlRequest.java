package com.commercetools.sync.customerGroup.helpers;

import com.commercetools.sync.commons.helpers.BaseGraphQlRequest;

import javax.annotation.Nonnull;
import java.util.Set;

public class CustomerGroupGraphQlRequest extends BaseGraphQlRequest<CustomerGroupGraphQlRequest,
    CustomerGroupGraphQlResult> {

    private static final String ENDPOINT = "customerGroups";

    public CustomerGroupGraphQlRequest(@Nonnull final Set<String> keysToSearch) {
        super(keysToSearch, ENDPOINT, CustomerGroupGraphQlResult.class);
    }

    /**
     * Returns an instance of this class to be used in the superclass's generic methods. Please see the JavaDoc in the
     * overridden method for further details.
     *
     * @return an instance of this class.
     */
    @Override
    protected CustomerGroupGraphQlRequest getThis() {
        return this;
    }
}
