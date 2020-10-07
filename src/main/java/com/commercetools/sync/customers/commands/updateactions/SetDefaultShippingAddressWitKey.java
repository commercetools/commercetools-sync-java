package com.commercetools.sync.customers.commands.updateactions;

import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.customers.Customer;

import javax.annotation.Nonnull;

// TODO (JVM-SDK), see: SUPPORT-10260, Address selection by key is not supported yet.
public final class SetDefaultShippingAddressWitKey extends UpdateActionImpl<Customer> {
    private final String addressKey;

    private SetDefaultShippingAddressWitKey(@Nonnull final String addressKey) {
        super("setDefaultShippingAddress");
        this.addressKey = addressKey;
    }

    public static SetDefaultShippingAddressWitKey of(@Nonnull final String addressKey) {
        return new SetDefaultShippingAddressWitKey(addressKey);
    }

    @Nonnull
    public String getAddressKey() {
        return addressKey;
    }
}

