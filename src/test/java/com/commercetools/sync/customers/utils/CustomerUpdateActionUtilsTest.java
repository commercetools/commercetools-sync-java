package com.commercetools.sync.customers.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class CustomerUpdateActionUtilsTest {

    private static Address old;
    private static Address newSame;
    private static Address newDifferent;


    @BeforeAll
    static void setup() {
        final Address address = AddressBuilder.of
    }

    @Test
    void buildChangeEmailUpdateAction_WithDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>>
    }

}
