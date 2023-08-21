package com.commercetools.sync.integration.externalsource.customers;

import static com.commercetools.sync.sdk2.customers.utils.CustomerUpdateActionUtils.CUSTOMER_NUMBER_EXISTS_WARNING;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.common.AddressBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerAddAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerAddBillingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerAddShippingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerAddStoreActionBuilder;
import com.commercetools.api.models.customer.CustomerChangeEmailActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerRemoveAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveBillingAddressIdActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCompanyNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomFieldActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomerGroupActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDateOfBirthActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultBillingAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerSetFirstNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetLocaleActionBuilder;
import com.commercetools.api.models.customer.CustomerSetMiddleNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetSalutationActionBuilder;
import com.commercetools.api.models.customer.CustomerSetStoresActionBuilder;
import com.commercetools.api.models.customer.CustomerSetTitleActionBuilder;
import com.commercetools.api.models.customer.CustomerSetVatIdActionBuilder;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.store.Store;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.integration.commons.utils.CustomerITUtils;
import com.commercetools.sync.integration.commons.utils.ITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import com.commercetools.sync.sdk2.customers.CustomerSync;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptions;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.sdk2.customers.helpers.CustomerSyncStatistics;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerSyncIT {
  private CustomerDraft customerDraftJohnDoe;
  private Customer customerJohnDoe;

  private List<String> errorMessages;
  private List<String> warningMessages;
  private List<Throwable> exceptions;
  private List<CustomerUpdateAction> updateActionList;
  private CustomerSync customerSync;

  @BeforeEach
  void setup() {
    final ImmutablePair<Customer, CustomerDraft> sampleCustomerJohnDoe =
        CustomerITUtils.ensureSampleCustomerJohnDoe(TestClientUtils.CTP_TARGET_CLIENT);
    customerJohnDoe = sampleCustomerJohnDoe.getLeft();
    customerDraftJohnDoe = sampleCustomerJohnDoe.getRight();
    setUpCustomerSync();
  }

  @AfterEach
  void tearDown() {
    CustomerITUtils.deleteCustomers(TestClientUtils.CTP_TARGET_CLIENT);
  }

  private void setUpCustomerSync() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    warningMessages = new ArrayList<>();
    updateActionList = new ArrayList<>();

    CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningMessages.add(exception.getMessage()))
            .beforeUpdateCallback(
                (updateActions, customerDraft, customer) -> {
                  updateActionList.addAll(Objects.requireNonNull(updateActions));
                  return updateActions;
                })
            .build();
    customerSync = new CustomerSync(customerSyncOptions);
  }

  @Test
  void sync_WithSameCustomer_ShouldNotUpdateCustomer() {
    final CustomerDraft sameCustomerDraft = CustomerDraftBuilder.of(customerDraftJohnDoe).build();

    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(sameCustomerDraft)).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 customers were processed in total (0 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithNewCustomer_ShouldCreateCustomer() {
    // ensure the customer is not exist on the project
    CustomerITUtils.deleteCustomer(TestClientUtils.CTP_TARGET_CLIENT, "customer-key-john-doe-2");

    final CustomerDraft newCustomerDraft =
        CustomerDraftBuilder.of(customerDraftJohnDoe)
            .isEmailVerified(false)
            .email("john-2@example.com")
            .customerNumber("gold-2")
            .key("customer-key-john-doe-2")
            .build();

    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT).build();

    final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(newCustomerDraft)).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();
    assertThat(updateActionList).isEmpty();

    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 customers were processed in total (1 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdatedCustomer_ShouldUpdateCustomer() {
    final Store storeCologne =
        CustomerITUtils.ensureStore(TestClientUtils.CTP_TARGET_CLIENT, "store-cologne");
    final CustomerDraft updatedCustomerDraft =
        CustomerDraftBuilder.of(customerDraftJohnDoe)
            .customerNumber("gold-new") // from gold-1, but can not be changed.
            .email("john-new@example.com") // from john@example.com
            .stores(
                asList( // store-cologne is added, store-munich is removed
                    StoreResourceIdentifierBuilder.of().key(storeCologne.getKey()).build(),
                    StoreResourceIdentifierBuilder.of().key("store-hamburg").build(),
                    StoreResourceIdentifierBuilder.of().key("store-berlin").build()))
            .build();

    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(updatedCustomerDraft)).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages)
        .containsExactly(
            format(CUSTOMER_NUMBER_EXISTS_WARNING, updatedCustomerDraft.getKey(), "gold-1"));
    assertThat(exceptions).isEmpty();
    assertThat(updateActionList)
        .containsExactly(
            CustomerChangeEmailActionBuilder.of().email("john-new@example.com").build(),
            CustomerSetStoresActionBuilder.of()
                .stores(
                    asList(
                        StoreResourceIdentifierBuilder.of().key("store-cologne").build(),
                        StoreResourceIdentifierBuilder.of().key("store-hamburg").build(),
                        StoreResourceIdentifierBuilder.of().key("store-berlin").build()))
                .build());

    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 customers were processed in total (0 created, 1 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdatedAllFieldsOfCustomer_ShouldUpdateCustomerWithAllExpectedActions() {
    final Store storeCologne =
        CustomerITUtils.ensureStore(TestClientUtils.CTP_TARGET_CLIENT, "store-cologne");
    final CustomerGroup customerGroupSilverMember =
        CustomerITUtils.ensureCustomerGroup(
            TestClientUtils.CTP_TARGET_CLIENT, "silver members", "silver");

    final CustomerDraft updatedCustomerDraft =
        CustomerDraftBuilder.of()
            .email("jane@example.com")
            .password("54321")
            .customerNumber("gold-1") // can not be changed after it set.
            .key("customer-key-john-doe")
            .stores(
                asList(
                    StoreResourceIdentifierBuilder.of()
                        .key(storeCologne.getKey())
                        .build(), // new store
                    StoreResourceIdentifierBuilder.of().key("store-munich").build(),
                    StoreResourceIdentifierBuilder.of().key("store-hamburg").build(),
                    StoreResourceIdentifierBuilder.of().key("store-berlin").build()))
            .firstName("Jane")
            .lastName("Doe")
            .middleName("")
            .title("Miss")
            .salutation("")
            .dateOfBirth(LocalDate.now().minusYears(26))
            .companyName("Acme Corporation 2")
            .vatId("DE000000000")
            .isEmailVerified(true)
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of()
                    .key(customerGroupSilverMember.getKey())
                    .build())
            .addresses(
                asList( // address2 is removed, address4 is added
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("berlin")
                        .key("address1")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("munich")
                        .key("address3")
                        .build(),
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("cologne")
                        .key("address4")
                        .build()))
            .defaultBillingAddress(2) // 0 becomes 2 -> berlin to cologne.
            .billingAddresses(singletonList(2)) // 0, 1 becomes 2 -> berlin, hamburg to cologne.
            .defaultShippingAddress(1) // 2 becomes 1 -> munich to munich.
            .shippingAddresses(asList(0, 1)) // 2 become 0, 1 -> munich to berlin, munich.
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("customer-type-gold").build())
                    .fields(ITUtils.createCustomFieldsJsonMap())
                    .build())
            .locale(Locale.FRENCH.toLanguageTag())
            .build();

    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(updatedCustomerDraft)).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(warningMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    final Map<String, String> addressKeyToIdMap =
        customerJohnDoe.getAddresses().stream().collect(toMap(Address::getKey, Address::getId));

    assertThat(updateActionList)
        .containsExactly(
            CustomerChangeEmailActionBuilder.of().email("jane@example.com").build(),
            CustomerSetFirstNameActionBuilder.of().firstName("Jane").build(),
            CustomerSetMiddleNameActionBuilder.of().middleName("").build(),
            CustomerSetTitleActionBuilder.of().title("Miss").build(),
            CustomerSetSalutationActionBuilder.of().salutation("").build(),
            CustomerSetCustomerGroupActionBuilder.of()
                .customerGroup(
                    CustomerGroupResourceIdentifierBuilder.of()
                        .id(customerGroupSilverMember.getId())
                        .build())
                .build(),
            CustomerSetCompanyNameActionBuilder.of().companyName("Acme Corporation 2").build(),
            CustomerSetDateOfBirthActionBuilder.of()
                .dateOfBirth(LocalDate.now().minusYears(26))
                .build(),
            CustomerSetVatIdActionBuilder.of().vatId("DE000000000").build(),
            CustomerSetLocaleActionBuilder.of().locale(Locale.FRENCH.toLanguageTag()).build(),
            CustomerRemoveAddressActionBuilder.of()
                .addressId(addressKeyToIdMap.get("address2"))
                .build(),
            CustomerAddAddressActionBuilder.of()
                .address(
                    AddressBuilder.of()
                        .country(CountryCode.DE.name())
                        .city("cologne")
                        .key("address4")
                        .build())
                .build(),
            CustomerRemoveBillingAddressIdActionBuilder.of()
                .addressId(addressKeyToIdMap.get("address1"))
                .build(),
            CustomerSetDefaultBillingAddressActionBuilder.of().addressKey("address4").build(),
            CustomerAddShippingAddressIdActionBuilder.of().addressKey("address1").build(),
            CustomerAddBillingAddressIdActionBuilder.of().addressKey("address4").build(),
            CustomerSetCustomFieldActionBuilder.of()
                .name(ITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME)
                .value(JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red"))
                .build(),
            CustomerSetCustomFieldActionBuilder.of()
                .name(ITUtils.BOOLEAN_CUSTOM_FIELD_NAME)
                .value(JsonNodeFactory.instance.booleanNode(false))
                .build(),
            CustomerAddStoreActionBuilder.of()
                .store(StoreResourceIdentifierBuilder.of().key(storeCologne.getKey()).build())
                .build());

    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 customers were processed in total (0 created, 1 updated and 0 failed to sync).");
  }
}
