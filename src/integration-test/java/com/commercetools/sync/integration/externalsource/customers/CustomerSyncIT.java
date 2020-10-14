package com.commercetools.sync.integration.externalsource.customers;

import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.commands.updateactions.AddBillingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.AddShippingAddressIdWithKey;
import com.commercetools.sync.customers.commands.updateactions.SetDefaultBillingAddressWithKey;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.updateactions.AddAddress;
import io.sphere.sdk.customers.commands.updateactions.AddStore;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.RemoveAddress;
import io.sphere.sdk.customers.commands.updateactions.RemoveBillingAddressId;
import io.sphere.sdk.customers.commands.updateactions.SetCompanyName;
import io.sphere.sdk.customers.commands.updateactions.SetCustomField;
import io.sphere.sdk.customers.commands.updateactions.SetCustomerGroup;
import io.sphere.sdk.customers.commands.updateactions.SetDateOfBirth;
import io.sphere.sdk.customers.commands.updateactions.SetFirstName;
import io.sphere.sdk.customers.commands.updateactions.SetLocale;
import io.sphere.sdk.customers.commands.updateactions.SetMiddleName;
import io.sphere.sdk.customers.commands.updateactions.SetSalutation;
import io.sphere.sdk.customers.commands.updateactions.SetTitle;
import io.sphere.sdk.customers.commands.updateactions.SetVatId;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.createCustomerGroup;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJohnDoe;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomerSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ITUtils.BOOLEAN_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.LOCALISED_STRING_CUSTOM_FIELD_NAME;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerSyncIT {
    private CustomerDraft customerDraftJohnDoe;
    private Customer customerJohnDoe;

    private List<String> errorMessages;
    private List<Throwable> exceptions;
    private List<UpdateAction<Customer>> updateActionList;
    private CustomerSync customerSync;

    @BeforeEach
    void setup() {
        deleteCustomerSyncTestData(CTP_TARGET_CLIENT);
        final ImmutablePair<Customer, CustomerDraft> sampleCustomerJohnDoe =
            createSampleCustomerJohnDoe(CTP_TARGET_CLIENT);
        customerJohnDoe = sampleCustomerJohnDoe.getLeft();
        customerDraftJohnDoe = sampleCustomerJohnDoe.getRight();
        setUpCustomerSync();
    }

    private void setUpCustomerSync() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        updateActionList = new ArrayList<>();

        CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .beforeUpdateCallback((updateActions, customerDraft, customer) -> {
                updateActionList.addAll(Objects.requireNonNull(updateActions));
                return updateActions;
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

    @Test
    void sync_WithUpdatedAllFieldsOfCustomer_ShouldUpdateCustomerWithAllExpectedActions() {
        final Store storeCologne = createStore(CTP_TARGET_CLIENT, "store-cologne");
        final CustomerGroup customerGroupSilverMember =
            createCustomerGroup(CTP_TARGET_CLIENT, "silver members", "silver");

        final CustomerDraft updatedCustomerDraft =
            CustomerDraftBuilder
                .of("jane@example.com", "54321")
                .customerNumber("gold-1") // can not be changed after it set.
                .key("customer-key-john-doe")
                .stores(asList(
                    ResourceIdentifier.ofKey(storeCologne.getKey()), // new store
                    ResourceIdentifier.ofKey("store-munich"),
                    ResourceIdentifier.ofKey("store-hamburg"),
                    ResourceIdentifier.ofKey("store-berlin")))
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
                .addresses(asList( // address2 is removed, address4 is added
                    Address.of(CountryCode.DE).withCity("berlin").withKey("address1"),
                    Address.of(CountryCode.DE).withCity("munich").withKey("address3"),
                    Address.of(CountryCode.DE).withCity("cologne").withKey("address4")))
                .defaultBillingAddress(2) // 0 becomes 2 -> berlin to cologne.
                .billingAddresses(singletonList(2)) // 0, 1 becomes 2 -> berlin, hamburg to cologne.
                .defaultShippingAddress(1) // 2 becomes 1 -> munich to munich.
                .shippingAddresses(asList(0, 1)) // 2 become 0, 1 -> munich to berlin, munich.
                .custom(CustomFieldsDraft.ofTypeKeyAndJson("customer-type-gold",
                    createCustomFieldsJsonMap()))
                .locale(Locale.FRENCH)
                .build();

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(updatedCustomerDraft))
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();

        assertThat(customerSyncStatistics).hasValues(1, 0, 1, 0);
        assertThat(customerSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 1 customers were processed in total (0 created, 1 updated and 0 failed to sync).");

        final Map<String, String> addressKeyToIdMap =
            customerJohnDoe.getAddresses().stream().collect(toMap(Address::getKey, Address::getId));

        assertThat(updateActionList).containsExactly(
            ChangeEmail.of("jane@example.com"),
            SetFirstName.of("Jane"),
            SetMiddleName.of(""),
            SetTitle.of("Miss"),
            SetSalutation.of(""),
            SetCustomerGroup.of(Reference.of(CustomerGroup.referenceTypeId(), customerGroupSilverMember.getId())),
            SetCompanyName.of("Acme Corporation 2"),
            SetDateOfBirth.of(LocalDate.now().minusYears(26)),
            SetVatId.of("DE000000000"),
            SetLocale.of(Locale.FRENCH),
            RemoveAddress.of(addressKeyToIdMap.get("address2")),
            AddAddress.of(Address.of(CountryCode.DE).withCity("cologne").withKey("address4")),
            RemoveBillingAddressId.of(addressKeyToIdMap.get("address1")),
            SetDefaultBillingAddressWithKey.of("address4"),
            AddShippingAddressIdWithKey.of("address1"),
            AddBillingAddressIdWithKey.of("address4"),
            SetCustomField.ofJson(LOCALISED_STRING_CUSTOM_FIELD_NAME,
                JsonNodeFactory.instance.objectNode().put("de", "rot").put("en", "red")),
            SetCustomField.ofJson(BOOLEAN_CUSTOM_FIELD_NAME, JsonNodeFactory.instance.booleanNode(false)),
            AddStore.of(ResourceIdentifier.ofKey(storeCologne.getKey()))
        );
    }
}
