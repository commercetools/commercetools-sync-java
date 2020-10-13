package com.commercetools.sync.customers;

import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.sphere.sdk.client.BadRequestException;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.CustomerDraftBuilder;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.helpers.CustomReferenceResolver.TYPE_DOES_NOT_EXIST;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_EMAIL_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_IS_NULL;
import static com.commercetools.sync.customers.helpers.CustomerBatchValidator.CUSTOMER_DRAFT_KEY_NOT_SET;
import static com.commercetools.sync.customers.helpers.CustomerReferenceResolver.FAILED_TO_RESOLVE_CUSTOM_TYPE;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
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

public class CustomerSyncTest {

    private CustomerSyncOptions syncOptions;
    private List<String> errorMessages;
    private List<Throwable> exceptions;

    @BeforeEach
    void setup() {
        errorMessages = new ArrayList<>();
        exceptions = new ArrayList<>();
        final SphereClient ctpClient = mock(SphereClient.class);

        syncOptions = CustomerSyncOptionsBuilder
            .of(ctpClient)
            .errorCallback((exception, oldResource, newResource, updateActions) -> {
                errorMessages.add(exception.getMessage());
                exceptions.add(exception);
            })
            .build();
    }

    @Test
    void sync_WithNullDraft_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final CustomerSync customerSync = new CustomerSync(syncOptions);

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(null))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo(CUSTOMER_DRAFT_IS_NULL);
    }

    @Test
    void sync_WithoutKey_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final CustomerSync customerSync = new CustomerSync(syncOptions);

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(CustomerDraftBuilder.of("email", "pass").build()))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo(format(CUSTOMER_DRAFT_KEY_NOT_SET, "email"));
    }

    @Test
    void sync_WithoutEmail_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final CustomerSync customerSync = new CustomerSync(syncOptions);

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(CustomerDraftBuilder.of(" ", "pass").key("key").build()))
            .toCompletableFuture()
            .join();

        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

        //assertions
        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .isEqualTo(format(CUSTOMER_DRAFT_EMAIL_NOT_SET, "key"));
    }

    @Test
    void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
        // preparation
        final TypeService typeService = spy(new TypeServiceImpl(syncOptions));
        when(typeService.cacheKeysToIds(anySet()))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        final CustomerSync customerSync = new CustomerSync(syncOptions, mock(CustomerService.class),
            typeService, mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .customerGroup(ResourceIdentifier.ofKey("customerGroupKey"))
                                .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
                                .stores(asList(ResourceIdentifier.ofKey("storeKey1"),
                                    ResourceIdentifier.ofKey("storeKey2"),
                                    ResourceIdentifier.ofId("storeId3")))
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();


        // assertions
        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING)).isEqualTo("Failed to build a cache of keys to ids.");

        assertThat(exceptions)
            .hasSize(1)
            .singleElement(as(THROWABLE))
            .isExactlyInstanceOf(SyncException.class)
            .hasCauseExactlyInstanceOf(CompletionException.class)
            .hasRootCauseExactlyInstanceOf(SphereException.class);
    }

    @Test
    void sync_WithErrorFetchingExistingKeys_ShouldExecuteCallbackOnErrorAndIncreaseFailedCounter() {
        final CustomerService mockCustomerService = mock(CustomerService.class);

        when(mockCustomerService.fetchMatchingCustomersByKeys(singleton("customer-key")))
            .thenReturn(supplyAsync(() -> {
                throw new SphereException();
            }));

        final CustomerSync customerSync = new CustomerSync(syncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        // test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(CustomerDraftBuilder.of("email", "pass")
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
            .hasRootCauseExactlyInstanceOf(SphereException.class);
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

        final CustomerSync customerSync = new CustomerSync(syncOptions, mockCustomerService,
            mockTypeService, mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .custom(CustomFieldsDraft.ofTypeKeyAndJson("typeKey", emptyMap()))
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

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
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

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
        when(mockCustomerService.createCustomer(any()))
            .thenReturn(completedFuture(Optional.empty()));

        final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

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
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

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
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

        // assertions
        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 0);

        verify(spyCustomerSyncOptions).applyBeforeUpdateCallback(emptyList(), customerDraft, mockCustomer);
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

        when(mockCustomerService.updateCustomer(any(), anyList()))
            .thenReturn(exceptionallyCompletedFuture(new BadRequestException("Invalid request")));

        final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

        // assertions
        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .contains("Invalid request");

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

        when(mockCustomerService.updateCustomer(any(), anyList()))
            .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
            .thenReturn(completedFuture(mockCustomer));

        when(mockCustomerService.fetchCustomerByKey("customerKey"))
            .thenReturn(completedFuture(Optional.of(mockCustomer)));

        final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

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

        when(mockCustomerService.updateCustomer(any(), anyList()))
            .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
            .thenReturn(completedFuture(mockCustomer));

        when(mockCustomerService.fetchCustomerByKey("customerKey"))
            .thenReturn(exceptionallyCompletedFuture(new SphereException()));

        final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

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
            .hasRootCauseExactlyInstanceOf(SphereException.class);
    }

    @Test
    void sync_WithConcurrentModificationExceptionAndUnexpectedDelete_ShouldFailToReFetchAndUpdate() {
        // preparation
        final CustomerService mockCustomerService = mock(CustomerService.class);
        final Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getKey()).thenReturn("customerKey");

        when(mockCustomerService.fetchMatchingCustomersByKeys(anySet()))
            .thenReturn(completedFuture(singleton(mockCustomer)));

        when(mockCustomerService.updateCustomer(any(), anyList()))
            .thenReturn(exceptionallyCompletedFuture(new SphereException(new ConcurrentModificationException())))
            .thenReturn(completedFuture(mockCustomer));

        when(mockCustomerService.fetchCustomerByKey("customerKey"))
            .thenReturn(completedFuture(Optional.empty()));

        final CustomerSyncOptions spyCustomerSyncOptions = spy(syncOptions);
        final CustomerSync customerSync = new CustomerSync(spyCustomerSyncOptions, mockCustomerService,
            mock(TypeService.class), mock(CustomerGroupService.class));

        final CustomerDraft customerDraft =
            CustomerDraftBuilder.of("email", "pass")
                                .key("customerKey")
                                .build();

        //test
        final CustomerSyncStatistics customerSyncStatistics = customerSync
            .sync(singletonList(customerDraft))
            .toCompletableFuture()
            .join();

        // assertions
        AssertionsForStatistics.assertThat(customerSyncStatistics).hasValues(1, 0, 0, 1);

        assertThat(errorMessages)
            .hasSize(1)
            .singleElement(as(STRING))
            .contains("Not found when attempting to fetch while retrying after concurrency modification.");

        assertThat(exceptions)
            .hasSize(1)
            .singleElement(as(THROWABLE))
            .isExactlyInstanceOf(SyncException.class)
            .hasNoCause();
    }
}
