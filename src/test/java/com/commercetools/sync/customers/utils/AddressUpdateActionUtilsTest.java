package com.commercetools.sync.customers.utils;

import com.commercetools.sync.customers.commands.updateactions.AddBillingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.AddShippingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultBillingAddressWitKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultShippingAddressWitKey;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.ChangeAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveBillingAddressId;
import io.sphere.sdk.customers.commands.updateactions.RemoveShippingAddressId;
import io.sphere.sdk.models.Address;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildAddAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildAllAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildChangeAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildRemoveAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetDefaultBillingAddressUpdateAction;
import static com.commercetools.sync.customers.utils.CustomerUpdateActionUtils.buildSetDefaultShippingAddressUpdateAction;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddressUpdateActionUtilsTest {

    @Test
    void buildAllAddressUpdateActions_WithDifferentAddresses_ShouldReturnAddressAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));
        when(oldCustomer.getDefaultShippingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2"));
        when(oldCustomer.getDefaultBillingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final Address address2 = Address.of(CountryCode.DE)
                                        .withKey("address-key-2")
                                        .withBuilding("no 2")
                                        .withId("address-id-new-2");

        final Address address3 = Address.of(CountryCode.DE)
                                        .withKey("address-key-3")
                                        .withId("address-id-new-3");

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(address2, address3))
                                .defaultShippingAddress(1)
                                .defaultBillingAddress(0)
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAllAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(
            RemoveAddress.of("address-id-1"),
            ChangeAddress.of("address-id-2", address2),
            AddAddress.of(address3),
            SetDefaultShippingAddressWitKey.of("address-key-3"),
            SetDefaultBillingAddressWitKey.of("address-key-2"));
    }

    @Test
    void buildAllAddressUpdateActions_WithSameAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("123"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1")
                                           .withPostalCode("123")
                                           .withId("address-id-new-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2")
                                           .withBuilding("no 1")
                                           .withId("address-id-new-2")
                                ))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAllAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveAddressUpdateActions_WithoutOldAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(emptyList());

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(singletonList(Address.of(CountryCode.DE).withKey("address-key-1")))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveAddressUpdateActions_WithNullNewAddresses_ShouldReturnRemoveAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(null)
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(
            RemoveAddress.of("address-id-1"),
            RemoveAddress.of("address-id-2"));
    }

    @Test
    void buildRemoveAddressUpdateActions_WithEmptyNewAddresses_ShouldReturnRemoveAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(emptyList())
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(
            RemoveAddress.of("address-id-1"),
            RemoveAddress.of("address-id-2"));
    }

    @Test
    void buildRemoveAddressUpdateActions_WithNullAddresses_ShouldReturnRemoveAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(null, null))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(
            RemoveAddress.of("address-id-1"), RemoveAddress.of("address-id-2"));
    }

    @Test
    void buildRemoveAddressUpdateActions_WithAddressesWithoutKeys_ShouldReturnRemoveAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(singletonList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveAddress.of("address-id-2"));
    }

    @Test
    void buildChangeAddressUpdateActions_WithoutNewAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(null)
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildChangeAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeAddressUpdateActions_WithEmptyNewAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(emptyList())
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildChangeAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeAddressUpdateActions_WithSameAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("123"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1")
                                           .withPostalCode("123")
                                           .withId("address-id-new-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2")
                                           .withBuilding("no 1")
                                           .withId("address-id-new-2")
                                ))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildChangeAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeAddressUpdateActions_WithDifferentAddressesData_ShouldReturnChangeAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("321"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 2")
        ));

        final Address address1 = Address.of(CountryCode.DE)
                                        .withKey("address-key-1")
                                        .withPostalCode("123")
                                        .withId("address-id-new-1");

        final Address address2 = Address.of(CountryCode.DE)
                                        .withKey("address-key-2")
                                        .withBuilding("no 2")
                                        .withId("address-id-new-2");

        final Address address3 = Address.of(CountryCode.DE)
                                       .withKey("address-key-3")
                                       .withBuilding("no 1")
                                       .withId("address-id-new-3");

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(address1, address2, address3))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildChangeAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(ChangeAddress.of("address-id-1", address1));
    }

    @Test
    void buildChangeAddressUpdateActions_WithNullAddresses_ShouldNotReturnChangeAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(singletonList(null))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildChangeAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildChangeAddressUpdateActions_WithAddressesWithoutKeys_ShouldNotReturnChangeAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(singletonList(
                                    Address.of(CountryCode.DE).withId("address-id-1")))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildChangeAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddAddressUpdateActions_WithoutNewAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(null)
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddAddressUpdateActions_WithEmptyNewAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(emptyList())
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddAddressUpdateActions_WithSameAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("123"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1")
                                           .withPostalCode("123")
                                           .withId("address-id-new-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2")
                                           .withBuilding("no 1")
                                           .withId("address-id-new-2")
                                ))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddAddressUpdateActions_WithNewAddresses_ShouldReturnAddAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("123"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));

        final Address address1 = Address.of(CountryCode.DE)
                                        .withKey("address-key-1")
                                        .withPostalCode("123")
                                        .withId("address-id-new-1");

        final Address address2 = Address.of(CountryCode.DE)
                                        .withKey("address-key-2")
                                        .withBuilding("no 2")
                                        .withId("address-id-new-2");

        final Address address3 = Address.of(CountryCode.DE)
                                        .withKey("address-key-3")
                                        .withBuilding("no 1")
                                        .withId("address-id-new-3");

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(address1, address2, address3))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(AddAddress.of(address3));
    }

    @Test
    void buildAddAddressUpdateActions_WithNullAddresses_ShouldNotReturnAddAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(null, null))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddAddressUpdateActions_WithAddressesWithoutKeys_ShouldNotReturnAddAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withId("address-id-2")
        ));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(singletonList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildSetDefaultShippingAddressUpdateAction_WithSameDefaultShippingAddress_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getDefaultShippingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
                                ))
                                .defaultShippingAddress(0)
                                .build();

        final Optional<UpdateAction<Customer>> customerUpdateAction =
            buildSetDefaultShippingAddressUpdateAction(oldCustomer, newCustomer);

        assertThat(customerUpdateAction).isNotPresent();
    }

    @Test
    void buildSetDefaultShippingAddressUpdateAction_WithDifferentDefaultShippingAddress_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getDefaultShippingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
                                ))
                                .defaultShippingAddress(1)
                                .build();

        final Optional<UpdateAction<Customer>> customerUpdateAction =
            buildSetDefaultShippingAddressUpdateAction(oldCustomer, newCustomer);

        assertThat(customerUpdateAction)
            .isPresent()
            .contains(SetDefaultShippingAddressWitKey.of("address-key-2"));
    }

    @Test
    void buildSetDefaultShippingAddressUpdateAction_WithoutDefaultShippingAddress_ShouldReturnUnsetAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getDefaultShippingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
                                ))
                                .defaultShippingAddress(null)
                                .build();


        final Optional<UpdateAction<Customer>> customerUpdateAction =
            buildSetDefaultShippingAddressUpdateAction(oldCustomer, newCustomer);

        assertThat(customerUpdateAction)
            .isPresent()
            .contains(SetDefaultShippingAddressWitKey.of(null));
    }

    @Test
    void buildSetDefaultBillingAddressUpdateAction_WithSameDefaultBillingAddress_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getDefaultBillingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
                                ))
                                .defaultBillingAddress(0)
                                .build();

        final Optional<UpdateAction<Customer>> customerUpdateAction =
            buildSetDefaultBillingAddressUpdateAction(oldCustomer, newCustomer);

        assertThat(customerUpdateAction).isNotPresent();
    }

    @Test
    void buildSetDefaultBillingAddressUpdateAction_WithDifferentDefaultBillingAddress_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getDefaultBillingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
                                ))
                                .defaultBillingAddress(1)
                                .build();

        final Optional<UpdateAction<Customer>> customerUpdateAction =
            buildSetDefaultBillingAddressUpdateAction(oldCustomer, newCustomer);

        assertThat(customerUpdateAction)
            .isPresent()
            .contains(SetDefaultBillingAddressWitKey.of("address-key-2"));
    }

    @Test
    void buildSetDefaultBillingAddressUpdateAction_WithoutDefaultBillingAddress_ShouldReturnUnsetAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getDefaultBillingAddress())
            .thenReturn(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(
                                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                                    Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
                                ))
                                .defaultBillingAddress(null)
                                .build();


        final Optional<UpdateAction<Customer>> customerUpdateAction =
            buildSetDefaultBillingAddressUpdateAction(oldCustomer, newCustomer);

        assertThat(customerUpdateAction)
            .isPresent()
            .contains(SetDefaultBillingAddressWitKey.of(null));
    }

    @Test
    void buildAddShippingAddressUpdateActions_WithoutNewShippingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .shippingAddresses(null)
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddShippingAddressUpdateActions_WithEmptyShippingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .shippingAddresses(emptyList())
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddShippingAddressUpdateActions_WithSameShippingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(singletonList(0))
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddShippingAddressUpdateActions_WithNewShippingAddresses_ShouldReturnAddShippingAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("123"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final Address address1 = Address.of(CountryCode.DE)
                                        .withKey("address-key-1")
                                        .withPostalCode("123")
                                        .withId("address-id-new-1");

        final Address address2 = Address.of(CountryCode.DE)
                                        .withKey("address-key-2")
                                        .withBuilding("no 2")
                                        .withId("address-id-new-2");

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(address1, address2))
                                .shippingAddresses(asList(0, 1))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(AddShippingAddressIdWithKey.of("address-key-2"));
    }

    @Test
    void buildAddShippingAddressUpdateActions_WithShippingAddressIdLessThanZero_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(singletonList(-1))
                .build();

       assertThatThrownBy(() -> buildAddShippingAddressUpdateActions(oldCustomer, newCustomer))
           .isExactlyInstanceOf(IllegalArgumentException.class)
           .hasMessage(format("Addresses list does not contain an address at the index: %s", -1));
    }

    @Test
    void buildAddShippingAddressUpdateActions_InOutBoundOfTheExistingIndexes_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(singletonList(2))
                .build();

        assertThatThrownBy(() -> buildAddShippingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Addresses list does not contain an address at the index: %s", 2));
    }

    @Test
    void buildAddShippingAddressUpdateActions_InWithNullAddress_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(null))
                .shippingAddresses(singletonList(0))
                .build();

        assertThatThrownBy(() -> buildAddShippingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Address is null at the index: %s of the addresses list.", 0));
    }

    @Test
    void buildAddShippingAddressUpdateActions_InWithBlankKey_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withId("address-id-1")
                ))
                .shippingAddresses(singletonList(0))
                .build();

        assertThatThrownBy(() -> buildAddShippingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Address does not have a key at the index: %s of the addresses list.", 0));
    }

    @Test
    void buildRemoveShippingAddressUpdateActions_WithEmptyOldShippingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses()).thenReturn(emptyList());

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveShippingAddressUpdateActions_WitNullOldShippingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses()).thenReturn(null);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveShippingAddressUpdateActions_WithEmptyNewShippingAddresses_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(emptyList())
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveShippingAddressId.of("address-id-1"));
    }

    @Test
    void buildRemoveShippingAddressUpdateActions_WithNullNewShippingAddresses_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(null)
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveShippingAddressId.of("address-id-1"));
    }

    @Test
    void buildRemoveShippingAddressUpdateActions_WithSameShippingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(singletonList(0))
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveShippingAddressUpdateActions_WithLessShippingAddresses_ShouldReturnRemoveShippingAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getShippingAddresses())
            .thenReturn(asList(
                Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .shippingAddresses(singletonList(0))
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveShippingAddressId.of("address-id-2"));
    }

    @Test
    void buildAddBillingAddressUpdateActions_WithoutNewBillingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .billingAddresses(null)
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddBillingAddressUpdateActions_WithEmptyBillingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .billingAddresses(emptyList())
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddBillingAddressUpdateActions_WithSameBillingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(singletonList(0))
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAddBillingAddressUpdateActions_WithNewBillingAddresses_ShouldReturnAddBillingAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1").withPostalCode("123"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2").withBuilding("no 1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final Address address1 = Address.of(CountryCode.DE)
                                        .withKey("address-key-1")
                                        .withPostalCode("123")
                                        .withId("address-id-new-1");

        final Address address2 = Address.of(CountryCode.DE)
                                        .withKey("address-key-2")
                                        .withBuilding("no 2")
                                        .withId("address-id-new-2");

        final CustomerDraft newCustomer =
            CustomerDraftBuilder.of("email", "pass")
                                .addresses(asList(address1, address2))
                                .billingAddresses(asList(0, 1))
                                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(AddBillingAddressIdWithKey.of("address-key-2"));
    }

    @Test
    void buildAddBillingAddressUpdateActions_WithShippingAddressIdLessThanZero_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(singletonList(-1))
                .build();

        assertThatThrownBy(() -> buildAddBillingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Addresses list does not contain an address at the index: %s", -1));
    }

    @Test
    void buildAddBillingAddressUpdateActions_InOutBoundOfTheExistingIndexes_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(singletonList(2))
                .build();

        assertThatThrownBy(() -> buildAddBillingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Addresses list does not contain an address at the index: %s", 2));
    }

    @Test
    void buildAddBillingAddressUpdateActions_InWithNullAddress_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(null))
                .billingAddresses(singletonList(0))
                .build();

        assertThatThrownBy(() -> buildAddBillingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Address is null at the index: %s of the addresses list.", 0));
    }

    @Test
    void buildAddBillingAddressUpdateActions_InWithBlankKey_ShouldThrowIllegalArgumentException() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withId("address-id-1")
                ))
                .billingAddresses(singletonList(0))
                .build();

        assertThatThrownBy(() -> buildAddBillingAddressUpdateActions(oldCustomer, newCustomer))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage(format("Address does not have a key at the index: %s of the addresses list.", 0));
    }

    @Test
    void buildRemoveBillingAddressUpdateActions_WithEmptyOldBillingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses()).thenReturn(emptyList());

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveBillingAddressUpdateActions_WitNullOldBillingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses()).thenReturn(null);

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveBillingAddressUpdateActions_WithEmptyNewBillingAddresses_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(emptyList())
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveBillingAddressId.of("address-id-1"));
    }

    @Test
    void buildRemoveBillingAddressUpdateActions_WithNullNewBillingAddresses_ShouldReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(null)
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveBillingAddressId.of("address-id-1"));
    }

    @Test
    void buildRemoveBillingAddressUpdateActions_WithSameBillingAddresses_ShouldNotReturnAction() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(singletonList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(singletonList(Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(singletonList(0))
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildRemoveBillingAddressUpdateActions_WithLessBillingAddresses_ShouldReturnRemoveBillingAddressActions() {
        final Customer oldCustomer = mock(Customer.class);
        when(oldCustomer.getAddresses()).thenReturn(asList(
            Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
            Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")
        ));
        when(oldCustomer.getBillingAddresses())
            .thenReturn(asList(
                Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1"),
                Address.of(CountryCode.DE).withKey("address-key-2").withId("address-id-2")));

        final CustomerDraft newCustomer =
            CustomerDraftBuilder
                .of("email", "pass")
                .addresses(singletonList(
                    Address.of(CountryCode.DE).withKey("address-key-1").withId("address-id-1")
                ))
                .billingAddresses(singletonList(0))
                .build();

        final List<UpdateAction<Customer>> updateActions =
            buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

        assertThat(updateActions).containsExactly(RemoveBillingAddressId.of("address-id-2"));
    }
}
