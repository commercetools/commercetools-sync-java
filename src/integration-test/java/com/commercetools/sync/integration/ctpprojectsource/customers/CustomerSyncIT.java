package com.commercetools.sync.integration.ctpprojectsource.customers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.commercetools.api.models.common.AddressDraftBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.store.Store;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.customers.CustomerSync;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.customers.utils.CustomerTransformUtils;
import com.commercetools.sync.integration.commons.utils.CustomerITUtils;
import com.commercetools.sync.integration.commons.utils.ITUtils;
import com.commercetools.sync.integration.commons.utils.TestClientUtils;
import com.neovisionaries.i18n.CountryCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerSyncIT {
  private List<String> errorMessages;
  private List<Throwable> exceptions;
  private CustomerSync customerSync;
  private ReferenceIdToKeyCache referenceIdToKeyCache;

  @BeforeEach
  void setup() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(TestClientUtils.CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();
    customerSync = new CustomerSync(customerSyncOptions);
    referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
  }

  @AfterAll
  static void tearDown() {
    CustomerITUtils.deleteCustomers(TestClientUtils.CTP_TARGET_CLIENT);
    CustomerITUtils.deleteCustomers(TestClientUtils.CTP_SOURCE_CLIENT);
  }

  @Test
  void sync_WithoutUpdates_ShouldReturnProperStatistics() {
    CustomerITUtils.ensureSampleCustomerJohnDoe(TestClientUtils.CTP_TARGET_CLIENT);
    CustomerITUtils.ensureSampleCustomerJaneDoe(TestClientUtils.CTP_TARGET_CLIENT);
    final List<Customer> customers =
        TestClientUtils.CTP_TARGET_CLIENT.customers().get().execute().join().getBody().getResults();

    final List<CustomerDraft> customerDrafts =
        CustomerTransformUtils.toCustomerDrafts(
                TestClientUtils.CTP_TARGET_CLIENT, referenceIdToKeyCache, customers)
            .join();

    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(customerDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 2 customers were processed in total (0 created, 2 updated and 0 failed to sync).");
  }

  @Test
  void sync_WithUpdates_ShouldReturnProperStatistics() {
    CustomerITUtils.ensureSampleCustomerJaneDoe(TestClientUtils.CTP_TARGET_CLIENT);
    final List<Customer> customers =
        TestClientUtils.CTP_TARGET_CLIENT.customers().get().execute().join().getBody().getResults();

    final List<CustomerDraft> updatedCustomerDrafts = prepareUpdatedCustomerDrafts(customers);
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(updatedCustomerDrafts).toCompletableFuture().join();

    assertThat(errorMessages).isEmpty();
    assertThat(exceptions).isEmpty();

    // AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(2, 1, 1, 0);
    assertThat(customerSyncStatistics.getReportMessage())
        .isEqualTo(
            "Summary: 1 customers were processed in total (0 created, 1 updated and 0 failed to sync).");
  }

  private List<CustomerDraft> prepareUpdatedCustomerDrafts(
      @Nonnull final List<Customer> customers) {

    final Store storeCologne =
        CustomerITUtils.ensureStore(TestClientUtils.CTP_TARGET_CLIENT, "store-cologne");

    CustomerITUtils.ensureCustomerCustomType(
        "customer-type-gold", Locale.ENGLISH, "gold customers", TestClientUtils.CTP_TARGET_CLIENT);

    final List<CustomerDraft> customerDrafts =
        CustomerTransformUtils.toCustomerDrafts(
                TestClientUtils.CTP_TARGET_CLIENT, referenceIdToKeyCache, customers)
            .join();

    return customerDrafts.stream()
        .map(
            customerDraft ->
                CustomerDraftBuilder.of(customerDraft)
                    .plusStores(builder -> builder.key(storeCologne.getKey()))
                    .custom(
                        CustomFieldsDraftBuilder.of()
                            .type(
                                TypeResourceIdentifierBuilder.of()
                                    .key("customer-type-gold")
                                    .build())
                            .fields(ITUtils.createCustomFieldsJsonMap())
                            .build())
                    .addresses(
                        singletonList(
                            AddressDraftBuilder.of()
                                .country(CountryCode.DE.toString())
                                .city("cologne")
                                .key("address1")
                                .build()))
                    .defaultBillingAddress(0)
                    .billingAddresses(singletonList(0))
                    .defaultShippingAddress(0)
                    .shippingAddresses(singletonList(0))
                    .build())
        .collect(Collectors.toList());
  }
}
