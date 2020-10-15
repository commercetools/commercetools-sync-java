package com.commercetools.sync.customers.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;

import javax.annotation.Nonnull;

// TODO (JVM-SDK), see: SUPPORT-10260, Address selection by key is not supported yet.
// https://github.com/commercetools/commercetools-jvm-sdk/issues/2071
public final class AddShippingAddressIdWithKey extends UpdateActionImpl<Customer> {
    private final String addressKey;

    private AddShippingAddressIdWithKey(@Nonnull final String addressKey) {
        super("addShippingAddressId");
        this.addressKey = addressKey;
    }

    public static AddShippingAddressIdWithKey of(@Nonnull final String addressKey) {
        return new AddShippingAddressIdWithKey(addressKey);
    }

    @Nonnull
    public String getAddressKey() {
        return addressKey;
    }
}
