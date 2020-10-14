package com.commercetools.sync.integration.ctpprojectsource.customers;

import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils.buildCustomerQuery;
import static com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils.mapToCustomerDrafts;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJaneDoe;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJohnDoe;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomerSyncTestData;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerSyncIT {

    @BeforeEach
    void setup() {
        deleteCustomerSyncTestDataFromProjects();

        createSampleCustomerJohnDoe(CTP_SOURCE_CLIENT);
        createSampleCustomerJaneDoe(CTP_SOURCE_CLIENT);

        createSampleCustomerJohnDoe(CTP_TARGET_CLIENT);
    }

    @AfterAll
    static void tearDown() {
        deleteCustomerSyncTestDataFromProjects();
    }

    private static void deleteCustomerSyncTestDataFromProjects() {
        deleteCustomerSyncTestData(CTP_SOURCE_CLIENT);
        deleteCustomerSyncTestData(CTP_TARGET_CLIENT);
    }

    @Test
    void sync_WithoutUpdates_ShouldReturnProperStatistics() {

        final List<Customer> customers = CTP_SOURCE_CLIENT
            .execute(buildCustomerQuery())
            .toCompletableFuture()
            .join()
            .getResults();

        final List<CustomerDraft> customerDrafts = mapToCustomerDrafts(customers);

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();

        final CustomerSyncOptions customerSyncOptions = CustomerSyncOptionsBuilder
            .of(CTP_TARGET_CLIENT)
            .errorCallback((exception, oldResource, newResource, actions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();

        final CustomerSync customerSync = new CustomerSync(customerSyncOptions);

        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(customerDrafts)
            .toCompletableFuture()
            .join();

        assertThat(errorMessages).isEmpty();
        assertThat(exceptions).isEmpty();
        assertThat(customerSyncStatistics).hasValues(2, 1, 0, 0);
        assertThat(customerSyncStatistics
            .getReportMessage())
            .isEqualTo("Summary: 2 customers were processed in total (1 created, 0 updated and 0 failed to sync).");
    }

    @Test
    void sync_WithUpdates_ShouldReturnProperStatistics() {


    }
}
