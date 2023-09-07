package com.commercetools.sync.customers.utils;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.common.AddressBuilder;
import com.commercetools.api.models.common.AddressDraft;
import com.commercetools.api.models.common.AddressDraftBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerAddAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerAddBillingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerAddShippingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerChangeAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerRemoveAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveBillingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveShippingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultBillingAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultShippingAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.neovisionaries.i18n.CountryCode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AddressUpdateActionUtilsTest {

  private Customer oldCustomer;

  @BeforeEach
  void setup() {
    oldCustomer = mock(Customer.class);
  }

  @Test
  void buildAllAddressUpdateActions_WithDifferentAddresses_ShouldReturnAddressAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id-1").build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));
    when(oldCustomer.getDefaultShippingAddressId()).thenReturn("address-id-2");
    when(oldCustomer.getDefaultBillingAddressId()).thenReturn("address-id-1");
    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final AddressDraft address3 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-3")
            .id("address-id-new-3")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address2, address3))
            .defaultShippingAddress(1)
            .defaultBillingAddress(0)
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAllAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-1").build(),
            CustomerChangeAddressActionBuilder.of()
                .addressId("address-id-2")
                .address(address2)
                .build(),
            CustomerAddAddressActionBuilder.of().address(address3).build(),
            CustomerSetDefaultShippingAddressActionBuilder.of().addressKey("address-key-3").build(),
            CustomerSetDefaultBillingAddressActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void buildAllAddressUpdateActions_WithSameAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .postalCode("123")
                        .id("address-id-new-1")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .building("no 1")
                        .id("address-id-new-2")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAllAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildAllAddressUpdateActions_withRemovedAddresses_ShouldFilterOutRemoveBillingAndShippingAddressIdActions() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .building("no 1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAllAddressUpdateActions(oldCustomer, newCustomer);
    // assertions
    assertThat(updateActions)
        .containsExactly(CustomerRemoveAddressActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveAddressUpdateActions_WithoutOldAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses()).thenReturn(emptyList());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveAddressUpdateActions_WithEmptyAddressKey_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .building("no 1")
                    .build()));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of().country(CountryCode.DE.toString()).key("").build()))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    // assertions
    assertThat(updateActions)
        .containsExactly(CustomerRemoveAddressActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveAddressUpdateActions_WithNullNewAddresses_ShouldReturnRemoveAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-1").build(),
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-2").build());
  }

  @Test
  void buildRemoveAddressUpdateActions_WithEmptyNewAddresses_ShouldReturnRemoveAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").addresses(emptyList()).build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-1").build(),
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-2").build());
  }

  @Test
  void buildRemoveAddressUpdateActions_WithNullAddresses_ShouldReturnRemoveAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(null, null))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-1").build(),
            CustomerRemoveAddressActionBuilder.of().addressId("address-id-2").build());
  }

  @Test
  void buildRemoveAddressUpdateActions_WithAddressesWithoutKeys_ShouldReturnRemoveAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id-2").build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(CustomerRemoveAddressActionBuilder.of().addressId("address-id-2").build());
  }

  @Test
  void buildChangeAddressUpdateActions_WithoutNewAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildChangeAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildChangeAddressUpdateActions_WithEmptyNewAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").addresses(emptyList()).build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildChangeAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  @Disabled("Address")
  void buildChangeAddressUpdateActions_WithSameAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .postalCode("123")
                        .id("address-id-new-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .building("no 1")
                        .id("address-id-new-2")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildChangeAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  @Disabled("Addresses")
  void
      buildChangeAddressUpdateActions_WithDifferentAddressesData_ShouldReturnChangeAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("321")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 2")
                    .build()));

    final AddressDraft address1 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .postalCode("123")
            .id("address-id-new-1")
            .build();

    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final AddressDraft address3 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-3")
            .building("no 1")
            .id("address-id-new-3")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address1, address2, address3))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildChangeAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerChangeAddressActionBuilder.of()
                .addressId("address-id-1")
                .address(address1)
                .build());
  }

  @Test
  void buildChangeAddressUpdateActions_WithNullAddresses_ShouldNotReturnChangeAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(singletonList(null))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildChangeAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildChangeAddressUpdateActions_WithAddressesWithoutKeys_ShouldNotReturnChangeAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .id("address-id-1")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildChangeAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddAddressUpdateActions_WithoutNewAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddAddressUpdateActions_WithEmptyNewAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").addresses(emptyList()).build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddAddressUpdateActions_WithSameAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .postalCode("123")
                        .id("address-id-new-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .building("no 1")
                        .id("address-id-new-2")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddAddressUpdateActions_WithNewAddresses_ShouldReturnAddAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));

    final AddressDraft address1 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .postalCode("123")
            .id("address-id-new-1")
            .build();

    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final AddressDraft address3 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-3")
            .building("no 1")
            .id("address-id-new-3")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address1, address2, address3))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(CustomerAddAddressActionBuilder.of().address(address3).build());
  }

  @Test
  void buildAddAddressUpdateActions_WithNullAddresses_ShouldNotReturnAddAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(null, null))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddAddressUpdateActions_WithAddressesWithoutKeys_ShouldNotReturnAddAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id-2").build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddAddressUpdateActions_WithEmptyAddressKey_ShouldNotReturnAction() {
    // preparation
    final Address address1 =
        AddressBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .building("no 1")
            .id("address-id-1")
            .build();
    when(oldCustomer.getAddresses()).thenReturn(Collections.singletonList(address1));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of().country(CountryCode.DE.toString()).key("").build()))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddAddressUpdateActions(oldCustomer, newCustomer);

    // assertions
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildSetDefaultShippingAddressUpdateAction_WithSameDefaultShippingAddress_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    when(oldCustomer.getDefaultShippingAddressId()).thenReturn("address-id-1");
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultShippingAddress(0)
            .build();

    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultShippingAddressUpdateAction(
            oldCustomer, newCustomer);

    assertThat(customerUpdateAction).isNotPresent();
  }

  @Test
  void
      buildSetDefaultShippingAddressUpdateAction_WithDifferentDefaultShippingAddress_ShouldReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getDefaultShippingAddressId()).thenReturn("address-id-1");
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultShippingAddress(1)
            .build();

    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultShippingAddressUpdateAction(
            oldCustomer, newCustomer);

    assertThat(customerUpdateAction)
        .isPresent()
        .contains(
            CustomerSetDefaultShippingAddressActionBuilder.of()
                .addressKey("address-key-2")
                .build());
  }

  @Test
  void
      buildSetDefaultShippingAddressUpdateAction_WithNoExistingShippingAddress_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses()).thenReturn(emptyList());
    when(oldCustomer.getDefaultShippingAddressId()).thenReturn(null);
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultShippingAddress(1)
            .build();
    // test
    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultShippingAddressUpdateAction(
            oldCustomer, newCustomer);

    // assertions
    assertThat(customerUpdateAction)
        .isPresent()
        .contains(
            CustomerSetDefaultShippingAddressActionBuilder.of()
                .addressKey("address-key-2")
                .build());
  }

  @Test
  void
      buildSetDefaultShippingAddressUpdateAction_WithoutDefaultShippingAddress_ShouldReturnUnsetAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getDefaultShippingAddressId()).thenReturn("address-id-1");
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultShippingAddress(null)
            .build();

    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultShippingAddressUpdateAction(
            oldCustomer, newCustomer);

    assertThat(customerUpdateAction)
        .isPresent()
        .contains(CustomerSetDefaultShippingAddressActionBuilder.of().addressKey(null).build());
  }

  @Test
  void
      buildSetDefaultBillingAddressUpdateAction_WithSameDefaultBillingAddress_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getDefaultBillingAddressId()).thenReturn("address-id-1");
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultBillingAddress(0)
            .build();

    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultBillingAddressUpdateAction(
            oldCustomer, newCustomer);

    assertThat(customerUpdateAction).isNotPresent();
  }

  @Test
  void
      buildSetDefaultBillingAddressUpdateAction_WithDifferentDefaultBillingAddress_ShouldReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getDefaultBillingAddressId()).thenReturn("address-id-1");
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultBillingAddress(1)
            .build();

    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultBillingAddressUpdateAction(
            oldCustomer, newCustomer);

    assertThat(customerUpdateAction)
        .isPresent()
        .contains(
            CustomerSetDefaultBillingAddressActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void
      buildSetDefaultBillingAddressUpdateAction_WithoutDefaultBillingAddress_ShouldReturnUnsetAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));

    when(oldCustomer.getDefaultBillingAddressId()).thenReturn("address-id-1");

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultBillingAddress(null)
            .build();

    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultBillingAddressUpdateAction(
            oldCustomer, newCustomer);

    assertThat(customerUpdateAction)
        .isPresent()
        .contains(CustomerSetDefaultBillingAddressActionBuilder.of().addressKey(null).build());
  }

  @Test
  void buildSetDefaultBillingAddressUpdateAction_WithNoExistingBillingAddress_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses()).thenReturn(emptyList());
    when(oldCustomer.getDefaultBillingAddressId()).thenReturn(null);
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                asList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build(),
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-2")
                        .id("address-id-2")
                        .build()))
            .defaultBillingAddress(1)
            .build();
    // test
    final Optional<CustomerUpdateAction> customerUpdateAction =
        CustomerUpdateActionUtils.buildSetDefaultBillingAddressUpdateAction(
            oldCustomer, newCustomer);

    // assertions
    assertThat(customerUpdateAction)
        .isPresent()
        .contains(
            CustomerSetDefaultBillingAddressActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void buildAddShippingAddressUpdateActions_WithoutNewShippingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddShippingAddressUpdateActions_WithEmptyShippingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .shippingAddresses(emptyList())
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddShippingAddressUpdateActions_WithSameShippingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(0))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddShippingAddressUpdateActions_WithEmptyAddressKey_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final AddressDraft address1 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .postalCode("123")
            .id("address-id-new-1")
            .build();

    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address1, address2))
            .shippingAddresses(singletonList(1))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

    // assertions
    assertThat(updateActions)
        .containsExactly(
            CustomerAddShippingAddressIdActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void
      buildAddShippingAddressUpdateActions_WithNewShippingAddresses_ShouldReturnAddShippingAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));

    final AddressDraft address1 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .postalCode("123")
            .id("address-id-new-1")
            .build();

    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address1, address2))
            .shippingAddresses(asList(0, 1))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerAddShippingAddressIdActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void
      buildAddShippingAddressUpdateActions_WithShippingAddressIdLessThanZero_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(-1))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Addresses list does not contain an address at the index: %s", -1));
  }

  @Test
  void
      buildAddShippingAddressUpdateActions_InOutBoundOfTheExistingIndexes_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(2))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Addresses list does not contain an address at the index: %s", 2));
  }

  @Test
  void
      buildAddShippingAddressUpdateActions_WithAddressListSizeNull_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .shippingAddresses(singletonList(0))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Addresses list does not contain an address at the index: %s", 0));
  }

  @Test
  void
      buildAddShippingAddressUpdateActions_WithIndexBiggerThanListSize_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(3))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Addresses list does not contain an address at the index: %s", 3));
  }

  @Test
  void
      buildAddShippingAddressUpdateActions_InWithNullAddress_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(singletonList(null))
            .shippingAddresses(singletonList(0))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Address is null at the index: %s of the addresses list.", 0));
  }

  @Test
  void buildAddShippingAddressUpdateActions_InWithBlankKey_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(0))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddShippingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            format("Address does not have a key at the index: %s of the addresses list.", 0));
  }

  @Test
  void
      buildRemoveShippingAddressUpdateActions_WithEmptyOldShippingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));

    when(oldCustomer.getShippingAddressIds()).thenReturn(emptyList());
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveShippingAddressUpdateActions_WithBlankOldKey_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key(" ")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    // assertions
    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveShippingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveShippingAddressUpdateActions_WitNullOldShippingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveShippingAddressUpdateActions_WithEmptyNewShippingAddresses_ShouldReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(emptyList())
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveShippingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveShippingAddressUpdateActions_WithNullNewShippingAddresses_ShouldReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses((Integer) null)
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveShippingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveShippingAddressUpdateActions_WithSameShippingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(0))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveShippingAddressUpdateActions_WithOldShippingAddresses_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(singletonList("address-id-1"));

    final AddressDraft address3 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-3")
            .id("address-id-3")
            .build();

    final AddressDraft address4 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-4")
            .id("address-id-new-4")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address3, address4))
            .shippingAddresses(singletonList(1))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);
    // assertions
    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveShippingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void
      buildRemoveShippingAddressUpdateActions_WithLessShippingAddresses_ShouldReturnRemoveShippingAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getShippingAddressIds()).thenReturn(asList("address-id-1", "address-id-2"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .shippingAddresses(singletonList(0))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveShippingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveShippingAddressIdActionBuilder.of().addressId("address-id-2").build());
  }

  @Test
  void buildAddBillingAddressUpdateActions_WithoutNewBillingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));

    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddBillingAddressUpdateActions_WithEmptyBillingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .billingAddresses(emptyList())
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAddBillingAddressUpdateActions_WithSameBillingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses(singletonList(0))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildAddBillingAddressUpdateActions_WithNewBillingAddresses_ShouldReturnAddBillingAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final AddressDraft address1 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .postalCode("123")
            .id("address-id-new-1")
            .build();

    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address1, address2))
            .billingAddresses(asList(0, 1))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerAddBillingAddressIdActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void buildAddBillingAddressUpdateActions_WithEmptyAddressKey_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("")
                    .id("address-id-1")
                    .postalCode("123")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("")
                    .id("address-id-2")
                    .building("no 1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final AddressDraft address1 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-1")
            .postalCode("123")
            .id("address-id-new-1")
            .build();

    final AddressDraft address2 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-2")
            .building("no 2")
            .id("address-id-new-2")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address1, address2))
            .billingAddresses(singletonList(1))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(oldCustomer, newCustomer);

    // assertions
    assertThat(updateActions)
        .containsExactly(
            CustomerAddBillingAddressIdActionBuilder.of().addressKey("address-key-2").build());
  }

  @Test
  void
      buildAddBillingAddressUpdateActions_WithShippingAddressIdLessThanZero_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses(singletonList(-1))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Addresses list does not contain an address at the index: %s", -1));
  }

  @Test
  void
      buildAddBillingAddressUpdateActions_InOutBoundOfTheExistingIndexes_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses(singletonList(2))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Addresses list does not contain an address at the index: %s", 2));
  }

  @Test
  void buildAddBillingAddressUpdateActions_InWithNullAddress_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(singletonList(null))
            .billingAddresses(singletonList(0))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(format("Address is null at the index: %s of the addresses list.", 0));
  }

  @Test
  void buildAddBillingAddressUpdateActions_InWithBlankKey_ShouldThrowIllegalArgumentException() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressBuilder.of()
                        .country(CountryCode.DE.toString())
                        .id("address-id-1")
                        .build()))
            .billingAddresses(singletonList(0))
            .build();

    assertThatThrownBy(
            () ->
                CustomerUpdateActionUtils.buildAddBillingAddressUpdateActions(
                    oldCustomer, newCustomer))
        .isExactlyInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            format("Address does not have a key at the index: %s of the addresses list.", 0));
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WithOldBillingAddresses_ShouldReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));

    final AddressDraft address3 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-3")
            .id("address-id-3")
            .build();

    final AddressDraft address4 =
        AddressDraftBuilder.of()
            .country(CountryCode.DE.toString())
            .key("address-key-4")
            .id("address-id-new-4")
            .build();

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(asList(address3, address4))
            .billingAddresses(singletonList(1))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);
    // assertions
    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveBillingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WithEmptyOldBillingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(emptyList());

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WitNullOldBillingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of().email("email").password("pass").build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WithEmptyNewBillingAddresses_ShouldReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses(emptyList())
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveBillingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WithNullNewBillingAddresses_ShouldReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses((Integer) null)
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveBillingAddressIdActionBuilder.of().addressId("address-id-1").build());
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WithSameBillingAddresses_ShouldNotReturnAction() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(singletonList("address-id-1"));
    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses(singletonList(0))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildRemoveBillingAddressUpdateActions_WithEmptyOldKey_ShouldNotReturnAction() {
    // preparation
    when(oldCustomer.getAddresses())
        .thenReturn(
            singletonList(
                AddressBuilder.of().country(CountryCode.DE.toString()).id("address-id-1").build()));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .build();
    // test
    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    // assertions
    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildRemoveBillingAddressUpdateActions_WithLessBillingAddresses_ShouldReturnRemoveBillingAddressActions() {

    when(oldCustomer.getAddresses())
        .thenReturn(
            asList(
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-1")
                    .id("address-id-1")
                    .build(),
                AddressBuilder.of()
                    .country(CountryCode.DE.toString())
                    .key("address-key-2")
                    .id("address-id-2")
                    .build()));
    when(oldCustomer.getBillingAddressIds()).thenReturn(asList("address-id-1", "address-id-2"));

    final CustomerDraft newCustomer =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .addresses(
                singletonList(
                    AddressDraftBuilder.of()
                        .country(CountryCode.DE.toString())
                        .key("address-key-1")
                        .id("address-id-1")
                        .build()))
            .billingAddresses(singletonList(0))
            .build();

    final List<CustomerUpdateAction> updateActions =
        CustomerUpdateActionUtils.buildRemoveBillingAddressUpdateActions(oldCustomer, newCustomer);

    assertThat(updateActions)
        .containsExactly(
            CustomerRemoveBillingAddressIdActionBuilder.of().addressId("address-id-2").build());
  }
}
