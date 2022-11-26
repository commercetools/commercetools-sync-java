package com.commercetools.sync.integration.sdk2.services.impl;

import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerChangeEmailAction;
import com.commercetools.api.models.customer.CustomerChangeEmailActionBuilder;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptions;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.sdk2.services.CustomerService;
import com.commercetools.sync.sdk2.services.impl.CustomerServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceImplIT {
  private static final String EXISTING_CUSTOMER_KEY = "existing-customer-key";
  private CustomerService customerService;
  private Customer customer;

  private List<String> errorCallBackMessages;
  private List<String> warningCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  @BeforeEach
  void setupTest() {
    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
    warningCallBackMessages = new ArrayList<>();

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

    createTestCustomer();
    customerService = new CustomerServiceImpl(customerSyncOptions);
  }

  private void createTestCustomer() {
    try {
      customer =
          CTP_TARGET_CLIENT
              .customers()
              .withKey(EXISTING_CUSTOMER_KEY)
              .get()
              .executeBlocking()
              .getBody();
    } catch (Exception e) {
      // Create a mock new customer in the target project.
      CustomerDraft customerDraft =
          CustomerDraftBuilder.of()
              .email("mail@mail.com")
              .password("password")
              .key(EXISTING_CUSTOMER_KEY)
              .build();

      CTP_TARGET_CLIENT
          .customers()
          .post(customerDraft)
          .execute()
          .toCompletableFuture()
          .join()
          .getBody()
          .getCustomer();
    }
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
    final ProjectApiRoot spyClient = spy(CTP_TARGET_CLIENT);
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

    verify(spyClient, times(1)).graphql();

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
        CustomerDraftBuilder.of().email("mail@mail.com").password("password").key("newKeyTest1").build();

    Optional<Customer> customerOptional =
        customerService.createCustomer(customerDraft).toCompletableFuture().join();

    assertThat(customerOptional).isEmpty();
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .contains("There is already an existing customer with the provided email.");
    assertThat(errorCallBackExceptions).hasSize(1);
  }

  @Test
  void updateCustomer_WithValidChanges_ShouldUpdateCustomerCorrectly() {
    final String newEmail = "newMail@newmail.com";
    final CustomerChangeEmailAction changeEmail =
        CustomerChangeEmailActionBuilder.of().email(newEmail).build();

    final Customer updatedCustomer =
        customerService
            .updateCustomer(customer, singletonList(changeEmail))
            .toCompletableFuture()
            .join();
    assertThat(updatedCustomer).isNotNull();

    final Optional<Customer> queried =
        customerService.fetchCustomerByKey(EXISTING_CUSTOMER_KEY).toCompletableFuture().join();

    assertThat(errorCallBackExceptions).isEmpty();
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(queried).isNotEmpty();
    final Customer fetchedCustomer = queried.get();
    assertThat(fetchedCustomer.getEmail()).isEqualTo(updatedCustomer.getEmail());
    assertThat(fetchedCustomer.getPassword()).isEqualTo(updatedCustomer.getPassword());
  }
}
