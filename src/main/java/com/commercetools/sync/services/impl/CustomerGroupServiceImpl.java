package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.helpers.ResourceKeyIdGraphQlRequest;
import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.services.CustomerGroupService;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customergroups.CustomerGroupDraft;
import io.sphere.sdk.customergroups.expansion.CustomerGroupExpansionModel;
import io.sphere.sdk.customergroups.queries.CustomerGroupQuery;
import io.sphere.sdk.customergroups.queries.CustomerGroupQueryBuilder;
import io.sphere.sdk.customergroups.queries.CustomerGroupQueryModel;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomerGroupServiceImpl
    extends BaseServiceWithKey<
        CustomerGroupDraft,
        CustomerGroup,
        CustomerGroup,
        BaseSyncOptions,
        CustomerGroupQuery,
        CustomerGroupQueryModel,
        CustomerGroupExpansionModel<CustomerGroup>>
    implements CustomerGroupService {

  public CustomerGroupServiceImpl(@Nonnull final BaseSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> customerGroupKeys) {

    return cacheKeysToIds(
        customerGroupKeys,
        keysNotCached ->
            new ResourceKeyIdGraphQlRequest(keysNotCached, GraphQlQueryResources.CUSTOMER_GROUPS));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nullable final String key) {

    return fetchCachedResourceId(
        key,
        () ->
            CustomerGroupQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.key().is(key))
                .build());
  }
}
