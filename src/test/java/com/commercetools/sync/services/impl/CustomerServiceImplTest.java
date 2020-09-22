package com.commercetools.sync.services.impl;

import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerName;
import io.sphere.sdk.customers.CustomerSignInResult;
import io.sphere.sdk.customers.commands.CustomerChangePasswordCommand;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.commands.updateactions.ChangeName;
import io.sphere.sdk.utils.CompletableFutureUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        customerSyncOptions = CustomerSyncOptionsBuilder
                .of(mock(SphereClient.class))
                .errorCallback((exception, oldResource, newResource, updateActions) -> {
                    errorMessages.add(exception.getMessage());
                    errorExceptions.add(exception.getCause());
                })
                .build();
        service = new CustomerServiceImpl(customerSyncOptions);
    }

    @Test
    void createCustomer_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        final CustomerSignInResult resultMock = mock(CustomerSignInResult.class);
        final Customer customerMock = mock(Customer.class);
        when(customerMock.getId()).thenReturn("customerId");
        when(customerMock.getKey()).thenReturn("customerKey");
        when(resultMock.getCustomer()).thenReturn(customerMock);

        when(customerSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(resultMock));

        final CustomerDraft draft = mock(CustomerDraft.class);
        when(draft.getKey()).thenReturn("customerKey");
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();

        assertThat(customerOptional).isNotEmpty();
        assertThat(customerOptional).containsSame(customerMock);
        verify(customerSyncOptions.getCtpClient()).execute(eq(CustomerCreateCommand.of(draft)));
    }

    @Test
    void createCustomer_WithUnSuccessfulMockResponse_ShouldNotCreate() {
        final CustomerSignInResult resultMock = mock(CustomerSignInResult.class);
        final Customer customerMock = mock(Customer.class);
        when(customerMock.getId()).thenReturn("customerId");
        when(customerMock.getKey()).thenReturn("customerKey");
        when(resultMock.getCustomer()).thenReturn(customerMock);

        when(customerSyncOptions.getCtpClient().execute(any())).thenReturn(
                CompletableFutureUtils.failed(new BadRequestException("bad request")));

        final CustomerDraft draft = mock(CustomerDraft.class);
        when(draft.getKey()).thenReturn("customerKey");
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();

        assertThat(customerOptional).isEmpty();
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorExceptions.get(0)).isExactlyInstanceOf(BadRequestException.class);
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0)).contains("Failed to create draft with key: 'customerKey'.");
        verify(customerSyncOptions.getCtpClient()).execute(eq(CustomerCreateCommand.of(draft)));
    }

    @Test
    void createCustomer_WithDraftWithEmptyKey_ShouldNotCreate() {
        final CustomerDraft draft = mock(CustomerDraft.class);
        final Optional<Customer> customerOptional = service.createCustomer(draft).toCompletableFuture().join();

        assertThat(customerOptional).isEmpty();
        assertThat(errorExceptions).hasSize(1);
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0))
                .contains("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
        verifyNoInteractions(customerSyncOptions.getCtpClient());
    }

    @Test
    void updateCustomer_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        Customer customer = mock(Customer.class);
        when(customerSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(customer));

        List<UpdateAction<Customer>> updateActions =
                singletonList(ChangeName.of(CustomerName.of("title", "Max", "", "Mustermann")));
        Customer result = service.updateCustomer(customer, updateActions).toCompletableFuture().join();

        assertThat(result).isSameAs(customer);
        verify(customerSyncOptions.getCtpClient()).execute(eq(CustomerUpdateCommand.of(customer, updateActions)));
    }

    @Test
    void changePassword_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
        Customer customer = mock(Customer.class);
        when(customerSyncOptions.getCtpClient().execute(any())).thenReturn(completedFuture(customer));

        Customer result = service.changePassword(customer, "oldPwd", "newPwd")
                .toCompletableFuture()
                .join();

        assertThat(result).isSameAs(customer);
        verify(customerSyncOptions.getCtpClient()).execute(eq(CustomerChangePasswordCommand.of(customer, "oldPwd",
                "newPwd")));
    }

}