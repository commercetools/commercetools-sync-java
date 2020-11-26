package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.FakeClient;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import io.sphere.sdk.client.BadGatewayException;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.updateactions.ChangeName;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

    private CustomerServiceImpl service;
    private CustomerSyncOptions customerSyncOptions;
    private List<String> errorMessages;
    private List<Throwable> errorExceptions;

    @BeforeEach
    void setUp() {
        errorMessages = new ArrayList<>();
        errorExceptions = new ArrayList<>();
        initMockService(mock(SphereClient.class));
    }

    @Test
    void createCustomer_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        final CustomerSignInResult resultMock = mock(CustomerSignInResult.class);
        final Customer customerMock = mock(Customer.class);
        when(customerMock.getId()).thenReturn("customerId");
        when(customerMock.getKey()).thenReturn("customerKey");
        when(resultMock.getCustomer()).thenReturn(customerMock);

        final FakeClient<CustomerSignInResult> fakeClient = new FakeClient<>(resultMock);
        initMockService(fakeClient);
        final CustomerDraft draft = mock(CustomerDraft.class);
        when(draft.getKey()).thenReturn("customerKey");
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();

        assertThat(customerOptional).isNotEmpty();
        assertThat(customerOptional).containsSame(customerMock);
        assertThat(fakeClient.isExecuted()).isTrue();
    }

    @Test
    void createCustomer_WithUnSuccessfulMockResponse_ShouldNotCreate() {
        final CustomerSignInResult resultMock = mock(CustomerSignInResult.class);
        final Customer customerMock = mock(Customer.class);
        when(customerMock.getId()).thenReturn("customerId");
        when(customerMock.getKey()).thenReturn("customerKey");
        when(resultMock.getCustomer()).thenReturn(customerMock);

        final FakeClient<Throwable> fakeClient = new FakeClient<>(new BadRequestException("bad request"));
        initMockService(fakeClient);

        final CustomerDraft draft = mock(CustomerDraft.class);
        when(draft.getKey()).thenReturn("customerKey");
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();

        assertThat(customerOptional).isEmpty();
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorExceptions.get(0)).isExactlyInstanceOf(BadRequestException.class);
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0)).contains("Failed to create draft with key: 'customerKey'.");
        assertThat(fakeClient.isExecuted()).isTrue();
    }

    @Test
    void createCustomer_WithDraftWithEmptyKey_ShouldNotCreate() {
        final CustomerDraft draft = mock(CustomerDraft.class);
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();
        final FakeClient<Customer> fakeClient = new FakeClient<>(mock(Customer.class));
        initMockService(fakeClient);
        assertThat(customerOptional).isEmpty();
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0))
            .contains("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
        assertThat(fakeClient.isExecuted()).isFalse();
    }

    @Test
    void createCustomer_WithResponseIsNull_ShouldReturnEmpty() {
        final FakeClient<CustomerSignInResult> fakeClient = new FakeClient<>(mock(CustomerSignInResult.class));
        initMockService(fakeClient);

        final CustomerDraft draft = mock(CustomerDraft.class);
        when(draft.getKey()).thenReturn("key");
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();

        assertThat(customerOptional).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }

    @Test
    void updateCustomer_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        Customer customer = mock(Customer.class);
        final FakeClient<Customer> fakeClient = new FakeClient<>(customer);
        initMockService(fakeClient);

        List<UpdateAction<Customer>> updateActions =
            singletonList(ChangeName.of(CustomerName.of("title", "Max", "", "Mustermann")));
        Customer result = service.updateCustomer(customer, updateActions).toCompletableFuture().join();

        assertThat(result).isSameAs(customer);
        assertThat(fakeClient.isExecuted()).isTrue();
    }

    @Test
    void fetchCachedCustomerId_WithBlankKey_ShouldNotFetchCustomerId() {
        final FakeClient<Customer> fakeClient = new FakeClient<>(mock(Customer.class));
        initMockService(fakeClient);
        Optional<String> customerId = service.fetchCachedCustomerId("")
                                             .toCompletableFuture()
                                             .join();

        assertThat(customerId).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeClient.isExecuted()).isFalse();
    }

    @Test
    void fetchCachedCustomerId_WithCachedCustomer_ShouldFetchIdFromCache() {
        final FakeClient<Customer> fakeClient = new FakeClient<>(mock(Customer.class));
        initMockService(fakeClient);
        service.keyToIdCache.put("key", "id");
        Optional<String> customerId = service.fetchCachedCustomerId("key")
                                             .toCompletableFuture()
                                             .join();

        assertThat(customerId).contains("id");
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeClient.isExecuted()).isFalse();
    }

    @Test
    void fetchCachedCustomerId_WithUnexpectedException_ShouldFail() {
        final FakeClient<Throwable> fakeClient = new FakeClient<>(new BadGatewayException("bad gateway"));
        initMockService(fakeClient);
        assertThat(service.fetchCachedCustomerId("key"))
            .failsWithin(1, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }

    @Test
    void fetchCustomerByKey_WithUnexpectedException_ShouldFail() {
        final FakeClient<Throwable> fakeClient = new FakeClient<>(new BadGatewayException("bad gateway"));
        initMockService(fakeClient);
        assertThat(service.fetchCustomerByKey("key"))
            .failsWithin(1, TimeUnit.SECONDS)
            .withThrowableOfType(ExecutionException.class)
            .withCauseExactlyInstanceOf(BadGatewayException.class);
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
    }

    @Test
    void fetchCustomerByKey_WithBlankKey_ShouldNotFetchCustomer() {
        final FakeClient<Throwable> fakeClient = new FakeClient<>(new BadGatewayException("bad gateway"));
        initMockService(fakeClient);
        Optional<Customer> customer = service.fetchCustomerByKey("")
                                             .toCompletableFuture()
                                             .join();

        assertThat(customer).isEmpty();
        assertThat(errorExceptions).isEmpty();
        assertThat(errorMessages).isEmpty();
        assertThat(fakeClient.isExecuted()).isFalse();
    }

    private void initMockService(@Nonnull final SphereClient client) {
        customerSyncOptions = CustomerSyncOptionsBuilder
                .of(client)
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    errorExceptions.add(exception.getCause());
                })
                .build();
        service = new CustomerServiceImpl(customerSyncOptions);
    }

}