package com.commercetools.sync.customers.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;

// TODO (JVM-SDK), see: SUPPORT-10260, Address selection by key is not supported yet.
public final class RemoveShippingAddressIdWithKey extends UpdateActionImpl<Customer> {
    private final String addressKey;

    private RemoveShippingAddressIdWithKey(final String addressKey) {
        super("removeShippingAddressId");
        this.addressKey = addressKey;
    }

    public static RemoveShippingAddressIdWithKey of(final String addressKey) {
        return new RemoveShippingAddressIdWithKey(addressKey);
    }

    public String getAddressKey() {
        return addressKey;
    }
}
