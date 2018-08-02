package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.utils.CtpQueryUtils;
import com.commercetools.sync.services.CustomerGroupService;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.lang.String.format;

public final class CustomerGroupServiceImpl implements CustomerGroupService {

    private final BaseSyncOptions syncOptions;
    private final Map<String, String> keyToIdCache = new ConcurrentHashMap<>();
    private boolean isCached = false;
    private static final String CUSTOMER_GROUP_KEY_NOT_SET = "CustomerGroup with id: '%s' has no key set. Keys are "
        + "required for customer group matching.";

    public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
        this.syncOptions = syncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nonnull final String key) {
        if (isCached) {
            return CompletableFuture.completedFuture(Optional.ofNullable(keyToIdCache.get(key)));
        }
        return cacheAndFetch(key);
    }

    private CompletionStage<Optional<String>> cacheAndFetch(@Nonnull final String key) {
        return cacheKeysToIds().thenApply(result -> Optional.ofNullable(keyToIdCache.get(key)));
    }

    @Nonnull
    @Override
    public CompletionStage<Map<String, String>> cacheKeysToIds() {
        if (isCached) {
            return CompletableFuture.completedFuture(keyToIdCache);
        }

        final Consumer<List<CustomerGroup>> customerGroupPageConsumer = customerGroupsPage ->
            customerGroupsPage.forEach(customerGroup -> {
                final String key = customerGroup.getKey();
                final String id = customerGroup.getId();
                if (StringUtils.isNotBlank(key)) {
                    keyToIdCache.put(key, id);
                } else {
                    syncOptions.applyWarningCallback(format(CUSTOMER_GROUP_KEY_NOT_SET, id));
                }
            });

        return CtpQueryUtils.queryAll(syncOptions.getCtpClient(), CustomerGroupQuery.of(), customerGroupPageConsumer)
                            .thenAccept(result -> isCached = true)
                            .thenApply(result -> keyToIdCache);
    }
}
