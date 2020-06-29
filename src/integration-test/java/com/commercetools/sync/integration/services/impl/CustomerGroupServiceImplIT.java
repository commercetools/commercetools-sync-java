package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.impl.CustomerGroupServiceImpl;
import io.sphere.sdk.customergroups.CustomerGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.createCustomerGroup;
import static com.commercetools.sync.integration.commons.utils.CustomerGroupITUtils.deleteCustomerGroups;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;

class CustomerGroupServiceImplIT {
    private CustomerGroupService customerGroupService;
    private static final String CUSTOMER_GROUP_KEY = "customerGroup_key";
    private static final String CUSTOMER_GROUP_NAME = "customerGroup_name";

    private CustomerGroup oldCustomerGroup;
    private ArrayList<String> warnings;

    /**
     * Deletes customer group from the target CTP projects, then it populates the project with test data.
     */
    @BeforeEach
    void setup() {
        deleteCustomerGroups(CTP_TARGET_CLIENT);
        warnings = new ArrayList<>();
        oldCustomerGroup = createCustomerGroup(CTP_TARGET_CLIENT, CUSTOMER_GROUP_NAME, CUSTOMER_GROUP_KEY);
        final ProductSyncOptions productSyncOptions =
            ProductSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
                .warningCallback((exception, oldResource, newResource) -> warnings.add(exception.getMessage()))
                .build();
        customerGroupService = new CustomerGroupServiceImpl(productSyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteCustomerGroups(CTP_TARGET_CLIENT);
    }

    @Test
    void fetchCachedCustomerGroupId_WithNonExistingCustomerGroup_ShouldNotFetchACustomerGroup() {
        final Optional<String> customerGroupId = customerGroupService
            .fetchCachedCustomerGroupId("nonExistingKey")
            .toCompletableFuture()
            .join();

        assertThat(customerGroupId).isEmpty();
    }

    @Test
    void fetchCachedCustomerGroupId_WithExistingCustomerGroup_ShouldFetchCustomerGroupAndCache() {
        final Optional<String> customerGroupId = customerGroupService
            .fetchCachedCustomerGroupId(oldCustomerGroup.getKey())
            .toCompletableFuture()
            .join();
        assertThat(customerGroupId).contains(oldCustomerGroup.getId());
        assertThat(warnings).isEmpty();
    }

    @Test
    void fetchCachedCustomerGroupId_WithNullKey_ShouldReturnFutureWithEmptyOptional() {
        final Optional<String> customerGroupId = customerGroupService
            .fetchCachedCustomerGroupId(null)
            .toCompletableFuture()
            .join();

        assertThat(customerGroupId).isEmpty();
        assertThat(warnings).isEmpty();
    }
}
