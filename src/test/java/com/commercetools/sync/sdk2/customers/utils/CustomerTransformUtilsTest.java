package com.commercetools.sync.sdk2.customers.utils;

import static com.commercetools.sync.sdk2.commons.utils.TestUtils.mockGraphQLResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraft;
import com.commercetools.api.models.customer_group.CustomerGroupReference;
import com.commercetools.api.models.customer_group.CustomerGroupReferenceBuilder;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CustomerTransformUtilsTest {

  final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void transform_CustomerReferences_ShouldResolveReferencesUsingCacheAndMapToCustomerDraft()
      throws Exception {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);

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
      when(mockCustomer.getEmail()).thenReturn("test@email.com");
      mockCustomersPage.add(mockCustomer);
    }

    final String jsonStringCustomTypes =
        "{ \"typeDefinitions\": {\"results\":[{\"id\":\""
            + customTypeId
            + "\","
            + "\"key\":\""
            + customTypeKey
            + "\"}]}}";
    final ApiHttpResponse<GraphQLResponse> customTypesResponse =
        mockGraphQLResponse(jsonStringCustomTypes);

    final String jsonStringCustomerGroups =
        "{ \"customerGroups\": {\"results\":[{\"id\":\""
            + customerGroupId
            + "\","
            + "\"key\":\""
            + customerGroupKey
            + "\"}]}}";
    final ApiHttpResponse<GraphQLResponse> customerGroupsResponse =
        mockGraphQLResponse(jsonStringCustomerGroups);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(customTypesResponse))
        .thenReturn(CompletableFuture.completedFuture(customerGroupsResponse));

    // test
    final List<CustomerDraft> customersResolved =
        CustomerTransformUtils.toCustomerDrafts(
                sourceClient, referenceIdToKeyCache, mockCustomersPage)
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
