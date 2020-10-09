package com.commercetools.sync.customers.commands.updateactions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetDefaultBillingAddressWithKeyTest {

    @Test
    void whenSetDefaultBillingAddressWithKeyIsCreated_thenGetAddressKeyReturnsCorrectKey() {

        final String billingAddressKey = "key";
        final SetDefaultBillingAddressWithKey setDefaultBillingAddressWithKey = SetDefaultBillingAddressWithKey
            .of(billingAddressKey);

        assertThat(setDefaultBillingAddressWithKey.getAddressKey()).isEqualTo("key");
    }
}