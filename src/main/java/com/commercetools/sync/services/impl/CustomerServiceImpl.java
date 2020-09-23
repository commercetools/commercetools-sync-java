package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.services.CustomerService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.expansion.CustomerExpansionModel;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.customers.queries.CustomerQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.commercetools.sync.commons.utils.CtpQueryUtils.buildResourceKeysQueryPredicate;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isBlank;


public final class CustomerServiceImpl extends BaseServiceWithKey<CustomerDraft, Customer, CustomerSyncOptions,
    CustomerQuery, CustomerQueryModel, CustomerExpansionModel<Customer>> implements CustomerService {

    public CustomerServiceImpl(@Nonnull final CustomerSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(
        @Nonnull final Set<String> keysToCache) {
        return cacheKeysToIds(keysToCache, keysNotCached -> CustomerQuery
            .of()
            .withPredicates(buildResourceKeysQueryPredicate(keysNotCached)));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Customer>> fetchMatchingCustomersByKeys(
        @Nonnull final Set<String> customerKeys) {
        return fetchMatchingResources(customerKeys, () -> CustomerQuery
            .of()
            .withPredicates(buildResourceKeysQueryPredicate(customerKeys)));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Customer>> fetchCustomerByKey(@Nullable final String key) {
        return fetchResource(key, () -> CustomerQuery
            .of()
            .withPredicates(buildResourceKeysQueryPredicate(singleton(key))));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCustomerId(@Nonnull final String key) {
        return fetchCachedResourceId(key, () -> CustomerQuery
            .of()
            .withPredicates(buildResourceKeysQueryPredicate(singleton(key))));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Customer>> createCustomer(
        @Nonnull final CustomerDraft customerDraft) {

        final String draftKey = customerDraft.getKey();
        CustomerCreateCommand createCommand = CustomerCreateCommand.of(customerDraft);

        if (isBlank(draftKey)) {
            syncOptions.applyErrorCallback(
                new SyncException(format(CREATE_FAILED, draftKey, "Draft key is blank!")),
                null, customerDraft, null);
            return CompletableFuture.completedFuture(Optional.empty());
        } else {
            return syncOptions
                .getCtpClient()
                .execute(createCommand)
                .handle(((resource, exception) -> {
                    if (exception == null && resource.getCustomer() != null) {
                        keyToIdCache.put(draftKey, resource.getCustomer().getId());
                        return Optional.of(resource.getCustomer());
                    } else if (exception != null) {
                        syncOptions.applyErrorCallback(
                            new SyncException(format(CREATE_FAILED, draftKey, exception.getMessage()),
                                exception),
                            null, customerDraft, null);
                        return Optional.empty();
                    } else {
                        return Optional.empty();
                    }
                }));
        }

    }

    @Nonnull
    @Override
    public CompletionStage<Customer> updateCustomer(@Nonnull final Customer customer,
                                                    @Nonnull final
                                                    List<UpdateAction<Customer>> updateActions) {
        return updateResource(customer, CustomerUpdateCommand::of, updateActions);
    }

}