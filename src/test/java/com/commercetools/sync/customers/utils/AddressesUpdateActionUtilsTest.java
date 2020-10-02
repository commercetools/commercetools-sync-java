package com.commercetools.sync.customers.utils;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddressesUpdateActionUtilsTest {

    private static Address old;
    private static Address newSame;
    private static Address newDifferent;

    @BeforeEach
    void setup(){

        final String key = "old-key";
        final String newKey = "new-key";

        old = mock(Address.class);
        when(old.getKey()).thenReturn(key);

        newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
        newDifferent = AddressBuilder.of(newSame).key(newKey).build();

        final List<Address> oldAddresses;

        //don't know if necessary:
        //final List<Address> newAddresses;





    }
}
