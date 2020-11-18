package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.customers.CustomerSyncOptions;
import com.commercetools.sync.services.CustomerService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.customers.commands.CustomerCreateCommand;
import io.sphere.sdk.customers.commands.CustomerUpdateCommand;
import io.sphere.sdk.customers.expansion.CustomerExpansionModel;
import io.sphere.sdk.customers.queries.CustomerQuery;
import io.sphere.sdk.customers.queries.CustomerQueryBuilder;
import io.sphere.sdk.customers.queries.CustomerQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
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
        return cacheKeysToIds(keysToCache, keysNotCached -> new ResourceKeyIdGraphQlRequest(keysNotCached,
            GraphQlQueryResources.CUSTOMERS));
    }

    @Nonnull
    @Override
    public CompletionStage<Set<Customer>> fetchMatchingCustomersByKeys(
        @Nonnull final Set<String> customerKeys) {
        return fetchMatchingResources(customerKeys, () -> CustomerQueryBuilder
            .of()
            .plusPredicates(customerQueryModel -> customerQueryModel.key().isIn(customerKeys))
            .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Customer>> fetchCustomerByKey(@Nullable final String key) {
        return fetchResource(key, () -> CustomerQueryBuilder
            .of()
            .plusPredicates(customerQueryModel -> customerQueryModel.key().is(key))
            .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCustomerId(@Nonnull final String key) {
        return fetchCachedResourceId(key, () -> CustomerQueryBuilder
            .of()
            .plusPredicates(customerQueryModel -> customerQueryModel.key().is(key))
            .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<Customer>> createCustomer(
        @Nonnull final CustomerDraft customerDraft) {

        // Uses a different implementation than in the base service because CustomerCreateCommand uses a
        // different library as CTP responds with a CustomerSignInResult which is not extending resource but a
        // different model, containing the customer resource.
        final String draftKey = customerDraft.getKey();
        final CustomerCreateCommand createCommand = CustomerCreateCommand.of(customerDraft);

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