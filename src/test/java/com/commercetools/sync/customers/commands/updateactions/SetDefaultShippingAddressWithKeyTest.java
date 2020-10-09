package com.commercetools.sync.customers.commands.updateactions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SetDefaultShippingAddressWithKeyTest {
    @Test
    void whenSetDefaultShippingAddressWithKeyIsCreated_thenGetAddressKeyReturnsCorrectKey(){

        final String shippingAddressKey = "key";
        final SetDefaultShippingAddressWithKey setDefaultShippingAddressWithKey = SetDefaultShippingAddressWithKey.of(shippingAddressKey);

        assertThat(setDefaultShippingAddressWithKey.getAddressKey()).isEqualTo("key");
    }

}