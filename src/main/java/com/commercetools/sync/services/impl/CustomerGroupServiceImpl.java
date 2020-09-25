package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.services.CustomerGroupService;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.expansion.CustomerGroupExpansionModel;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.customergroups.queries.CustomerGroupQueryBuilder;
import io.sphere.sdk.customergroups.queries.CustomerGroupQueryModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public final class CustomerGroupServiceImpl
    extends BaseServiceWithKey<CustomerGroupDraft, CustomerGroup, BaseSyncOptions, CustomerGroupQuery,
        CustomerGroupQueryModel, CustomerGroupExpansionModel<CustomerGroup>> implements CustomerGroupService {

    public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> customerGroupKeys) {

        return cacheKeysToIds(
            customerGroupKeys, keysNotCached -> CustomerGroupQueryBuilder
                .of()
                .plusPredicates(customerGroupQueryModel ->
                    customerGroupQueryModel.key().isIn(keysNotCached))
                .build());
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nullable final String key) {

        return fetchCachedResourceId(key,
            () -> CustomerGroupQueryBuilder
                .of()
                .plusPredicates(queryModel -> queryModel.key().is(key))
                .build());
    }
}
