package com.commercetools.sync.customers.utils;

import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.customers.service.CustomerTransformService;
import com.commercetools.sync.customers.service.impl.CustomerTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public final class CustomerTransformUtils {

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
   * @param client commercetools client.
   * @param referenceIdToKeyCache the instance that manages cache.
   * @param customers the customers to resolve the references.
   * @return a new list which contains customerDrafts which have all their references resolved.
   *     <p>TODO: Move the implementation from service class to this util class.
   */
  @Nonnull
  public static CompletableFuture<List<CustomerDraft>> toCustomerDrafts(
      @Nonnull final SphereClient client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<Customer> customers) {

    final CustomerTransformService customerTransformService =
        new CustomerTransformServiceImpl(client, referenceIdToKeyCache);
    return customerTransformService.toCustomerDrafts(customers);
  }
}
