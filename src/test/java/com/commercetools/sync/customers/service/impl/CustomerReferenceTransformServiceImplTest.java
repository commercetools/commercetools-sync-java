package com.commercetools.sync.customers.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.customers.service.CustomerReferenceTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.customers.CustomerDraft;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class CustomerReferenceTransformServiceImplTest {

  @Test
  void transform_CustomerReferences_ShouldResolveReferencesUsingCacheAndMapToCustomerDraft() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final Map<String, String> cacheMap = new HashMap<>();
    final CustomerReferenceTransformService CustomerReferenceTransformService =
        new CustomerReferenceTransformServiceImpl(sourceClient, cacheMap);

    final String customerKey = "customerKey";
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String customerGroupId = UUID.randomUUID().toString();
    final String customerGroupKey = "customerGroupKey";

    final List<Customer> mockCustomersPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Customer mockCustomer = mock(Customer.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCustomer.getCustom()).thenReturn(mockCustomFields);
      when(mockCustomer.getKey()).thenReturn(customerKey);
      final Reference<CustomerGroup> customerGroupReference =
          Reference.ofResourceTypeIdAndId("resourceCustomerGroupId", customerGroupId);
      when(mockCustomer.getCustomerGroup()).thenReturn(customerGroupReference);
      mockCustomersPage.add(mockCustomer);
    }

    String jsonStringCustomTypes =
        "{\"results\":[{\"id\":\"" + customTypeId + "\"," + "\"key\":\"" + customTypeKey + "\"}]}";
    final ResourceKeyIdGraphQlResult customTypesResult =
        SphereJsonUtils.readObject(jsonStringCustomTypes, ResourceKeyIdGraphQlResult.class);

    String jsonStringCustomerGroups =
        "{\"results\":[{\"id\":\""
            + customerGroupId
            + "\","
            + "\"key\":\""
            + customerGroupKey
            + "\"}]}";
    final ResourceKeyIdGraphQlResult customerGroupsResult =
        SphereJsonUtils.readObject(jsonStringCustomerGroups, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(customTypesResult))
        .thenReturn(CompletableFuture.completedFuture(customerGroupsResult));

    // test
    final List<CustomerDraft> customersResolved =
        CustomerReferenceTransformService.transformCustomerReferences(mockCustomersPage)
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<CustomerDraft> customerKey1 =
        customersResolved.stream()
            .filter(customerDraft -> customerKey.equals(customerDraft.getKey()))
            .findFirst();

    assertThat(customerKey1)
        .hasValueSatisfying(
            customerDraft -> {
              assertThat(customerDraft.getCustom().getType().getKey()).isEqualTo(customTypeKey);
              assertThat(customerDraft.getCustomerGroup().getKey()).isEqualTo(customerGroupKey);
            });
  }
}
