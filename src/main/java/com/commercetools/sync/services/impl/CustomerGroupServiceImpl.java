package com.commercetools.sync.services.impl;

import com.commercetools.api.client.ByProjectKeyCustomerGroupsGet;
import com.commercetools.api.client.ByProjectKeyCustomerGroupsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyCustomerGroupsPost;
import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupDraft;
import com.commercetools.api.models.customer_group.CustomerGroupPagedQueryResponse;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.models.GraphQlQueryResource;
import com.commercetools.sync.services.CustomerGroupService;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// todo: reuse duplicated code between TypeService and CustomerService
public final class CustomerGroupServiceImpl
    extends BaseService<
        BaseSyncOptions,
        CustomerGroup,
        CustomerGroupDraft,
        ByProjectKeyCustomerGroupsGet,
        CustomerGroupPagedQueryResponse,
        ByProjectKeyCustomerGroupsKeyByKeyGet,
        CustomerGroup,
        ByProjectKeyCustomerGroupsPost>
    implements CustomerGroupService {

  public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> customerGroupKeys) {
    return super.cacheKeysToIdsUsingGraphQl(
        customerGroupKeys, GraphQlQueryResource.CUSTOMER_GROUPS);
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nonnull final String key) {
    ByProjectKeyCustomerGroupsGet query =
        syncOptions
            .getCtpClient()
            .customerGroups()
            .get()
            .withWhere("key in :keys")
            .withPredicateVar("keys", Collections.singletonList(key));

    return fetchCachedResourceId(key, query);
  }

  @Nonnull
  CompletionStage<Optional<String>> fetchCachedResourceId(
      @Nullable final String key, @Nonnull final ByProjectKeyCustomerGroupsGet query) {
    return super.fetchCachedResourceId(key, resource -> resource.getKey(), query);
  }
}
