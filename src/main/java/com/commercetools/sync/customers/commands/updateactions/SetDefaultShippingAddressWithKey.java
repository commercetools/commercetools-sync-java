package com.commercetools.sync.customers.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;

import javax.annotation.Nullable;

// TODO (JVM-SDK), see: SUPPORT-10260, Address selection by key is not supported yet.
// https://github.com/commercetools/commercetools-jvm-sdk/issues/2071
public final class SetDefaultShippingAddressWithKey extends UpdateActionImpl<Customer> {
    private final String addressKey;

    private SetDefaultShippingAddressWithKey(@Nullable final String addressKey) {
        super("setDefaultShippingAddress");
        this.addressKey = addressKey;
    }

    public static SetDefaultShippingAddressWithKey of(@Nullable final String addressKey) {
        return new SetDefaultShippingAddressWithKey(addressKey);
    }

    @Nullable
    public String getAddressKey() {
        return addressKey;
    }
}

