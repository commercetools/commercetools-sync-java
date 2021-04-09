package com.commercetools.sync.customers.service;

import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public interface CustomerTransformService {

  /**
   * Transforms customers by resolving the references and map them to CustomerDrafts.
   *
   * <p>This method resolves(fetch key values for the reference id's) non null and unexpanded
   * references of the customer{@link Customer} by using cache.
   *
   * <p>If the reference ids are already cached, key values are pulled from the cache, otherwise it
   * executes the query to fetch the key value for the reference id's and store the idToKey value
   * pair in the cache for reuse.
   *
   * <p>Then maps the Customer to CustomerDraft by performing reference resolution considering
   * idToKey value from the cache.
   *
   * @param customers the customers to resolve the references.
   * @return a new list which contains customerDrafts which have all their references resolved.
   */
  @Nonnull
  CompletableFuture<List<CustomerDraft>> toCustomerDrafts(@Nonnull List<Customer> customers);
}
