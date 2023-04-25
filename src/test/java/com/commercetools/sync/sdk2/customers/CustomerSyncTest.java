package com.commercetools.sync.sdk2.customers;

import static com.commercetools.sync.sdk2.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.sdk2.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET;
import static com.commercetools.sync.sdk2.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_IS_NULL;
import static com.commercetools.sync.sdk2.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.sdk2.customers.helpers.CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static io.vrap.rmf.base.client.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.statistics.AssertionsForStatistics;
import com.commercetools.sync.sdk2.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import com.commercetools.sync.sdk2.services.CustomerService;
import com.commercetools.sync.sdk2.services.TypeService;
import com.commercetools.sync.sdk2.services.impl.TypeServiceImpl;
import io.vrap.rmf.base.client.error.BadRequestException;
import io.vrap.rmf.base.client.error.ConcurrentModificationException;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomerSyncTest {

  private CustomerSyncOptions syncOptions;
  private List<String> errorMessages;
  private List<Throwable> exceptions;

  @BeforeEach
  void setup() {
    errorMessages = new ArrayList<>();
    exceptions = new ArrayList<>();
    final ProjectApiRoot ctpClient = mock(ProjectApiRoot.class);

    syncOptions =
        CustomerSyncOptionsBuilder.of(ctpClient)
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception);
                })
            .build();
  }

  @Test
  void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final CustomerSync customerSync = new CustomerSync(syncOptions);

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(null)).toCompletableFuture().join();

    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(CUSTOMER_DRAFT_IS_NULL);
  }

  @Test
  void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final CustomerSync customerSync = new CustomerSync(syncOptions);

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync
            .sync(singletonList(CustomerDraftBuilder.of().email("email").password("pass").build()))
            .toCompletableFuture()
            .join();

    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, "email"));
  }

  @Test
  void sync_WithoutEmail_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final CustomerSync customerSync = new CustomerSync(syncOptions);

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync
            .sync(
                singletonList(
                    CustomerDraftBuilder.of().email(" ").password("pass").key("key").build()))
            .toCompletableFuture()
            .join();

    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    // assertions
    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, "key"));
  }

  @SuppressWarnings("PMD")
  @Test
  void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
    // preparation
    final TypeService typeService = spy(new TypeServiceImpl(syncOptions));
    when(typeService.cacheKeysToIds(anySet()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new RuntimeException();
                }));

    final CustomerSync customerSync =
        new CustomerSync(
            syncOptions,
            mock(CustomerService.class),
            typeService,
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("customerKey")
            .customerGroup(
                CustomerGroupResourceIdentifierBuilder.of().key("customerGroupKey").build())
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("typeKey").build())
                    .build())
            .stores(
                asList(
                    StoreResourceIdentifierBuilder.of().key("storeKey1").build(),
                    StoreResourceIdentifierBuilder.of().key("storeKey2").build(),
                    StoreResourceIdentifierBuilder.of().id("storeId3").build()))
            .build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(RuntimeException.class);
  }

  @SuppressWarnings("PMD")
  @Test
  void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
    final CustomerService mockCustomerService = mock(CustomerService.class);

    when(mockCustomerService.fetchMatchingCustomersByKeys(singleton("customer-key")))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new RuntimeException();
                }));

    final CustomerSync customerSync =
        new CustomerSync(
            syncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync
            .sync(
                singletonList(
                    CustomerDraftBuilder.of()
                        .email("email")
                        .password("pass")
                        .key("customer-key")
                        .build()))
            .toCompletableFuture()
            .join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .isEqualTo("Failed to fetch existing customers with keys: '[customer-key]'.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(RuntimeException.class);
  }

  @Test
  void sync_WithNonExistingTypeReference_ShouldTriggerErrorCallbackAndReturnProperStats() {
    // preparation
    final TypeService mockTypeService = mock(TypeService.class);
    when(mockTypeService.fetchCachedTypeId(anyString())).thenReturn(completedFuture(empty()));
    when(mockTypeService.cacheKeysToIds(anySet())).thenReturn(completedFuture(emptyMap()));

    final CustomerService mockCustomerService = mock(CustomerService.class);
    when(mockCustomerService.fetchMatchingCustomersByKeys(singleton("customerKey")))
        .thenReturn(completedFuture(new HashSet<>(singletonList(mock(Customer.class)))));

    final CustomerSync customerSync =
        new CustomerSync(
            syncOptions, mockCustomerService, mockTypeService, mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of()
            .email("email")
            .password("pass")
            .key("customerKey")
            .key("customerKey")
            .custom(
                CustomFieldsDraftBuilder.of()
                    .type(TypeResourceIdentifierBuilder.of().key("typeKey").build())
                    .build())
            .build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    final String expectedExceptionMessage =
        format(FAILED_TO_RESOLVE_CUSTOM_TYPE, customerDraft.getKey());
    final String expectedMessageWithCause =
        format("%s Reason: %s", expectedExceptionMessage, format(TYPE_DOES_NOT_EXIST, "typeKey"));

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(expectedMessageWithCause);

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasCauseExactlyInstanceOf(CompletionException.class)
        .hasRootCauseExactlyInstanceOf(ReferenceResolutionException.class);
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallbackAndIncrementCreated() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomerService.fetchMatchingCustomersByKeys(singleton("customerKey")))
        .thenReturn(completedFuture(new HashSet<>(singletonList(mockCustomer))));

    when(mockCustomerService.createCustomer(any()))
        .thenReturn(completedFuture(Optional.of(mockCustomer)));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 1, 0, 0);

    verify(spyCustomerSyncOptions).applyBeforeCreateCallback(customerDraft);
    verify(spyCustomerSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_FailedOnCreation_ShouldCallBeforeCreateCallbackAndIncrementFailed() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomerService.fetchMatchingCustomersByKeys(singleton("customerKey")))
        .thenReturn(completedFuture(new HashSet<>(singletonList(mockCustomer))));

    // simulate an error during create, service will return an empty optional.
    when(mockCustomerService.createCustomer(any())).thenReturn(completedFuture(Optional.empty()));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    verify(spyCustomerSyncOptions).applyBeforeCreateCallback(customerDraft);
    verify(spyCustomerSyncOptions, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getKey()).thenReturn("customerKey");

    when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockCustomer)));

    when(mockCustomerService.updateCustomer(any(), anyList()))
        .thenReturn(completedFuture(mockCustomer));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 1, 0);

    verify(spyCustomerSyncOptions).applyBeforeUpdateCallback(any(), any(), any());
    verify(spyCustomerSyncOptions, never()).applyBeforeCreateCallback(customerDraft);
  }

  @Test
  void sync_WithoutUpdateActions_ShouldNotIncrementUpdated() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getKey()).thenReturn("customerKey");
    when(mockCustomer.getEmail()).thenReturn("email");

    when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockCustomer)));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 0);

    verify(spyCustomerSyncOptions)
        .applyBeforeUpdateCallback(emptyList(), customerDraft, mockCustomer);
    verify(spyCustomerSyncOptions, never()).applyBeforeCreateCallback(customerDraft);
  }

  @Test
  void sync_WithBadRequestException_ShouldFailToUpdateAndIncreaseFailedCounter() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getKey()).thenReturn("customerKey");

    when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockCustomer)));

    final BadRequestException badRequestException = mock(BadRequestException.class);
    when(badRequestException.getMessage()).thenReturn("bad request");

    when(mockCustomerService.updateCustomer(any(), anyList()))
        .thenReturn(exceptionallyCompletedFuture(badRequestException));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages).hasSize(1).singleElement(as(STRING)).contains("bad request");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasRootCauseExactlyInstanceOf(BadRequestException.class);
  }

  @Test
  void sync_WithConcurrentModificationException_ShouldRetryToUpdateNewCustomerWithSuccess() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getKey()).thenReturn("customerKey");

    when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockCustomer)));

    final ConcurrentModificationException concurrentModificationException =
        mock(ConcurrentModificationException.class);
    when(concurrentModificationException.getCause()).thenReturn(concurrentModificationException);

    when(mockCustomerService.updateCustomer(any(), anyList()))
        .thenReturn(exceptionallyCompletedFuture(concurrentModificationException))
        .thenReturn(completedFuture(mockCustomer));

    when(mockCustomerService.fetchCustomerByKey("customerKey"))
        .thenReturn(completedFuture(Optional.of(mockCustomer)));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 1, 0);
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndFailedFetch_ShouldFailToReFetchAndUpdate() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getKey()).thenReturn("customerKey");

    when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockCustomer)));

    final ConcurrentModificationException concurrentModificationException =
        mock(ConcurrentModificationException.class);
    when(concurrentModificationException.getCause()).thenReturn(concurrentModificationException);

    when(mockCustomerService.updateCustomer(any(), anyList()))
        .thenReturn(exceptionallyCompletedFuture(concurrentModificationException))
        .thenReturn(completedFuture(mockCustomer));

    final NotFoundException notFoundException = mock(NotFoundException.class);

    when(mockCustomerService.fetchCustomerByKey("customerKey"))
        .thenReturn(exceptionallyCompletedFuture(notFoundException));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to fetch from CTP while retrying after concurrency modification.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasRootCauseExactlyInstanceOf(NotFoundException.class);
  }

  @Test
  void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
    // preparation
    final CustomerService mockCustomerService = mock(CustomerService.class);
    final Customer mockCustomer = mock(Customer.class);
    when(mockCustomer.getKey()).thenReturn("customerKey");

    when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
        .thenReturn(completedFuture(singleton(mockCustomer)));

    final ConcurrentModificationException concurrentModificationException =
        mock(ConcurrentModificationException.class);
    when(concurrentModificationException.getCause()).thenReturn(concurrentModificationException);

    when(mockCustomerService.updateCustomer(any(), anyList()))
        .thenReturn(exceptionallyCompletedFuture(concurrentModificationException))
        .thenReturn(completedFuture(mockCustomer));

    when(mockCustomerService.fetchCustomerByKey("customerKey"))
        .thenReturn(completedFuture(Optional.empty()));

    final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
    final CustomerSync customerSync =
        new CustomerSync(
            spyCustomerSyncOptions,
            mockCustomerService,
            mock(TypeService.class),
            mock(CustomerGroupService.class));

    final CustomerDraft customerDraft =
        CustomerDraftBuilder.of().email("email").password("pass").key("customerKey").build();

    // test
    final CustomerSyncStatistics customerSyncStatistics =
        customerSync.sync(singletonList(customerDraft)).toCompletableFuture().join();

    // assertions
    AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains(
            "Not found when attempting to fetch while retrying after concurrency modification.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(SyncException.class)
        .hasNoCause();
  }
}
