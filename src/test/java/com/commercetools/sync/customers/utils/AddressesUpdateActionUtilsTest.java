package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.customers.utils.AddressesUpdateActionUtils.buildAddressUpdateActions;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddressesUpdateActionUtilsTest {

    private static Address old;
    private static Address newSame;
    private static Address newDifferent;

    @BeforeEach
    void setup() {

    }

    //TODO case 1:
    //Addresses in the draft, but no address in target, create all addresses in the draft to the target


    @Test
    void buildAddressUpdateActions_withNewAddress_shouldReturnAction() throws BuildUpdateActionException {
        final String key = "old-key";
        final String key2 = "old-key2";
        final String newKey = "new-key";
        final String id = "old-id";
        final String id2 = "old-id2";
        final String newId = "newId";


        old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
        newDifferent = AddressBuilder.of(newSame).key(newKey).build();


        final List<Address> oldAddresses = asList(old, anotherAddress);
        //final List<Address> oldAddresses = Collections.emptyList();

        final List<Address> newAddresses = asList(old, newDifferent);
        final List<UpdateAction<Customer>> result = buildAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(AddAddress.of(newDifferent));

    }


    //TODO case 2:
    //No address in the draft, but has addresses in the target, remove all addresses of the target.

    //TODO case 3:
    //Same addresses in drafts and the target project with diffs.

    //TODO case 4:
    //Remove the addresses that do not exist in the draft, add addresses that exist in the draft but not in the target
    //project.
}
