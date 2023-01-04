package com.commercetools.sync.sdk2.services.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.services.CustomerGroupService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// todo: reuse duplicated code between TypeService and CustomerService
public final class CustomerGroupServiceImpl extends BaseService<BaseSyncOptions>
    implements CustomerGroupService {

  public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> customerGroupKeys) {
    return super.cacheKeysToIds(customerGroupKeys, GraphQlQueryResource.CUSTOMER_GROUPS);
  }

  @Nonnull
  public CompletionStage<Optional<CustomerGroup>> fetchCustomerGroupByKey(
      @Nullable final String key) {

    if (isBlank(key)) {
      return CompletableFuture.completedFuture(null);
    }

    return syncOptions
        .getCtpClient()
        .customerGroups()
        .withKey(key)
        .get()
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .thenApply(
            customerGroup -> {
              keyToIdCache.put(customerGroup.getKey(), customerGroup.getId());
              return Optional.of(customerGroup);
            })
        .exceptionally(
            throwable -> {
              if (throwable instanceof NotFoundException) {
                return Optional.empty();
              }
              // todo: what is the best way to handle this ?
              syncOptions.applyErrorCallback(new SyncException(throwable));
              return Optional.empty();
            });
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nonnull final String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }

    return fetchCustomerGroupByKey(key)
        .thenApply(customerGroup -> customerGroup.map(CustomerGroup::getId));
  }
}
