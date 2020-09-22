package com.commercetools.sync.integration.services.impl;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.impl.CustomerServiceImpl;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.ErrorResponseException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeEmail;
import io.sphere.sdk.customers.commands.updateactions.SetKey;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.queries.QueryPredicate;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import static org.mockito.Mockito.when;

class CustomerServiceImplIT {
    private static final String EXISTING_CUSTOMER_KEY = "existing-customer-key";
    private CustomerService customerService;
    private Customer customer;


    private List<String> errorCallBackMessages;
    private List<String> warningCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Deletes Customers from target CTP projects, then it populates target CTP project with customer test data.
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
                            (exception, oldResource, newResource) -> warningCallBackMessages
                                        .add(exception.getMessage()))
                        .build();

        // Create a mock new customer in the target project.
        CustomerDraft customerDraft = CustomerDraftBuilder
                .of("mail@mail.com", "password")
                .key(EXISTING_CUSTOMER_KEY)
                .build();
        customer = CTP_TARGET_CLIENT.execute(CustomerCreateCommand.of(customerDraft))
                .toCompletableFuture().join().getCustomer();

        customerService = new CustomerServiceImpl(customerSyncOptions);
    }

    /**
     * Cleans up the target test data that were built in this test class.
     */
    @AfterAll
    static void tearDown() {
        deleteCustomers(CTP_TARGET_CLIENT);
    }

    @Test
    void fetchCachedCustomerId_WithNonExistingCustomer_ShouldNotFetchACustomerId() {
        final Optional<String> customerId = customerService.fetchCachedCustomerId("non-existing-customer-key")
                .toCompletableFuture()
                .join();
        assertThat(customerId).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchCachedCustomerId_WithExistingNotCachedCustomer_ShouldFetchACustomerId() {
        final Optional<String> customerId = customerService.fetchCachedCustomerId(EXISTING_CUSTOMER_KEY)
                .toCompletableFuture()
                .join();
        assertThat(customerId).isNotEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchCachedCustomerId_WithCachedExistingCustomer_ShouldFetchFromCache() {
        final Optional<String> oldCustomerId = customerService.fetchCachedCustomerId(EXISTING_CUSTOMER_KEY)
                .toCompletableFuture()
                .join();

        final String newKey = "newKey";
        customerService.updateCustomer(customer, singletonList(SetKey.of(newKey)))
                .toCompletableFuture()
                .join();

        final Optional<String> cachedCustomerId = customerService.fetchCachedCustomerId(EXISTING_CUSTOMER_KEY)
                .toCompletableFuture().join();

        assertThat(cachedCustomerId).isNotEmpty();
        assertThat(cachedCustomerId).isEqualTo(oldCustomerId);

        final Optional<String> newCustomerId = customerService.fetchCachedCustomerId(newKey)
                .toCompletableFuture().join();

        assertThat(newCustomerId).isNotEmpty();
        assertThat(newCustomerId).isEqualTo(cachedCustomerId);

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }


    @Test
    void fetchCachedCustomerId_WithUnexpectedException_ShouldFail() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CustomerQuery.class)))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
                .thenCallRealMethod();
        final CustomerSyncOptions spyOptions = CustomerSyncOptionsBuilder
                .of(spyClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();
        final CustomerServiceImpl spyCustomerService = new CustomerServiceImpl(spyOptions);

        assertThat(spyCustomerService.fetchCachedCustomerId("key"))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void fetchCustomerByKey_WithBlankKey_ShouldReturnEmptyOptional() {
        Optional<Customer> customer = customerService.fetchCustomerByKey("")
                .toCompletableFuture()
                .join();
        assertThat(customer).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchCustomerByKey_WithExistingCustomer_ShouldFetchCustomer() {
        Optional<Customer> customer = customerService.fetchCustomerByKey(EXISTING_CUSTOMER_KEY)
                .toCompletableFuture()
                .join();
        assertThat(customer).isNotEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchCustomerByKey_WithNotExistingCustomer_ShouldReturnEmptyOptional() {
        Optional<Customer> customer = customerService.fetchCustomerByKey("not-existing-customer-key")
                .toCompletableFuture()
                .join();
        assertThat(customer).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }


    @Test
    void fetchCustomerByKey_WithUnexpectedException_ShouldFail() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CustomerQuery.class)))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
                .thenCallRealMethod();
        final CustomerSyncOptions spyOptions = CustomerSyncOptionsBuilder
                .of(spyClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();
        final CustomerServiceImpl spyCustomerService = new CustomerServiceImpl(spyOptions);

        assertThat(spyCustomerService.fetchCustomerByKey("key"))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void cacheKeysToIds_WithEmptyKeys_ShouldReturnCurrentCache() {
        Map<String, String> cache = customerService.cacheKeysToIds(emptySet()).toCompletableFuture().join();
        assertThat(cache).hasSize(0);

        cache = customerService.cacheKeysToIds(singleton(EXISTING_CUSTOMER_KEY)).toCompletableFuture().join();
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
                        .errorCallback((exception, oldResource, newResource, updateActions) -> {
                            errorCallBackMessages.add(exception.getMessage());
                            errorCallBackExceptions.add(exception.getCause());
                        })
                        .warningCallback((exception, oldResource, newResource)
                            -> warningCallBackMessages.add(exception.getMessage()))
                        .build();
        final CustomerServiceImpl spyCustomerService = new CustomerServiceImpl(customerSyncOptions);


        Map<String, String> cache = spyCustomerService.cacheKeysToIds(singleton(EXISTING_CUSTOMER_KEY))
                .toCompletableFuture().join();
        assertThat(cache).hasSize(1);

        cache = spyCustomerService.cacheKeysToIds(singleton(EXISTING_CUSTOMER_KEY))
                .toCompletableFuture().join();
        assertThat(cache).hasSize(1);

        verify(spyClient, times(1)).execute(any());
        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
    }

    @Test
    void fetchMatchingCustomersByKeys_WithEmptyKeys_ShouldReturnEmptySet() {
        Set<Customer> customers = customerService.fetchMatchingCustomersByKeys(emptySet())
                .toCompletableFuture()
                .join();

        assertThat(customers).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void fetchMatchingCustomersByKeys_WithExistingCustomerKeys_ShouldReturnCustomers() {
        Set<Customer> customers = customerService.fetchMatchingCustomersByKeys(singleton(EXISTING_CUSTOMER_KEY))
                .toCompletableFuture()
                .join();

        assertThat(customers).hasSize(1);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    void fetchMatchingCustomersByKeys_WithUnexpectedException_ShouldFail() {
        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        when(spyClient.execute(any(CustomerQuery.class)))
                .thenReturn(CompletableFutureUtils.exceptionallyCompletedFuture(new BadGatewayException()))
                .thenCallRealMethod();
        final CustomerSyncOptions spyOptions = CustomerSyncOptionsBuilder
                .of(spyClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();
        final CustomerServiceImpl spyCustomerService = new CustomerServiceImpl(spyOptions);

        assertThat(spyCustomerService.fetchMatchingCustomersByKeys(singleton(EXISTING_CUSTOMER_KEY)))
                .hasFailedWithThrowableThat()
                .isExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }


    @Test
    void createCustomer_WithValidCustomerData_ShouldCreateAndCacheId() {
        String newKey = "new-customer-key";
        CustomerDraft customerDraft = CustomerDraftBuilder
                .of("newmail@mail.com", "password")
                .key(newKey)
                .build();

        final SphereClient spyClient = spy(CTP_TARGET_CLIENT);
        final CustomerSyncOptions spyOptions = CustomerSyncOptionsBuilder
                .of(spyClient)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorCallBackMessages.add(exception.getMessage());
                    errorCallBackExceptions.add(exception.getCause());
                })
                .build();

        final CustomerService spyCustomerService = new CustomerServiceImpl(spyOptions);

        final Optional<Customer> createdCustomer = spyCustomerService
                .createCustomer(customerDraft)
                .toCompletableFuture().join();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();

        final Optional<Customer> queriedOptional = CTP_TARGET_CLIENT
                .execute(CustomerQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", newKey))))
                .toCompletableFuture().join().head();

        assertThat(queriedOptional)
                .hasValueSatisfying(queried -> assertThat(createdCustomer)
                        .hasValueSatisfying(created -> {
                            assertThat(queried.getKey()).isEqualTo(created.getKey());
                            assertThat(queried.getEmail()).isEqualTo(created.getEmail());
                            assertThat(queried.getPassword()).isEqualTo(created.getPassword());
                        }));

        final Optional<String> customerId =
                spyCustomerService.fetchCachedCustomerId(newKey).toCompletableFuture().join();
        assertThat(customerId).isPresent();
        verify(spyClient, times(0)).execute(any(CustomerQuery.class));
    }

    @Test
    void createCustomer_WithBlankKey_ShouldNotCreateCustomer() {
        final String newKey = "";
        CustomerDraft customerDraft = CustomerDraftBuilder
                .of("newmail@mail.com", "password")
                .key(newKey)
                .build();

        final Optional<Customer> createdCustomer = customerService
                .createCustomer(customerDraft)
                .toCompletableFuture().join();

        assertThat(createdCustomer).isEmpty();
        assertThat(errorCallBackMessages)
                .containsExactly("Failed to create draft with key: ''. Reason: Draft key is blank!");
        assertThat(errorCallBackExceptions).hasSize(1);
    }

    @Test
    void createCustomer_WithDuplicationException_ShouldNotCreateCustomer() {
        CustomerDraft customerDraft = CustomerDraftBuilder
                .of("mail@mail.com", "password")
                .key("newKey")
                .build();

        Optional<Customer> customerOptional =
                customerService.createCustomer(customerDraft).toCompletableFuture().join();

        assertThat(customerOptional).isEmpty();
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).contains("Failed to create draft with key: 'newKey'. Reason: "
                + "detailMessage: There is already an existing customer with the email '\"mail@mail.com\"'.");
        assertThat(errorCallBackExceptions).hasSize(1);
    }

    @Test
    void updateCustomer_WithValidChanges_ShouldUpdateCustomerCorrectly() {
        final String newEmail = "newMail@newmail.com";
        final ChangeEmail changeEmail = ChangeEmail
                .of(newEmail);

        final Customer updatedCustomer = customerService
                .updateCustomer(customer, singletonList(changeEmail))
                .toCompletableFuture().join();
        assertThat(updatedCustomer).isNotNull();

        final Optional<Customer> queried = CTP_TARGET_CLIENT
                .execute(CustomerQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", EXISTING_CUSTOMER_KEY))))
                .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(queried).isNotEmpty();
        final Customer fetchedCustomer = queried.get();
        assertThat(fetchedCustomer.getEmail()).isEqualTo(updatedCustomer.getEmail());
        assertThat(fetchedCustomer.getPassword()).isEqualTo(updatedCustomer.getPassword());

    }

    @Test
    @Disabled
    void changePassword_WithValidChanges_ShouldChangePassword() {
        final String newPwd = "newPassword";

        final Customer updatedCustomer = customerService
                .changePassword(customer, customer.getPassword(), newPwd)
                .toCompletableFuture().join();
        assertThat(updatedCustomer).isNotNull();

        final Optional<Customer> queried = CTP_TARGET_CLIENT
                .execute(CustomerQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", EXISTING_CUSTOMER_KEY))))
                .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(queried).isNotEmpty();
        final Customer fetchedCustomer = queried.get();
        assertThat(fetchedCustomer.getEmail()).isEqualTo(updatedCustomer.getEmail());
        assertThat(fetchedCustomer.getPassword()).isEqualTo(updatedCustomer.getPassword());
    }


    @Test
    void changePassword_WithInvalidCurrentPassword_ShouldFail() {
        final String newPwd = "newPassword";

        customerService
                .changePassword(customer, customer.getPassword(), newPwd)
                .exceptionally(exception -> {
                    assertThat(exception).isNotNull();
                    assertThat(exception).isExactlyInstanceOf(ErrorResponseException.class);
                    assertThat(exception.getMessage())
                            .containsIgnoringCase("The given current password does not match.");
                    return null;
                })
                .toCompletableFuture().join();


        final Optional<Customer> queried = CTP_TARGET_CLIENT
                .execute(CustomerQuery.of()
                        .withPredicates(QueryPredicate.of(format("key = \"%s\"", EXISTING_CUSTOMER_KEY))))
                .toCompletableFuture().join().head();

        assertThat(errorCallBackExceptions).isEmpty();
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(queried).isNotEmpty();
        final Customer fetchedCustomer = queried.get();
        assertThat(fetchedCustomer.getPassword()).isNotEqualTo(newPwd);

    }
}
