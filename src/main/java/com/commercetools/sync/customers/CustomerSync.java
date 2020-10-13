package com.commercetools.sync.customers;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.customers.helpers.CustomerBatchValidator;
import com.commercetools.sync.customers.helpers.CustomerReferenceResolver;
import com.commercetools.sync.customers.helpers.CustomerSyncStatistics;
import com.commercetools.sync.services.CustomerGroupService;
import com.commercetools.sync.services.CustomerService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.CustomerGroupServiceImpl;
import com.commercetools.sync.services.impl.CustomerServiceImpl;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static com.commercetools.sync.customers.utils.CustomerSyncUtils.buildActions;
import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * This class syncs customer drafts with the corresponding customers in the CTP project.
 */
public class CustomerSync extends BaseSync<CustomerDraft, CustomerSyncStatistics, CustomerSyncOptions> {

    private static final String CTP_CUSTOMER_FETCH_FAILED = "Failed to fetch existing customers with keys: '%s'.";
    private static final String FAILED_TO_PROCESS = "Failed to process the CustomerDraft with key:'%s'. Reason: %s";
    private static final String CTP_CUSTOMER_UPDATE_FAILED = "Failed to update customer with key: '%s'. Reason: %s";

    private final CustomerService customerService;
    private final CustomerReferenceResolver referenceResolver;
    private final CustomerBatchValidator batchValidator;

    /**
     * Takes a {@link CustomerSyncOptions} to instantiate a new {@link CustomerSync} instance that could be used to
     * sync customer drafts in the CTP project specified in the injected {@link CustomerSync} instance.
     *
     * @param customerSyncOptions the container of all the options of the sync process including the CTP project
     *                                client and/or configuration and other sync-specific options.
     */
    public CustomerSync(@Nonnull final CustomerSyncOptions customerSyncOptions) {
        this(customerSyncOptions,
            new CustomerServiceImpl(customerSyncOptions),
            new TypeServiceImpl(customerSyncOptions),
            new CustomerGroupServiceImpl(customerSyncOptions));
    }

    /**
     * Takes a {@link CustomerSyncOptions} and service instances to instantiate a new {@link CustomerSync} instance
     * that could be used to sync customer drafts in the CTP project specified in the injected
     * {@link CustomerSyncOptions} instance.
     *
     * <p>NOTE: This constructor is mainly to be used for tests where the services can be mocked and passed to.
     *
     * @param customerSyncOptions the container of all the options of the sync process including the CTP project
     *                            client and/or configuration and other sync-specific options.
     * @param customerService  the customer service which is responsible for fetching/caching the Customers from the
     *                         CTP project.
     * @param typeService  the type service which is responsible for fetching/caching the Types from the CTP project.
     * @param customerGroupService  the customer group service which is responsible for fetching/caching the
     *                              CustomerGroups from the CTP project.
     */
    protected CustomerSync(@Nonnull final CustomerSyncOptions customerSyncOptions,
                           @Nonnull final CustomerService customerService,
                           @Nonnull final TypeService typeService,
                           @Nonnull final CustomerGroupService customerGroupService) {
        super(new CustomerSyncStatistics(), customerSyncOptions);
        this.customerService = customerService;
        this.referenceResolver = new CustomerReferenceResolver(getSyncOptions(), typeService, customerGroupService);
        this.batchValidator = new CustomerBatchValidator(getSyncOptions(), getStatistics());
    }

    /**
     * Iterates through the whole {@code customerDrafts} list and accumulates its valid drafts to batches.
     * Every batch is then processed by {@link CustomerSync#processBatch(List)}.
     *
     * <p><strong>Inherited doc:</strong>
     * {@inheritDoc}
     *
     * @param customerDrafts {@link List} of {@link CustomerDraft}'s that would be synced into CTP project.
     * @return {@link CompletionStage} with {@link CustomerSyncStatistics} holding statistics of all sync
     *         processes performed by this sync instance.
     */
    @Override
    protected CompletionStage<CustomerSyncStatistics> process(@Nonnull final List<CustomerDraft> customerDrafts) {
        final List<List<CustomerDraft>> batches = batchElements(customerDrafts, syncOptions.getBatchSize());
        return syncBatches(batches, completedFuture(statistics));

    }

    @Override
    protected CompletionStage<CustomerSyncStatistics> processBatch(@Nonnull final List<CustomerDraft> batch) {
        final ImmutablePair<Set<CustomerDraft>, CustomerBatchValidator.ReferencedKeys> result =
            batchValidator.validateAndCollectReferencedKeys(batch);

        final Set<CustomerDraft> validCustomerDrafts = result.getLeft();
        if (validCustomerDrafts.isEmpty()) {
            statistics.incrementProcessed(batch.size());
            return completedFuture(statistics);
        }

        return referenceResolver
            .populateKeyToIdCachesForReferencedKeys(result.getRight())
            .handle(ImmutablePair::new)
            .thenCompose(cachingResponse -> {
                final Throwable cachingException = cachingResponse.getValue();
                if (cachingException != null) {
                    handleError(new SyncException("Failed to build a cache of keys to ids.", cachingException),
                        validCustomerDrafts.size());
                    return CompletableFuture.completedFuture(null);
                }

                final Set<String> validCustomerKeys =
                    validCustomerDrafts.stream().map(CustomerDraft::getKey).collect(toSet());

                return customerService
                    .fetchMatchingCustomersByKeys(validCustomerKeys)
                    .handle(ImmutablePair::new)
                    .thenCompose(fetchResponse -> {
                        final Set<Customer> fetchedCustomers = fetchResponse.getKey();
                        final Throwable exception = fetchResponse.getValue();

                        if (exception != null) {
                            final String errorMessage = format(CTP_CUSTOMER_FETCH_FAILED, validCustomerKeys);
                            handleError(new SyncException(errorMessage, exception), validCustomerKeys.size());
                            return CompletableFuture.completedFuture(null);
                        } else {
                            return syncBatch(fetchedCustomers, validCustomerDrafts);
                        }
                    });
            })
            .thenApply(ignoredResult -> {
                statistics.incrementProcessed(batch.size());
                return statistics;
            });
    }

    @Nonnull
    private CompletionStage<Void> syncBatch(
        @Nonnull final Set<Customer> oldCustomers,
        @Nonnull final Set<CustomerDraft>  newCustomerDrafts) {

        final Map<String, Customer> oldCustomerMap = oldCustomers
            .stream()
            .collect(toMap(Customer::getKey, identity()));

        return CompletableFuture.allOf(newCustomerDrafts
            .stream()
            .map(customerDraft ->
                referenceResolver
                    .resolveReferences(customerDraft)
                    .thenCompose(resolvedCustomerDraft -> syncDraft(oldCustomerMap, resolvedCustomerDraft))
                    .exceptionally(completionException -> {
                        final String errorMessage = format(FAILED_TO_PROCESS, customerDraft.getKey(),
                            completionException.getMessage());
                        handleError(new SyncException(errorMessage, completionException), 1);
                        return null;
                    })
            )
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new));
    }

    @Nonnull
    private CompletionStage<Void> syncDraft(
        @Nonnull final Map<String, Customer> oldCustomerMap,
        @Nonnull final CustomerDraft newCustomerDraft) {

        final Customer oldCustomer = oldCustomerMap.get(newCustomerDraft.getKey());
        return Optional.ofNullable(oldCustomer)
                       .map(customer -> buildActionsAndUpdate(oldCustomer, newCustomerDraft))
                       .orElseGet(() -> applyCallbackAndCreate(newCustomerDraft));
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    @Nonnull
    private CompletionStage<Void> buildActionsAndUpdate(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomerDraft) {

        final List<UpdateAction<Customer>> updateActions =
            buildActions(oldCustomer, newCustomerDraft, syncOptions);

        final List<UpdateAction<Customer>> updateActionsAfterCallback
            = syncOptions.applyBeforeUpdateCallback(updateActions, newCustomerDraft, oldCustomer);

        if (!updateActionsAfterCallback.isEmpty()) {
            return updateCustomer(oldCustomer, newCustomerDraft, updateActionsAfterCallback);
        }

        return completedFuture(null);
    }

    @Nonnull
    private CompletionStage<Void> updateCustomer(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomerDraft,
        @Nonnull final List<UpdateAction<Customer>> updateActionsAfterCallback) {

        return customerService
            .updateCustomer(oldCustomer, updateActionsAfterCallback)
            .handle(ImmutablePair::of)
            .thenCompose(updateResponse -> {
                final Throwable exception = updateResponse.getValue();
                if (exception != null) {
                    return executeSupplierIfConcurrentModificationException(
                        exception,
                        () -> fetchAndUpdate(oldCustomer, newCustomerDraft),
                        () -> {
                            final String errorMessage =
                                format(CTP_CUSTOMER_UPDATE_FAILED, newCustomerDraft.getKey(), exception.getMessage());
                            handleError(new SyncException(errorMessage, exception), 1);
                            return CompletableFuture.completedFuture(null);
                        });
                } else {
                    statistics.incrementUpdated();
                    return CompletableFuture.completedFuture(null);
                }
            });
    }

    @Nonnull
    private CompletionStage<Void> fetchAndUpdate(
        @Nonnull final Customer oldCustomer,
        @Nonnull final CustomerDraft newCustomerDraft) {

        final String customerKey = oldCustomer.getKey();
        return customerService
            .fetchCustomerByKey(customerKey)
            .handle(ImmutablePair::of)
            .thenCompose(fetchResponse -> {
                final Optional<Customer> fetchedCustomerOptional = fetchResponse.getKey();
                final Throwable exception = fetchResponse.getValue();

                if (exception != null) {
                    final String errorMessage = format(CTP_CUSTOMER_UPDATE_FAILED, customerKey,
                        "Failed to fetch from CTP while retrying after concurrency modification.");
                    handleError(new SyncException(errorMessage, exception), 1);
                    return CompletableFuture.completedFuture(null);
                }

                return fetchedCustomerOptional
                    .map(fetchedCustomer -> buildActionsAndUpdate(fetchedCustomer, newCustomerDraft))
                    .orElseGet(() -> {
                        final String errorMessage = format(CTP_CUSTOMER_UPDATE_FAILED, customerKey,
                            "Not found when attempting to fetch while retrying after concurrency modification.");
                        handleError(new SyncException(errorMessage, null), 1);
                        return CompletableFuture.completedFuture(null);
                    });
            });
    }

    @Nonnull
    private CompletionStage<Void> applyCallbackAndCreate(@Nonnull final CustomerDraft customerDraft) {

        return syncOptions
            .applyBeforeCreateCallback(customerDraft)
            .map(draft -> customerService
                .createCustomer(draft)
                .thenAccept(customerOptional -> {
                    if (customerOptional.isPresent()) {
                        statistics.incrementCreated();
                    } else {
                        statistics.incrementFailed();
                    }
                }))
            .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    private void handleError(@Nonnull final SyncException syncException,
                             final int failedTimes) {
        syncOptions.applyErrorCallback(syncException);
        statistics.incrementFailed(failedTimes);
    }
}
