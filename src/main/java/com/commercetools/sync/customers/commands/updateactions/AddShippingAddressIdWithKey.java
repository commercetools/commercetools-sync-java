package com.commercetools.sync.customers.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;

// TODO (JVM-SDK), see: SUPPORT-10260, Address selection by key is not supported yet.
public final class AddShippingAddressIdWithKey extends UpdateActionImpl<Customer> {
    private final String addressKey;

    private AddShippingAddressIdWithKey(final String addressKey) {
        super("addShippingAddressId");
        this.addressKey = addressKey;
    }

    public static AddShippingAddressIdWithKey of(final String addressKey) {
        return new AddShippingAddressIdWithKey(addressKey);
    }

    public String getAddressKey() {
        return addressKey;
    }
}
