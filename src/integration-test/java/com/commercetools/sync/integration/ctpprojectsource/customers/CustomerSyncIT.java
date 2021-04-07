package com.commercetools.sync.integration.ctpprojectsource.customers;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils.buildCustomerQuery;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJaneDoe;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.createSampleCustomerJohnDoe;
import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomerSyncTestData;
import static com.commercetools.sync.integration.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.StoreITUtils.createStore;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.utils.InMemoryReferenceIdToKeyCache;
import com.commercetools.sync.commons.utils.InMemoryReferenceIdToKeyCacheImpl;
import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.customers.service.CustomerTransformService;
import com.commercetools.sync.customers.service.impl.CustomerTransformServiceImpl;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.types.CustomFieldsDraft;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerSyncIT {
  private List<String> errorMessages;
  private List<Throwable> exceptions;
  private CustomerSync customerSync;
  private InMemoryReferenceIdToKeyCache inMemoryReferenceIdToKeyCache;
  private CustomerTransformService customerTransformService;

  @BeforeEach
  void setup() {
    deleteCustomerSyncTestDataFromProjects();

    createSampleCustomerJohnDoe(CTP_SOURCE_CLIENT);
    createSampleCustomerJaneDoe(CTP_SOURCE_CLIENT);

    createSampleCustomerJohnDoe(CTP_TARGET_CLIENT);

    setUpCustomerSync();
  }

  @AfterAll
  static void tearDown() {
    deleteCustomerSyncTestDataFromProjects();
  }

  private static void deleteCustomerSyncTestDataFromProjects() {
    deleteCustomerSyncTestData(CTP_SOURCE_CLIENT);
    deleteCustomerSyncTestData(CTP_TARGET_CLIENT);
  }

  private void setUpCustomerSync() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();
    customerSync = new CustomerSync(customerSyncOptions);
    inMemoryReferenceIdToKeyCache = new InMemoryReferenceIdToKeyCacheImpl();
    customerTransformService =
        new CustomerTransformServiceImpl(CTP_SOURCE_CLIENT, inMemoryReferenceIdToKeyCache);
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {

    final List<Customer> customers =
        CTP_SOURCE_CLIENT.execute(buildCustomerQuery()).toCompletableFuture().join().getResults();

    final List<CustomerDraft> customerDrafts =
        customerTransformService.toCustomerDrafts(customers).join();

    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(customerDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    assertThat(customerSyncStatistics).hasValues(2, 1, 0, 0);
    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 customers were processed in total (1 created, 0 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdates_ShouldReturnProperStatistics() {

    final List<Customer> customers =
        CTP_SOURCE_CLIENT.execute(buildCustomerQuery()).toCompletableFuture().join().getResults();

    final List<CustomerDraft> updatedCustomerDrafts = prepareUpdatedCustomerDrafts(customers);
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(updatedCustomerDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(2, 1, 1, 0);
    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 customers were processed in total (1 created, 1 updated and 0 failed to sync).");
  }

  private List<CustomerDraft> prepareUpdatedCustomerDrafts(
      @Nonnull final List<Customer> customers) {

    final Store storeCologne = createStore(CTP_TARGET_CLIENT, "store-cologne");

    final List<CustomerDraft> customerDrafts =
        customerTransformService.toCustomerDrafts(customers).join();

    return customerDrafts.stream()
        .map(
            customerDraft ->
                CustomerDraftBuilder.of(customerDraft)
                    .plusStores(ResourceIdentifier.ofKey(storeCologne.getKey()))
                    .custom(
                        CustomFieldsDraft.ofTypeKeyAndJson(
                            "customer-type-gold", createCustomFieldsJsonMap()))
                    .addresses(
                        singletonList(
                            Address.of(CountryCode.DE).withCity("cologne").withKey("address1")))
                    .defaultBillingAddress(0)
                    .billingAddresses(singletonList(0))
                    .defaultShippingAddress(0)
                    .shippingAddresses(singletonList(0))
                    .build())
        .collect(Collectors.toList());
  }
}
