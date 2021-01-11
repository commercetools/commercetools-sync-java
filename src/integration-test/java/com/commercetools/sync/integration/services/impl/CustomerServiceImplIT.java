package com.commercetools.sync.integration.services.impl;

import static com.commercetools.sync.integration.commons.utils.CustomerITUtils.deleteCustomers;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.impl.CustomerServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.queries.QueryPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceImplIT {
  private static final String EXISTING_CUSTOMER_KEY = "existing-customer-key";
  private CustomerService customerService;
  private Customer customer;

  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /**
   * Deletes Customers from target CTP projects, then it populates target CTP project with customer
   * test data.
   */
  @BeforeEach
  void setupTest() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();

    deleteCustomers(CTP_TARGET_CLIENT);

    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(CTP_TARGET_CLIENT)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();

    // Create a mock new customer in the target project.
    CustomerDraft customerDraft =
        CustomerDraftBuilder.of("mail@mail.com", "password").key(EXISTING_CUSTOMER_KEY).build();
    customer =
        CTP_TARGET_CLIENT
            .execute(CustomerCreateCommand.of(customerDraft))
            .toCompletableFuture()
            .join()
            .getCustomer();

    customerService = new CustomerServiceImpl(customerSyncOptions);
  }

  /** Cleans up the target test data that were built in this test class. */
  @AfterAll
  static void tearDown() {
    deleteCustomers(CTP_TARGET_CLIENT);
  }

  @Test
  void fetchCachedCustomerId_WithNonExistingCustomer_ShouldNotFetchACustomerId() {
    final Optional<String> customerId =
        customerService
            .fetchCachedCustomerId("non-existing-customer-key")
            .toCompletableFuture()
            .join();
    assertThat(customerId).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCachedCustomerId_WithExistingNotCachedCustomer_ShouldFetchACustomerId() {
    final Optional<String> customerId =
        customerService.fetchCachedCustomerId(EXISTING_CUSTOMER_KEY).toCompletableFuture().join();
    assertThat(customerId).isNotEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCustomerByKey_WithExistingCustomer_ShouldFetchCustomer() {
    Optional<Customer> customer =
        customerService.fetchCustomerByKey(EXISTING_CUSTOMER_KEY).toCompletableFuture().join();
    assertThat(customer).isNotEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchCustomerByKey_WithNotExistingCustomer_ShouldReturnEmptyOptional() {
    Optional<Customer> customer =
        customerService
            .fetchCustomerByKey("not-existing-customer-key")
            .toCompletableFuture()
            .join();
    assertThat(customer).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithEmptyKeys_ShouldReturnCurrentCache() {
    Map<String, String> cache =
        customerService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
    assertThat(cache).hasSize(0);

    cache =
        customerService
            .cacheKeysToIds(singleton(EXISTING_CUSTOMER_KEY))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    cache = customerService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
    assertThat(cache).hasSize(1);

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void cacheKeysToIds_WithCachedKeys_ShouldReturnCacheWithoutAnyRequests() {
    final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
    final CustomerSyncOptions customerSyncOptions =
        CustomerSyncOptionsBuilder.of(spyClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorCallBackMessages.add(exception.getMessage());
                  errorCallBackExceptions.add(exception.getCause());
                })
            .warningCallback(
                (exception, oldResource, newResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
    final CustomerServiceImpl spyCustomerService = new CustomerServiceImpl(customerSyncOptions);

    Map<String, String> cache =
        spyCustomerService
            .cacheKeysToIds(singleton(EXISTING_CUSTOMER_KEY))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    cache =
        spyCustomerService
            .cacheKeysToIds(singleton(EXISTING_CUSTOMER_KEY))
            .toCompletableFuture()
            .join();
    assertThat(cache).hasSize(1);

    verify(spyClient, times(1)).execute(any());
    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
  }

  @Test
  void fetchMatchingCustomersByKeys_WithEmptyKeys_ShouldReturnEmptySet() {
    Set<Customer> customers =
        customerService.fetchMatchingCustomersByKeys(emptySet()).toCompletableFuture().join();

    assertThat(customers).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void fetchMatchingCustomersByKeys_WithExistingCustomerKeys_ShouldReturnCustomers() {
    Set<Customer> customers =
        customerService
            .fetchMatchingCustomersByKeys(singleton(EXISTING_CUSTOMER_KEY))
            .toCompletableFuture()
            .join();

    assertThat(customers).hasSize(1);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void createCustomer_WithDuplicationException_ShouldNotCreateCustomer() {
    CustomerDraft customerDraft =
        CustomerDraftBuilder.of("mail@mail.com", "password").key("newKey").build();

    Optional<Customer> customerOptional =
        customerService.createCustomer(customerDraft).toCompletableFuture().join();

    assertThat(customerOptional).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains(
            "Failed to create draft with key: 'newKey'. Reason: "
                + "detailMessage: There is already an existing customer with the email '\"mail@mail.com\"'.");
    assertThat(errorCallBackExceptions).hasSize(1);
  }

  @Test
  void updateCustomer_WithValidChanges_ShouldUpdateCustomerCorrectly() {
    final String newEmail = "newMail@newmail.com";
    final ChangeEmail changeEmail = ChangeEmail.of(newEmail);

    final Customer updatedCustomer =
        customerService
            .updateCustomer(customer, singletonList(changeEmail))
            .toCompletableFuture()
            .join();
    assertThat(updatedCustomer).isNotNull();

    final Optional<Customer> queried =
        CTP_TARGET_CLIENT
            .execute(
                CustomerQuery.of()
                    .withPredicates(
                        QueryPredicate.of(format("key = \"%s\"", EXISTING_CUSTOMER_KEY))))
            .toCompletableFuture()
            .join()
            .head();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(queried).isNotEmpty();
    final Customer fetchedCustomer = queried.get();
    assertThat(fetchedCustomer.getEmail()).isEqualTo(updatedCustomer.getEmail());
    assertThat(fetchedCustomer.getPassword()).isEqualTo(updatedCustomer.getPassword());
  }
}
