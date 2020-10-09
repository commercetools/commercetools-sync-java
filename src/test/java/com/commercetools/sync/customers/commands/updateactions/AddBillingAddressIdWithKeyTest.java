package com.commercetools.sync.customers.commands.updateactions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AddBillingAddressIdWithKeyTest {

    @Test
    void whenAddBillingAddressIdWithKeyIsCreated_thenGetAddressKeyReturnsCorrectKey() {

        final String billingAddressKey = "key";
        final AddBillingAddressIdWithKey addBillingAddressIdWithKey = AddBillingAddressIdWithKey.of(billingAddressKey);

        assertThat(addBillingAddressIdWithKey.getAddressKey()).isEqualTo("key");
    }

}