package com.commercetools.sync.sdk2.customers.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupReferenceBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CustomerTransformUtilsTest {

  // todo: how to mock sdk 2 grapqhl request ?
  @Disabled
  @Test
  void transform_CustomerReferences_ShouldResolveReferencesUsingCacheAndMapToCustomerDraft() {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

    final String customerKey = "customerKey";
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String customerGroupId = UUID.randomUUID().toString();
    final String customerGroupKey = "customerGroupKey";

    final List<Customer> mockCustomersPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Customer mockCustomer = mock(Customer.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCustomer.getCustom()).thenReturn(mockCustomFields);
      when(mockCustomer.getKey()).thenReturn(customerKey);
      final CustomerGroupReference customerGroupReference =
          CustomerGroupReferenceBuilder.of().id(customerGroupId).build();
      when(mockCustomer.getCustomerGroup()).thenReturn(customerGroupReference);
      mockCustomersPage.add(mockCustomer);
    }

    final String jsonStringCustomTypes =
        "{\"results\":[{\"id\":\"" + customTypeId + "\"," + "\"key\":\"" + customTypeKey + "\"}]}";

    final String jsonStringCustomerGroups =
        "{\"results\":[{\"id\":\""
            + customerGroupId
            + "\","
            + "\"key\":\""
            + customerGroupKey
            + "\"}]}";

    //    when(sourceClient.graphql())
    //        .thenReturn(CompletableFuture.completedFuture(customTypesResult))
    //        .thenReturn(CompletableFuture.completedFuture(customerGroupsResult));

    // test
    //    final List<CustomerDraft> customersResolved =
    //
    // CustomerTransformUtils.toCustomerDrafts(mockCustomersPage).toCompletableFuture().join();

    //    // assertions
    //    final Optional<CustomerDraft> customerKey1 =
    //        customersResolved.stream()
    //            .filter(customerDraft -> customerKey.equals(customerDraft.getKey()))
    //            .findFirst();
    //
    //    assertThat(customerKey1)
    //        .hasValueSatisfying(
    //            customerDraft -> {
    //
    // assertThat(customerDraft.getCustom().getType().getKey()).isEqualTo(customTypeKey);
    //
    // assertThat(customerDraft.getCustomerGroup().getKey()).isEqualTo(customerGroupKey);
    //            });
  }
}
