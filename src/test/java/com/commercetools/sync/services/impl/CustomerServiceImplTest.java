package com.commercetools.sync.services.impl;

import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.*;
import com.commercetools.api.models.customer.*;
import com.commercetools.sync.commons.ExceptionUtils;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.customers.CustomerSyncOptionsBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadGatewayException;
import io.vrap.rmf.base.client.utils.CompletableFutureUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CustomerServiceImplTest {

  private CustomerServiceImpl service;
  private CustomerSyncOptions customerSyncOptions;
  private List<String> errorMessages;
  private List<Throwable> errorExceptions;

  @BeforeEach
  void setUp() {
    errorMessages = new ArrayList<>();
    errorExceptions = new ArrayList<>();
    customerSyncOptions =
        CustomerSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
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

    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder =
        mock(ByProjectKeyCustomersRequestBuilder.class);
    final ByProjectKeyCustomersPost byProjectKeyCustomersPost =
        mock(ByProjectKeyCustomersPost.class);
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(resultMock);
    when(byProjectKeyCustomersPost.execute()).thenReturn(completedFuture(apiHttpResponse));
    when(byProjectKeyCustomersRequestBuilder.post(any(CustomerDraft.class)))
        .thenReturn(byProjectKeyCustomersPost);
    when(customerSyncOptions.getCtpClient().customers())
        .thenReturn(byProjectKeyCustomersRequestBuilder);

    final CustomerDraft draft = mock(CustomerDraft.class);
    when(draft.getKey()).thenReturn("customerKey");
    final Optional<Customer> customerOptional =
        service.createCustomer(draft).toCompletableFuture().join();

    assertThat(customerOptional).isNotEmpty();
    assertThat(customerOptional).containsSame(customerMock);
    verify(byProjectKeyCustomersPost).execute();
    verify(byProjectKeyCustomersRequestBuilder).post(eq(draft));
  }

  @Test
  void createCustomer_WithUnSuccessfulMockResponse_ShouldNotCreate() {
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder =
        mock(ByProjectKeyCustomersRequestBuilder.class);
    final ByProjectKeyCustomersPost byProjectKeyCustomersPost =
        mock(ByProjectKeyCustomersPost.class);
    when(byProjectKeyCustomersPost.execute())
        .thenReturn(CompletableFutureUtils.failed(ExceptionUtils.createBadGatewayException()));
    when(byProjectKeyCustomersRequestBuilder.post(any(CustomerDraft.class)))
        .thenReturn(byProjectKeyCustomersPost);
    when(customerSyncOptions.getCtpClient().customers())
        .thenReturn(byProjectKeyCustomersRequestBuilder);

    final CustomerDraft draft = mock(CustomerDraft.class);
    when(draft.getKey()).thenReturn("customerKey");
    final Optional<Customer> customerOptional =
        service.createCustomer(draft).toCompletableFuture().join();

    assertThat(customerOptional).isEmpty();
    assertThat(errorExceptions).hasSize(1);
    assertThat(errorExceptions.get(0)).isExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0)).contains("Failed to create draft with key: 'customerKey'.");
    verify(byProjectKeyCustomersPost).execute();
    verify(byProjectKeyCustomersRequestBuilder).post(eq(draft));
  }

  @Test
  void createCustomer_WithDraftWithEmptyKey_ShouldNotCreate() {
    final CustomerDraft draft = mock(CustomerDraft.class);
    final Optional<Customer> customerOptional =
        service.createCustomer(draft).toCompletableFuture().join();

    assertThat(customerOptional).isEmpty();
    assertThat(errorExceptions).hasSize(1);
    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .contains("Failed to create draft with key: 'null'. Reason: Draft key is blank!");
    Mockito.verifyNoInteractions(customerSyncOptions.getCtpClient());
  }

  @Test
  void createCustomer_WithResponseIsNull_ShouldReturnEmpty() {
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder =
        mock(ByProjectKeyCustomersRequestBuilder.class);
    final ByProjectKeyCustomersPost byProjectKeyCustomersPost =
        mock(ByProjectKeyCustomersPost.class);
    when(byProjectKeyCustomersPost.execute()).thenReturn(completedFuture(null));
    when(byProjectKeyCustomersRequestBuilder.post(any(CustomerDraft.class)))
        .thenReturn(byProjectKeyCustomersPost);
    when(customerSyncOptions.getCtpClient().customers())
        .thenReturn(byProjectKeyCustomersRequestBuilder);

    final CustomerDraft draft = mock(CustomerDraft.class);
    when(draft.getKey()).thenReturn("key");
    final Optional<Customer> customerOptional =
        service.createCustomer(draft).toCompletableFuture().join();

    assertThat(customerOptional).isEmpty();
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
  }

  @Test
  void updateCustomer_WithSuccessfulMockCtpResponse_ShouldReturnMock() {
    final Customer customer = mock(Customer.class);
    when(customer.getId()).thenReturn("customerId");
    when(customer.getVersion()).thenReturn(1L);
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder =
        mock(ByProjectKeyCustomersRequestBuilder.class);
    final ByProjectKeyCustomersByIDRequestBuilder byProjectKeyCustomersByIDRequestBuilder =
        mock(ByProjectKeyCustomersByIDRequestBuilder.class);
    final ByProjectKeyCustomersByIDPost byProjectKeyCustomersByIDPost =
        mock(ByProjectKeyCustomersByIDPost.class);
    final ApiHttpResponse apiHttpResponse = mock(ApiHttpResponse.class);
    when(apiHttpResponse.getBody()).thenReturn(customer);
    when(byProjectKeyCustomersByIDPost.execute()).thenReturn(completedFuture(apiHttpResponse));
    when(byProjectKeyCustomersByIDRequestBuilder.post(any(CustomerUpdate.class)))
        .thenReturn(byProjectKeyCustomersByIDPost);
    when(byProjectKeyCustomersRequestBuilder.withId(anyString()))
        .thenReturn(byProjectKeyCustomersByIDRequestBuilder);
    when(customerSyncOptions.getCtpClient().customers())
        .thenReturn(byProjectKeyCustomersRequestBuilder);

    List<CustomerUpdateAction> updateActions =
        singletonList(CustomerSetFirstNameActionBuilder.of().firstName("Max").build());
    Customer result = service.updateCustomer(customer, updateActions).toCompletableFuture().join();

    assertThat(result).isSameAs(customer);
    verify(byProjectKeyCustomersByIDPost).execute();
    verify(byProjectKeyCustomersByIDRequestBuilder)
        .post(eq(CustomerUpdateBuilder.of().actions(updateActions).version(1L).build()));
  }

  @Test
  void fetchCachedCustomerId_WithBlankKey_ShouldNotFetchCustomerId() {
    Optional<String> customerId = service.fetchCachedCustomerId("").toCompletableFuture().join();

    assertThat(customerId).isEmpty();
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    Mockito.verifyNoInteractions(customerSyncOptions.getCtpClient());
  }

  @Test
  void fetchCachedCustomerId_WithCachedCustomer_ShouldFetchIdFromCache() {
    service.keyToIdCache.put("key", "id");
    Optional<String> customerId = service.fetchCachedCustomerId("key").toCompletableFuture().join();

    assertThat(customerId).contains("id");
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    Mockito.verifyNoInteractions(customerSyncOptions.getCtpClient());
  }

  @Test
  void fetchCachedCustomerId_WithUnexpectedException_ShouldFail() {
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder =
        mock(ByProjectKeyCustomersRequestBuilder.class);
    final ByProjectKeyCustomersKeyByKeyRequestBuilder byProjectKeyCustomersKeyByKeyRequestBuilder =
        mock(ByProjectKeyCustomersKeyByKeyRequestBuilder.class);
    final ByProjectKeyCustomersKeyByKeyGet byProjectKeyCustomersKeyByKeyGet =
        mock(ByProjectKeyCustomersKeyByKeyGet.class);
    when(byProjectKeyCustomersRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyCustomersKeyByKeyRequestBuilder);
    when(byProjectKeyCustomersKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyCustomersKeyByKeyGet);
    when(customerSyncOptions.getCtpClient().customers())
        .thenReturn(byProjectKeyCustomersRequestBuilder);
    when(byProjectKeyCustomersKeyByKeyGet.execute())
        .thenReturn(CompletableFutureUtils.failed(ExceptionUtils.createBadGatewayException()));

    assertThat(service.fetchCachedCustomerId("key"))
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(BadGatewayException.class);
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
  }

  @Test
  void fetchCustomerByKey_WithBlankKey_ShouldNotFetchCustomer() {
    final ByProjectKeyCustomersRequestBuilder byProjectKeyCustomersRequestBuilder =
        mock(ByProjectKeyCustomersRequestBuilder.class);
    final ByProjectKeyCustomersKeyByKeyRequestBuilder byProjectKeyCustomersKeyByKeyRequestBuilder =
        mock(ByProjectKeyCustomersKeyByKeyRequestBuilder.class);
    final ByProjectKeyCustomersKeyByKeyGet byProjectKeyCustomersKeyByKeyGet =
        mock(ByProjectKeyCustomersKeyByKeyGet.class);
    when(byProjectKeyCustomersRequestBuilder.withKey(anyString()))
        .thenReturn(byProjectKeyCustomersKeyByKeyRequestBuilder);
    when(byProjectKeyCustomersKeyByKeyRequestBuilder.get())
        .thenReturn(byProjectKeyCustomersKeyByKeyGet);
    when(customerSyncOptions.getCtpClient().customers())
        .thenReturn(byProjectKeyCustomersRequestBuilder);

    Optional<Customer> customer = service.fetchCustomerByKey("").toCompletableFuture().join();

    assertThat(customer).isEmpty();
    assertThat(errorExceptions).isEmpty();
    assertThat(errorMessages).isEmpty();
    verifyNoInteractions(byProjectKeyCustomersKeyByKeyGet);
  }
}
