package com.commercetools.sync.customers.utils;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerNumber;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetExternalId;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetKey;
import io.sphere.sdk.customers.commands.updateactions.SetLastName;
import io.sphere.sdk.customers.commands.updateactions.SetLocale;
import io.sphere.sdk.customers.commands.updateactions.SetMiddleName;
import io.sphere.sdk.customers.commands.updateactions.SetSalutation;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import io.sphere.sdk.customers.commands.updateactions.SetVatId;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.AddressBuilder;
import io.sphere.sdk.states.commands.updateactions.SetName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.changeEmailUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setCompanyNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setCustomerNumberUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setDateOfBirthUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setExternalIdUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setFirstNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setKeyUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setLastNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setLocaleUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setMiddleNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setSalutationUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setTitleUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.setVatIdUpdateAction;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomerUpdateActionUtilsTest {

    private static Customer old;
    private static CustomerDraft newSame;
    private static CustomerDraft newDifferent;


    @BeforeEach
      void setup() {

        CustomerName oldCustomerName =  CustomerName.of("old-title", "old-firstName", "old-middleName", "old-lastName");
        CustomerName newCustomerName = CustomerName.of("new-title", "new-firstName", "new-middleName", "new-lastName");

       old = mock(Customer.class);
       when(old.getKey()).thenReturn("key1");
       when(old.getName()).thenReturn(oldCustomerName);
       when(old.getEmail()).thenReturn("old-email");
       when(old.getFirstName()).thenReturn("old-firstName");
       when(old.getMiddleName()).thenReturn("old-middleName");
       when(old.getLastName()).thenReturn("old-lastName");
       when(old.getTitle()).thenReturn("old-title");
       when(old.getSalutation()).thenReturn("old-salutation");
       when(old.getCustomerNumber()).thenReturn("old-customerNumber");
       when(old.getExternalId()).thenReturn("old-externalId");
       when(old.getCompanyName()).thenReturn("old-companyName");
       when(old.getDateOfBirth()).thenReturn(LocalDate.parse("1990-10-01"));
       when(old.getVatId()).thenReturn("old-VatId");
       when(old.getLocale()).thenReturn(Locale.forLanguageTag("old-locale"));


       newSame = CustomerDraftBuilder.of(oldCustomerName, "old-email", "oldPW").build();
       newDifferent = CustomerDraftBuilder.of(newCustomerName, "new-email", "newPW").build();
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

    @Test
    void buildSetFirstNameUpdateAction_WithSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setFirstNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetLastNameUpdateAction_WithDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setLastNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetLastName.class);
        assertThat(result).contains(SetLastName.of(newDifferent.getLastName()));
    }

    @Test
    void buildSetLastNameUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setLastNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetMiddleNameUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setMiddleNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetMiddleName.class);
        assertThat(result).contains(SetMiddleName.of(newDifferent.getMiddleName()));
    }

    @Test
    void buildSetMiddleNameUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setMiddleNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetTitleUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setTitleUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetTitle.class);
        assertThat(result).contains(SetTitle.of(newDifferent.getTitle()));
    }

    @Test
    void buildSetTitleUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setTitleUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetSalutationUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setSalutationUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetSalutation.class);
        assertThat(result).contains(SetSalutation.of(newDifferent.getSalutation()));
    }

    @Test
    void buildSetSalutationUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setSalutationUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetCustomerNumberUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setCustomerNumberUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetCustomerNumber.class);
        assertThat(result).contains(SetCustomerNumber.of(newDifferent.getCustomerNumber()));
    }

    @Test
    void buildSetCustomerNumberUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setCustomerNumberUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetExternalIdUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setExternalIdUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetExternalId.class);
        assertThat(result).contains(SetExternalId.of(newDifferent.getExternalId()));
    }

    @Test
    void buildSetExternalIdUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setExternalIdUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetCompanyNameUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setCompanyNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetCompanyName.class);
        assertThat(result).contains(SetCompanyName.of(newDifferent.getCompanyName()));
    }

    @Test
    void buildSetCompanyNameUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setCompanyNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetDateOfBirthUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setDateOfBirthUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetDateOfBirth.class);
        assertThat(result).contains(SetDateOfBirth.of(newDifferent.getDateOfBirth()));
    }

    @Test
    void buildSetDateOfBirthUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setDateOfBirthUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetVatIdUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setVatIdUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetVatId.class);
        assertThat(result).contains(SetVatId.of(newDifferent.getVatId()));
    }

    @Test
    void buildSetVatIdUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setVatIdUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetLocaleUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setLocaleUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetLocale.class);
        assertThat(result).contains(SetLocale.of(newDifferent.getLocale()));
    }

    @Test
    void buildSetLocaleUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setLocaleUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetKeyUpdateAction_withDifferentValues_ShouldReturnAction(){
        final Optional<UpdateAction<Customer>> result = setKeyUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetKey.class);
        assertThat(result).contains(SetKey.of(newDifferent.getKey()));
    }

    @Test
    void buildSetKeyUpdateAction_withSameValues_ShouldReturnEmptyOptional(){
        final Optional<UpdateAction<Customer>> result = setKeyUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }
}
