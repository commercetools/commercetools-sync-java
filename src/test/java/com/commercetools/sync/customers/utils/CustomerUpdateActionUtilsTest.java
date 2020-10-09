package com.commercetools.sync.customers.utils;

import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerGroup;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerNumber;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetExternalId;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetLastName;
import io.sphere.sdk.customers.commands.updateactions.SetLocale;
import io.sphere.sdk.customers.commands.updateactions.SetMiddleName;
import io.sphere.sdk.customers.commands.updateactions.SetSalutation;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import io.sphere.sdk.customers.commands.updateactions.SetVatId;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildChangeEmailUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetCompanyNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetCustomerGroupUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetCustomerNumberUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetExternalIdUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetFirstNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetLastNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetLocaleUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetMiddleNameUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetSalutationUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetTitleUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetVatIdUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetDateOfBirthUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomerUpdateActionUtilsTest {

    private static Customer old;
    private static CustomerDraft newSame;
    private static CustomerDraft newDifferent;

    @BeforeEach
    void setup() {
        CustomerName oldCustomerName = CustomerName.of("old-title", "old-firstName",
            "old-middleName", "old-lastName");

        final String key = "key1";
        final String companyName = "companyName1";
        final String salutation = "salutation1";
        final String vatId = "vatId1";
        final String locale = "DE";
        final String birthDate = "1990-10-01";
        final String externalId = "externalId1";
        final String customerNumber = "1234";

        old = mock(Customer.class);
        when(old.getKey()).thenReturn(key);
        when(old.getName()).thenReturn(oldCustomerName);
        when(old.getEmail()).thenReturn("old-email");
        when(old.getFirstName()).thenReturn("old-firstName");
        when(old.getMiddleName()).thenReturn("old-middleName");
        when(old.getLastName()).thenReturn("old-lastName");
        when(old.getTitle()).thenReturn("old-title");
        when(old.getSalutation()).thenReturn(salutation);
        when(old.getCustomerNumber()).thenReturn(customerNumber);
        when(old.getExternalId()).thenReturn(externalId);
        when(old.getCompanyName()).thenReturn(companyName);
        when(old.getDateOfBirth()).thenReturn(LocalDate.parse(birthDate));
        when(old.getVatId()).thenReturn(vatId);
        when(old.getLocale()).thenReturn(Locale.forLanguageTag(locale));


        newSame = CustomerDraftBuilder.of(oldCustomerName, "old-email", "oldPW")
                                      .key(key)
                                      .companyName(companyName)
                                      .salutation(salutation)
                                      .dateOfBirth(LocalDate.parse(birthDate))
                                      .locale(Locale.forLanguageTag(locale))
                                      .vatId(vatId)
                                      .externalId(externalId)
                                      .customerNumber(customerNumber)
                                      .build();

        CustomerName newCustomerName = CustomerName.of("new-title", "new-firstName",
            "new-middleName", "new-lastName");

        newDifferent = CustomerDraftBuilder.of(newCustomerName, "new-email", "newPW").build();
    }

    @Test
    void buildChangeEmailUpdateAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildChangeEmailUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(ChangeEmail.class);
        assertThat(result).contains(ChangeEmail.of(newDifferent.getEmail()));
    }

    @Test
    void buildChangeEmailUpdateAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildChangeEmailUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetFirstNameUpdateAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetFirstNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetFirstName.class);
        assertThat(result).contains(SetFirstName.of(newDifferent.getFirstName()));
    }

    @Test
    void buildSetFirstNameUpdateAction_WithSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetFirstNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetLastNameUpdateAction_WithDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetLastNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetLastName.class);
        assertThat(result).contains(SetLastName.of(newDifferent.getLastName()));
    }

    @Test
    void buildSetLastNameUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetLastNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetMiddleNameUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetMiddleNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetMiddleName.class);
        assertThat(result).contains(SetMiddleName.of(newDifferent.getMiddleName()));
    }

    @Test
    void buildSetMiddleNameUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetMiddleNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetTitleUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetTitleUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetTitle.class);
        assertThat(result).contains(SetTitle.of(newDifferent.getTitle()));
    }

    @Test
    void buildSetTitleUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetTitleUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetSalutationUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetSalutationUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetSalutation.class);
        assertThat(result).contains(SetSalutation.of(newDifferent.getSalutation()));
    }

    @Test
    void buildSetSalutationUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetSalutationUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetCustomerNumberUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetCustomerNumberUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetCustomerNumber.class);
        assertThat(result).contains(SetCustomerNumber.of(newDifferent.getCustomerNumber()));
    }

    @Test
    void buildSetCustomerNumberUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetCustomerNumberUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetExternalIdUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetExternalIdUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetExternalId.class);
        assertThat(result).contains(SetExternalId.of(newDifferent.getExternalId()));
    }

    @Test
    void buildSetExternalIdUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetExternalIdUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetCompanyNameUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetCompanyNameUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetCompanyName.class);
        assertThat(result).contains(SetCompanyName.of(newDifferent.getCompanyName()));
    }

    @Test
    void buildSetCompanyNameUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetCompanyNameUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetDateOfBirthUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetDateOfBirthUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetDateOfBirth.class);
        assertThat(result).contains(SetDateOfBirth.of(newDifferent.getDateOfBirth()));
    }

    @Test
    void buildSetDateOfBirthUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetDateOfBirthUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetVatIdUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetVatIdUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetVatId.class);
        assertThat(result).contains(SetVatId.of(newDifferent.getVatId()));
    }

    @Test
    void buildSetVatIdUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetVatIdUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetLocaleUpdateAction_withDifferentValues_ShouldReturnAction() {
        final Optional<UpdateAction<Customer>> result = buildSetLocaleUpdateAction(old, newDifferent);

        assertThat(result).containsInstanceOf(SetLocale.class);
        assertThat(result).contains(SetLocale.of(newDifferent.getLocale()));
    }

    @Test
    void buildSetLocaleUpdateAction_withSameValues_ShouldReturnEmptyOptional() {
        final Optional<UpdateAction<Customer>> result = buildSetLocaleUpdateAction(old, newSame);

        assertThat(result).isEmpty();
    }

    @Test
    void buildSetCustomerGroupAction_WithSameReference_ShouldNotReturnAction() {
        final String customerGroupId = UUID.randomUUID().toString();
        final Reference<CustomerGroup> customerGroupReference =
            Reference.of(CustomerGroup.referenceTypeId(), customerGroupId);

        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .customerGroup(ResourceIdentifier.ofId(customerGroupId))
                                .build();

        final Optional<UpdateAction<Customer>> result = buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
        assertThat(result).isNotPresent();
    }

    @Test
    void buildSetCustomerGroupAction_WithDifferentReference_ShouldReturnAction() {
        final String customerGroupId = UUID.randomUUID().toString();
        final Reference<CustomerGroup> customerGroupReference =
            Reference.of(CustomerGroup.referenceTypeId(), customerGroupId);

        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

        final String resolvedCustomerGroupId = UUID.randomUUID().toString();
        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .customerGroup(ResourceIdentifier.ofId(resolvedCustomerGroupId))
                                .build();

        final Optional<UpdateAction<Customer>> result = buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
        assertThat(result).isPresent();
        assertThat(result).containsInstanceOf(SetCustomerGroup.class);
        assertThat(((SetCustomerGroup) result.get()).getCustomerGroup())
            .isEqualTo(Reference.of(CustomerGroup.referenceTypeId(), resolvedCustomerGroupId));
    }

    @Test
    void buildSetCustomerGroupAction_WithOnlyNewReference_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        final String newCustomerGroupId = UUID.randomUUID().toString();
        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .customerGroup(ResourceIdentifier.ofId(newCustomerGroupId))
                                .build();

        final Optional<UpdateAction<Customer>> result = buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
        assertThat(result).isPresent();
        assertThat(result).containsInstanceOf(SetCustomerGroup.class);
        assertThat(((SetCustomerGroup) result.get()).getCustomerGroup())
            .isEqualTo(Reference.of(CustomerGroup.referenceTypeId(), newCustomerGroupId));
    }

    @Test
    void buildSetCustomerGroupAction_WithoutNewReference_ShouldReturnUnsetAction() {
        final String customerGroupId = UUID.randomUUID().toString();
        final Reference<CustomerGroup> customerGroupReference =
            Reference.of(CustomerGroup.referenceTypeId(), customerGroupId);

        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getCustomerGroup()).thenReturn(customerGroupReference);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .build();

        final Optional<UpdateAction<Customer>> result = buildSetCustomerGroupUpdateAction(oldCustomer, newCustomer);
        assertThat(result).isPresent();
        assertThat(result).containsInstanceOf(SetCustomerGroup.class);
        //Note: If the old value is set, but the new one is empty - the command will unset the customer group.
        assertThat(((SetCustomerGroup) result.get()).getCustomerGroup()).isNull();
    }
}
