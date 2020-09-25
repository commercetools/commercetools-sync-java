package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.commands.CustomerDeleteCommand;
import io.sphere.sdk.customers.queries.CustomerQuery;

import javax.annotation.Nonnull;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;

public final class CustomerITUtils {

    /**
     * Deletes all customers from CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the customers will be deleted from.
     */
    public static void deleteCustomers(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, CustomerQuery.of(), CustomerDeleteCommand::of);
    }

    private CustomerITUtils() {
    }
}
