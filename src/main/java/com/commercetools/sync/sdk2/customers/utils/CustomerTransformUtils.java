package com.commercetools.sync.sdk2.customers.utils;

import static com.commercetools.sync.sdk2.customers.utils.CustomerReferenceResolutionUtils.mapToCustomerDrafts;
import static java.util.stream.Collectors.toSet;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.sync.sdk2.commons.models.GraphQlQueryResource;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.sdk2.services.impl.BaseTransformServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
   */
  @Nonnull
  public static CompletableFuture<List<CustomerDraft>> toCustomerDrafts(
      @Nonnull final ProjectApiRoot client,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache,
      @Nonnull final List<Customer> customers) {

    final CustomerTransformUtils.CustomerTransformServiceImpl customerTransformService =
        new CustomerTransformUtils.CustomerTransformServiceImpl(client, referenceIdToKeyCache);
    return customerTransformService.toCustomerDrafts(customers);
  }

  private static class CustomerTransformServiceImpl extends BaseTransformServiceImpl {

    public CustomerTransformServiceImpl(
        @Nonnull final ProjectApiRoot ctpClient,
        @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
      super(ctpClient, referenceIdToKeyCache);
    }

    @Nonnull
    public CompletableFuture<List<CustomerDraft>> toCustomerDrafts(
        @Nonnull final List<Customer> customers) {

      final List<CompletableFuture<Void>> transformReferencesToRunParallel = new ArrayList<>();
      transformReferencesToRunParallel.add(this.transformCustomTypeReference(customers));
      transformReferencesToRunParallel.add(this.transformCustomerGroupReference(customers));

      return CompletableFuture.allOf(
              transformReferencesToRunParallel.stream().toArray(CompletableFuture[]::new))
          .thenApply(ignore -> mapToCustomerDrafts(customers, referenceIdToKeyCache));
    }

    @Nonnull
    private CompletableFuture<Void> transformCustomTypeReference(
        @Nonnull final List<Customer> customers) {

      final Set<String> setOfTypeIds =
          customers.stream()
              .map(Customer::getCustom)
              .filter(Objects::nonNull)
              .map(CustomFields::getType)
              .map(Reference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResource.TYPES);
    }

    @Nonnull
    private CompletableFuture<Void> transformCustomerGroupReference(
        @Nonnull final List<Customer> customers) {

      final Set<String> customerGroupIds =
          customers.stream()
              .map(Customer::getCustomerGroup)
              .filter(Objects::nonNull)
              .map(Reference::getId)
              .collect(toSet());

      return fetchAndFillReferenceIdToKeyCache(
          customerGroupIds, GraphQlQueryResource.CUSTOMER_GROUPS);
    }
  }
}
