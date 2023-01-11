package com.commercetools.sync.sdk2.services.impl;

import static com.commercetools.sync.commons.utils.SyncUtils.batchElements;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.commercetools.api.client.ByProjectKeyCustomersGet;
import com.commercetools.api.client.ByProjectKeyCustomersKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyCustomersPost;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.customer.CustomerUpdateBuilder;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.customers.CustomerSyncOptions;
import com.commercetools.sync.sdk2.services.CustomerService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CustomerServiceImpl
    extends BaseService<
        CustomerSyncOptions,
        Customer,
        CustomerDraft,
        ByProjectKeyCustomersGet,
        ByProjectKeyCustomersKeyByKeyGet,
        CustomerSignInResult,
        ByProjectKeyCustomersPost>
    implements CustomerService {

  public CustomerServiceImpl(@Nonnull final CustomerSyncOptions syncOptions) {
    super(syncOptions);
  }

  @Nonnull
  @Override
  public CompletionStage<Map<String, String>> cacheKeysToIds(
      @Nonnull final Set<String> customerKeys) {
    return super.cacheKeysToIdsUsingGraphQl(customerKeys, GraphQlQueryResource.CUSTOMERS);
  }

  @Nonnull
  @Override
  public CompletionStage<Set<Customer>> fetchMatchingCustomersByKeys(
      @Nonnull final Set<String> keys) {
    return fetchMatchingResources(
        keys,
        customer -> customer.getKey(),
        (keysNotCached) ->
            syncOptions
                .getCtpClient()
                .customers()
                .get()
                .withWhere("key in :keys")
                .withPredicateVar("keys", keysNotCached));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Customer>> fetchCustomerByKey(@Nullable final String key) {
    return super.fetchResource(key, syncOptions.getCtpClient().customers().withKey(key).get());
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<String>> fetchCachedCustomerId(@Nonnull String key) {
    if (isBlank(key)) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    final String id = keyToIdCache.getIfPresent(key);
    if (id != null) {
      return CompletableFuture.completedFuture(Optional.of(id));
    }

    return fetchCustomerByKey(key).thenApply(customer -> customer.map(Customer::getId));
  }

  @Nonnull
  @Override
  public CompletionStage<Optional<Customer>> createCustomer(
      @Nonnull final CustomerDraft customerDraft) {
    return super.createResource(
        customerDraft,
        CustomerDraft::getKey,
        customerSignInResult -> customerSignInResult.getCustomer().getId(),
        CustomerSignInResult::getCustomer,
        syncOptions.getCtpClient().customers().post(customerDraft));
  }

  @Nonnull
  @Override
  public CompletionStage<Customer> updateCustomer(
      @Nonnull final Customer customer, @Nonnull final List<CustomerUpdateAction> updateActions) {

    final List<List<CustomerUpdateAction>> actionBatches =
        batchElements(updateActions, MAXIMUM_ALLOWED_UPDATE_ACTIONS);

    CompletionStage<ApiHttpResponse<Customer>> resultStage =
        CompletableFuture.completedFuture(new ApiHttpResponse<>(200, null, customer));

    for (final List<CustomerUpdateAction> batch : actionBatches) {
      resultStage =
          resultStage
              .thenApply(ApiHttpResponse::getBody)
              .thenCompose(
                  updatedCustomer ->
                      syncOptions
                          .getCtpClient()
                          .customers()
                          .withId(updatedCustomer.getId())
                          .post(
                              CustomerUpdateBuilder.of()
                                  .actions(batch)
                                  .version(updatedCustomer.getVersion())
                                  .build())
                          .execute());
    }

    return resultStage.thenApply(ApiHttpResponse::getBody);
  }
}
