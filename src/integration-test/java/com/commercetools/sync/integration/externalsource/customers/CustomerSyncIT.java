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
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createCustomerCustomType;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJohnDoe;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomerSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerSyncIT {
    private CustomerDraft customerDraftJohnDoe;

    private List<String> errorMessages;
    private List<Throwable> exceptions;
    private CustomerSync customerSync;

    @BeforeEach
    void setup() {
        deleteCustomerSyncTestData(CTP_TARGET_CLIENT);
        customerDraftJohnDoe = createSampleCustomerJohnDoe(CTP_TARGET_CLIENT);
        setUpCustomerSync();
    }

    private void setUpCustomerSync() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();
        customerSync = new CustomerSync(customerSyncOptions);
    }

    @AfterAll
    static void tearDown() {
        deleteCustomerSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithSameCustomer_ShouldNotUpdateCustomer() {
        final CustomerDraft sameCustomerDraft = CustomerDraftBuilder.of(customerDraftJohnDoe).build();

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(sameCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 0, 0, 0);
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(customerSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 customers were processed in total (0 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithNewCustomer_ShouldCreateCustomer() {
        final CustomerDraft newCustomerDraft =
            CustomerDraftBuilder.of(customerDraftJohnDoe)
                                .emailVerified(false)
                                .email("john-2@example.com")
                                .customerNumber("gold-2")
                                .key("customer-key-john-doe-2")
                                .build();

        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .build();

        final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(newCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 1, 0, 0);
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(customerSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 customers were processed in total (1 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithUpdatedCustomer_ShouldUpdateCustomer() {
        final CustomerDraft updatedCustomerDraft =
            CustomerDraftBuilder.of(customerDraftJohnDoe)
                                .email("JOhn@example.com")
                                .build();

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(updatedCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(customerSyncStatistics).hasValues(1, 0, 1, 0);
        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(customerSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 customers were processed in total (0 created, 1 updated and 0 failed to sync).");
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
                    ResourceIdentifier.ofKey("store-hamburg"),
                    ResourceIdentifier.ofKey("store-berlin"),
                    ResourceIdentifier.ofKey("store-munich")))
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

        //        final CustomerDraft newCustomerDraft =
        //            CustomerDraftBuilder.of(customerDraftJohnDoe)
        //                                .email("jane@example.com")
        //                                .build();


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
