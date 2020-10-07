package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.commercetools.sync.customers.utils.AddressesUpdateActionUtils.buildAddressUpdateActions;
import static com.commercetools.sync.customers.utils.AddressesUpdateActionUtils.buildRemoveAddressUpdateActions;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AddressesUpdateActionUtilsTest {

//    private static Address old;
//    private static Address newSame;
//    private static Address newDifferent;
//    private static Address anotherAddress;

//    @BeforeEach
//    void setup() {
//
//        final String key = "old-key";
//        final String key2 = "old-key2";
//        final String newKey = "new-key";
//        final String id = "old-id";
//        final String id2 = "old-id2";
//        final String newId = "newId";
//
//
//        old = mock(Address.class);
//        when(old.getKey()).thenReturn(key);
//        when(old.getId()).thenReturn(id);
//
//        anotherAddress = mock(Address.class);
//        when(anotherAddress.getKey()).thenReturn(key2);
//        when(anotherAddress.getId()).thenReturn(id2);
//
//        newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
//        newDifferent = AddressBuilder.of(newSame).key(newKey).build();
//    }

    //case 1:
    //Addresses in the draft, but no address in target, create all addresses in the draft to the target

    @Test
    void buildAddressUpdateActions_withNewAddressWithoutExisting_shouldReturnAction() throws BuildUpdateActionException {

        final String key = "old-key";
        final String key2 = "old-key2";
        final String newKey = "new-key";
        final String id = "old-id";
        final String id2 = "old-id2";
        final String newId = "newId";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

       final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final Address newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
        final Address newDifferent = AddressBuilder.of(newSame).key(newKey).build();

        final List<Address> oldAddresses = Collections.emptyList();

        final List<Address> newAddresses = asList(old, newDifferent);
        final List<UpdateAction<Customer>> result = buildAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(AddAddress.of(old), AddAddress.of(newDifferent));
        assertThat(result).isNotEmpty();
    }

    //case 2:
    //No address in the draft, but has addresses in the target, remove all addresses of the target.
    @Test
    void buildAddressUpdateActions_withEmptyDraftWithExisting_shouldReturnAction() throws BuildUpdateActionException {

        final String key = "old-key";
        final String key2 = "old-key2";
        final String id = "old-id";
        final String id2 = "old-id2";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final List<Address> oldAddresses = asList(old, anotherAddress);

        final List<Address> newAddresses = Collections.emptyList();
        final List<UpdateAction<Customer>> result = buildAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(RemoveAddress.of(old), RemoveAddress.of(anotherAddress));
    }


    //case 3:
    //Same addresses in drafts and the target project with diffs.

    @Test
    void buildAddressUpdateActions_withNewAddressWithExisting_shouldReturnAction() throws BuildUpdateActionException {

        final String key = "old-key";
        final String key2 = "old-key2";
        final String newKey = "new-key";
        final String id = "old-id";
        final String id2 = "old-id2";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final Address newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
        final Address newDifferent = AddressBuilder.of(newSame).key(newKey).build();

        final List<Address> oldAddresses = asList(old, anotherAddress);

        final List<Address> newAddresses = asList(old, anotherAddress, newDifferent);
        final List<UpdateAction<Customer>> result = buildAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(AddAddress.of(newDifferent));

    }

    //case 4:
    //Remove the addresses that do not exist in the draft, add addresses that exist in the draft but not in the target
    //project.

    @Test
    void buildAddressUpdateActions_withNewAddressesAndRemovedAddresses_shouldReturnAction()
        throws BuildUpdateActionException {

        final String key = "old-key";
        final String key2 = "old-key2";
        final String newKey = "new-key";
        final String id = "old-id";
        final String id2 = "old-id2";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final Address newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
        final Address newDifferent = AddressBuilder.of(newSame).key(newKey).build();

        final List<Address> oldAddresses = asList(old, anotherAddress);

        final List<Address> newAddresses = asList(old, newDifferent);
        final List<UpdateAction<Customer>> result = buildAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(AddAddress.of(newDifferent), RemoveAddress.of(anotherAddress));

    }

    //TODO test cases for buildRemoveAddressUpdateActions

    @Test
    void buildRemoveAddressUpdateActions_withNewAddressWithExisting_shouldReturnAction(){

        final String key = "old-key";
        final String key2 = "old-key2";
        final String newKey = "new-key";
        final String id = "old-id";
        final String id2 = "old-id2";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final Address newSame = AddressBuilder.of(CountryCode.DE).key(key).build();
        final Address newDifferent = AddressBuilder.of(newSame).key(newKey).build();

        final List<Address> oldAddresses = asList(old, anotherAddress);
        final List<Address> newAddresses = asList(old, newDifferent);
        final List<UpdateAction<Customer>> result = buildRemoveAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(RemoveAddress.of(anotherAddress));
    }

    @Test
    void buildRemoveAddressUpdateActions_withoutNewAddressWithExisting_shouldReturnAction() {

        final String key = "old-key";
        final String key2 = "old-key2";
        final String id = "old-id";
        final String id2 = "old-id2";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final List<Address> oldAddresses = asList(old, anotherAddress);
        final List<Address> newAddresses = Collections.singletonList(old);
        final List<UpdateAction<Customer>> result = buildRemoveAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(RemoveAddress.of(anotherAddress));

    }

    @Test
    void buildRemoveAddressUpdateActions_withEmptyDraftWithExistingAddress_shouldReturnAction(){

        final String key = "old-key";
        final String key2 = "old-key2";
        final String newKey = "new-key";
        final String id = "old-id";
        final String id2 = "old-id2";

        final Address old = mock(Address.class);
        when(old.getKey()).thenReturn(key);
        when(old.getId()).thenReturn(id);

        final Address anotherAddress = mock(Address.class);
        when(anotherAddress.getKey()).thenReturn(key2);
        when(anotherAddress.getId()).thenReturn(id2);

        final List<Address> oldAddresses = asList(old, anotherAddress);
        final List<Address> newAddresses = Collections.singletonList(old);
        final List<UpdateAction<Customer>> result = buildRemoveAddressUpdateActions(oldAddresses, newAddresses);

        assertThat(result).contains(RemoveAddress.of(anotherAddress));

    }

    //TODO test cases for buildAddAddressUpdateActions





}
