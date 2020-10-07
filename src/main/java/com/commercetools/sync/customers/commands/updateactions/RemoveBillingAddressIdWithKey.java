package com.commercetools.sync.customers.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;

// TODO (JVM-SDK), see: SUPPORT-10260, Address selection by key is not supported yet.
public final class RemoveBillingAddressIdWithKey extends UpdateActionImpl<Customer> {
    private final String addressKey;

    private RemoveBillingAddressIdWithKey(final String addressKey) {
        super("removeBillingAddressId");
        this.addressKey = addressKey;
    }

    public static RemoveBillingAddressIdWithKey of(final String addressKey) {
        return new RemoveBillingAddressIdWithKey(addressKey);
    }

    public String getAddressKey() {
        return addressKey;
    }
}
