package com.commercetools.sync.customers.commands.updateactions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddShippingAddressIdWithKeyTest {

    @Test
    void whenAddShippingAddressIdWithKeyIsCreated_thenGetAddressKeyReturnsCorrectKey() {

        final String shippingAddressKey = "key";
        final AddShippingAddressIdWithKey addShippingAddressIdWithKey = AddShippingAddressIdWithKey
            .of(shippingAddressKey);

        assertThat(addShippingAddressIdWithKey.getAddressKey()).isEqualTo("key");
    }
}