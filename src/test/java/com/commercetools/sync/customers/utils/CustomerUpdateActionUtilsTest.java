package com.commercetools.sync.customers.utils;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import io.sphere.sdk.states.commands.updateactions.SetName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.changeEmailUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setFirstNameUpdateAction;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomerUpdateActionUtilsTest {

    private static Customer old;
    private static CustomerDraft newSame;
    private static CustomerDraft newDifferent;


    @BeforeAll
    static void setup() {
        final Address addressType = Address.of(CountryCode.DE);
        final Address address = AddressBuilder.of(addressType).build();

        final List<Address> oldAddresses = singletonList(address);
        final List<Address> sameAddressDrafts = singletonList(AddressBuilder.of(address).build());

//        old = mock(Customer.class);
//        when(old.getId()).thenReturn("addressId1");
//        when(old.get)
    }

    @Test
    void buildChangeEmailUpdateAction_WithDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = changeEmailUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeEmail.class);
        assertThat(result).contains(ChangeEmail.of(newDifferent.getEmail()));
    }

    @Test
    void buildChangeEmailUpdateAction_WithSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = changeEmailUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }



    @Test
    void buildSetFirstNameUpdateAction_WithDifferentValues_ShouldReturnAction(){
       final Optional<UpdateAction<Customer>> result = setFirstNameUpdateAction(old, newDifferent);

       assertThat(result).containsInstanceOf(SetFirstName.class);
       assertThat(result).contains(SetFirstName.of(newDifferent.getFirstName()));

    }

}
