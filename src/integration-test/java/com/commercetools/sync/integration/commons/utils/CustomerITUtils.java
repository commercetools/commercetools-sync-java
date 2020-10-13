package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerDeleteCommand;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.types.ResourceTypeIdsSetBuilder;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import java.util.Locale;

import static com.commercetools.sync.integration.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;

public final class CustomerITUtils {

    /**
     * Deletes all customers from CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the customers will be deleted from.
     */
    public static void deleteCustomers(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, CustomerQuery.of(), CustomerDeleteCommand::of);
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
