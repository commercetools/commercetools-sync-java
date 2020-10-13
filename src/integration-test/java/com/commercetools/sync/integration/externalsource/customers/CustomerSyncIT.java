package com.commercetools.sync.integration.externalsource.customers;

import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.createCustomerGroup;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.deleteCustomerGroups;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomer;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomerCustomType;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomers;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.deleteStores;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerSyncIT {

    private Store storeBerlin;
    private Store storeHamburg;
    private Store storeMunich;
    private Type customTypeGoldMember;
    private CustomerGroup customerGroupGoldMember;

    private CustomerDraft customerDraftJohnDoe;
    private Customer customerJohnDoe;

    @BeforeEach
    void setup() {
        deleteCustomersAndTheirReferences();

        storeBerlin = createStore(CTP_TARGET_CLIENT, "store-berlin");
        storeHamburg = createStore(CTP_TARGET_CLIENT, "store-hamburg");
        storeMunich = createStore(CTP_TARGET_CLIENT, "store-munich");

        customTypeGoldMember =
            createCustomerCustomType("customer-type-gold", Locale.ENGLISH, "gold customers", CTP_TARGET_CLIENT);

        customerGroupGoldMember =
            createCustomerGroup(CTP_TARGET_CLIENT, "gold members", "gold");

        customerDraftJohnDoe =
            CustomerDraftBuilder
                .of("john@example.com", "12345")
                .customerNumber("gold-1")
                .key("customer-key")
                .stores(asList(
                    ResourceIdentifier.ofKey(storeBerlin.getKey()),
                    ResourceIdentifier.ofKey(storeHamburg.getKey()),
                    ResourceIdentifier.ofKey(storeMunich.getKey())))
                .firstName("John")
                .lastName("Doe")
                .middleName("Jr")
                .title("Mr")
                .salutation("Dear")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .companyName("Acme Corporation")
                .vatId("DE999999999")
                .emailVerified(true)
                .customerGroup(ResourceIdentifier.ofKey(customerGroupGoldMember.getKey()))
                .addresses(asList(
                    Address.of(CountryCode.DE).withCity("berlin").withKey("address1"),
                    Address.of(CountryCode.DE).withCity("hamburg").withKey("address2"),
                    Address.of(CountryCode.DE).withCity("munich").withKey("address3")))
                .defaultBillingAddress(0)
                .billingAddresses(asList(0, 1))
                .defaultShippingAddress(2)
                .shippingAddresses(singletonList(2))
                .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeGoldMember.getKey(), emptyMap()))
                .locale(Locale.ENGLISH)
                .build();

        customerJohnDoe = createCustomer(CTP_TARGET_CLIENT, customerDraftJohnDoe);
    }

    @AfterAll
    static void tearDown() {
        deleteCustomersAndTheirReferences();
    }

    static void deleteCustomersAndTheirReferences() {
        deleteCustomers(CTP_TARGET_CLIENT);
        deleteTypes(CTP_TARGET_CLIENT);
        deleteStores(CTP_TARGET_CLIENT);
        deleteCustomerGroups(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithSameCustomer_ShouldNotUpdateCustomer() {
        final CustomerDraft sameCustomerDraft =
            CustomerDraftBuilder.of(customerDraftJohnDoe)
                                .build();
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(sameCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 0, 0, 0);
    }

    @Test
    void sync_WithNewCustomer_ShouldCreateCustomer() {
        final CustomerDraft newCustomerDraft =
            CustomerDraftBuilder.of(customerDraftJohnDoe)
                                .emailVerified(false)
                                .email("jane@example.com")
                                .customerNumber("customer-2")
                                .firstName("Jane")
                                .lastName("Doe")
                                .title("Miss")
                                .key("new-customer-key")
                                .build();

        //todo: update custom type values

        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(newCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 1, 0, 0);
    }

    @Test
    void sync_WithUpdatedCustomer_ShouldUpdateCustomer() {
        final CustomerDraft updatedCustomerDraft =
            CustomerDraftBuilder.of(customerDraftJohnDoe)
                                .email("JOhn@example.com")
                                .build();

        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(updatedCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 0, 1, 0);
    }

    @Disabled
    @Test
    void sync_WithUpdatedAllFieldsOfCustomer_ShouldUpdateCustomerWithAllExpectedActions() {
        final Store storeCologne = createStore(CTP_TARGET_CLIENT, "store-cologne");
        final Type customTypeSilverMember = createCustomerCustomType("customer-type-silver",
            Locale.ENGLISH, "silver customers", CTP_TARGET_CLIENT);
        final CustomerGroup customerGroupSilverMember =
            createCustomerGroup(CTP_TARGET_CLIENT, "silver members", "silver");

        // todo 1: email update action should check case insensitivity
        //  https://docs.commercetools.com/api/projects/customers#email-case-insensitivity

        // todo 2: customerNumber - String - Optional
        // It should be unique across a project. Once it's set, it cannot be changed.

        // todo 3: email has some link with the stores, if the store is removed and email changed, the email in
        // removed store causes the error below:
        // https://docs.commercetools.com/api/projects/customers#email-uniqueness

        // also the action below fails because store changes.
        final CustomerDraft updatedCustomerDraft =
            CustomerDraftBuilder
                .of("jane@example.com", "54321")
                .customerNumber("gold-1")
                .key("customer-key")
                .stores(asList(
                    ResourceIdentifier.ofKey(storeCologne.getKey()),
                    ResourceIdentifier.ofKey(storeHamburg.getKey()),
                    ResourceIdentifier.ofKey(storeBerlin.getKey()),
                    ResourceIdentifier.ofKey(storeMunich.getKey())))
                .firstName("Jane")
                .lastName("Doe")
                .middleName("")
                .title("Miss")
                .salutation("")
                .dateOfBirth(LocalDate.now().minusYears(26))
                .companyName("Acme Corporation 2")
                .vatId("DE000000000")
                .emailVerified(true)
                .customerGroup(ResourceIdentifier.ofKey(customerGroupSilverMember.getKey()))
                .addresses(asList(
                    Address.of(CountryCode.DE).withCity("berlin").withKey("address1"),
                    Address.of(CountryCode.DE).withCity("munich").withKey("address3"),
                    Address.of(CountryCode.DE).withCity("cologne").withKey("address4")))
                .defaultBillingAddress(2)
                .billingAddresses(singletonList(2))
                .defaultShippingAddress(1)
                .shippingAddresses(asList(0, 1))
                .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeSilverMember.getKey(), emptyMap()))
                .locale(Locale.FRENCH)
                .build();

        final CustomerDraft newCustomerDraft =
            CustomerDraftBuilder.of(customerDraftJohnDoe)
                                .email("jane@example.com")
                                .build();


        final List<UpdateAction<Customer>> updateActionList = new ArrayList<>();
        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .beforeUpdateCallback((updateActions, customerDraft, customer) -> {
                updateActionList.addAll(Objects.requireNonNull(updateActions));
                return updateActions;
            })
            .build();

        final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(updatedCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 0, 1, 0);
        assertThat(updateActionList).containsExactly();
    }
}
