package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.CustomerGroupService;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class CustomerGroupServiceImpl implements CustomerGroupService {
    private final BaseSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    public static final String CUSTOMER_GROUP_KEY_NOT_SET = "CustomerGroup with id: '%s' has no key set. Keys are "
        + "required for customer group matching.";

    public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nullable final String key) {
        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (keyToIdCache.isEmpty()) {
            return cacheAndFetch(key);
        }
        return CompletableFuture.completedFuture(ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        final Consumer<List<CustomerGroup>> customerGroupPageConsumer = customerGroupsPage ->
            customerGroupsPage.forEach(customerGroup -> {

                final String fetchedCustomerGroupKey = customerGroup.getKey();
                final String fetchedCustomerGroupId = customerGroup.getId();

                if (isNotBlank(fetchedCustomerGroupKey)) {
                    keyToIdCache.put(fetchedCustomerGroupKey, fetchedCustomerGroupId);
                } else {
                    syncOptions.applyWarningCallback(format(CUSTOMER_GROUP_KEY_NOT_SET, fetchedCustomerGroupId));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), CustomerGroupQuery.of(), customerGroupPageConsumer)
                            .thenApply(result -> ofNullable(keyToIdCache.get(key)));
    }
}
