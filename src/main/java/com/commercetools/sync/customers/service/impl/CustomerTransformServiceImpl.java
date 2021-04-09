package com.commercetools.sync.customers.service.impl;

import static com.commercetools.sync.customers.utils.CustomerReferenceResolutionUtils.mapToCustomerDrafts;
import static java.util.stream.Collectors.toSet;

import com.commercetools.sync.commons.models.GraphQlQueryResources;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.customers.service.CustomerTransformService;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class CustomerTransformServiceImpl extends BaseTransformServiceImpl
    implements CustomerTransformService {

  public CustomerTransformServiceImpl(
      @Nonnull final SphereClient ctpClient,
      @Nonnull final ReferenceIdToKeyCache referenceIdToKeyCache) {
    super(ctpClient, referenceIdToKeyCache);
  }

  @Nonnull
  @Override
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

    return fetchAndFillReferenceIdToKeyCache(setOfTypeIds, GraphQlQueryResources.TYPES);
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
        customerGroupIds, GraphQlQueryResources.CUSTOMER_GROUPS);
  }
}
