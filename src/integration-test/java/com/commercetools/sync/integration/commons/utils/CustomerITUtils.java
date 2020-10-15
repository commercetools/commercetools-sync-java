package com.commercetools.sync.integration.commons.utils;

import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerDeleteCommand;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.util.Locale;

import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.createCustomerGroup;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.deleteCustomerGroups;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.commons.utils.ITUtils.deleteTypes;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.deleteStores;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

public final class CustomerITUtils {

    /**
     * Deletes all customers, types, stores and customer groups from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the customer sync test data.
     */
    public static void deleteCustomerSyncTestData(@Nonnull final SphereClient ctpClient) {
        deleteCustomers(ctpClient);
        deleteTypes(ctpClient);
        deleteStores(ctpClient);
        deleteCustomerGroups(ctpClient);
    }

    /**
     * Deletes all customers from CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the customers will be deleted from.
     */
    public static void deleteCustomers(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, CustomerQuery.of(), CustomerDeleteCommand::of);
    }

    public static ImmutablePair<Customer, CustomerDraft> createSampleCustomerJohnDoe(
        @Nonnull final SphereClient ctpClient) {

        final Store storeBerlin = createStore(ctpClient, "store-berlin");
        final Store storeHamburg = createStore(ctpClient, "store-hamburg");
        final Store storeMunich = createStore(ctpClient, "store-munich");

        final Type customTypeGoldMember = createCustomerCustomType("customer-type-gold", Locale.ENGLISH,
            "gold customers", ctpClient);

        final CustomerGroup customerGroupGoldMembers = createCustomerGroup(ctpClient, "gold members", "gold");

        final CustomerDraft customerDraftJohnDoe = CustomerDraftBuilder
            .of("john@example.com", "12345")
            .customerNumber("gold-1")
            .key("customer-key-john-doe")
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
            .customerGroup(ResourceIdentifier.ofKey(customerGroupGoldMembers.getKey()))
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

        final Customer customer = createCustomer(ctpClient, customerDraftJohnDoe);
        return ImmutablePair.of(customer, customerDraftJohnDoe);
    }

    public static void createSampleCustomerJaneDoe(@Nonnull final SphereClient ctpClient) {
        final CustomerDraft customerDraftJaneDoe = CustomerDraftBuilder
            .of("jane@example.com", "12345")
            .customerNumber("random-1")
            .key("customer-key-jane-doe")
            .firstName("Jane")
            .lastName("Doe")
            .middleName("Jr")
            .title("Miss")
            .salutation("Dear")
            .dateOfBirth(LocalDate.now().minusYears(25))
            .companyName("Acme Corporation")
            .vatId("FR000000000")
            .emailVerified(false)
            .addresses(asList(
                Address.of(CountryCode.DE).withCity("cologne").withKey("address1"),
                Address.of(CountryCode.DE).withCity("berlin").withKey("address2")))
            .defaultBillingAddress(0)
            .billingAddresses(singletonList(0))
            .defaultShippingAddress(1)
            .shippingAddresses(singletonList(1))
            .locale(Locale.ENGLISH)
            .build();

        createCustomer(ctpClient, customerDraftJaneDoe);
    }

    /**
     * Creates a {@link Customer} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the CustomerGroup in.
     * @param customerDraft  the draft of the customer to create.
     * @return the created customer.
     */
    public static Customer createCustomer(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final CustomerDraft customerDraft) {

        final CustomerSignInResult customerSignInResult = executeBlocking(
            ctpClient.execute(CustomerCreateCommand.of(customerDraft)));
        return customerSignInResult.getCustomer();
    }

    /**
     * This method blocks to create a customer custom type on the CTP project defined by the supplied
     * {@code ctpClient}, with the supplied data.
     *
     * @param typeKey   the type key
     * @param locale    the locale to be used for specifying the type name and field definitions names.
     * @param name      the name of the custom type.
     * @param ctpClient defines the CTP project to create the type on.
     */
    public static Type createCustomerCustomType(
        @Nonnull final String typeKey,
        @Nonnull final Locale locale,
        @Nonnull final String name,
        @Nonnull final SphereClient ctpClient) {

        return createTypeIfNotAlreadyExisting(
            typeKey,
            locale,
            name,
            ResourceTypeIdsSetBuilder.of().addCustomers(),
            ctpClient);
    }

    private CustomerITUtils() {
    }
}
