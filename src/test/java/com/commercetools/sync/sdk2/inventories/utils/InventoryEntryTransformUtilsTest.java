package com.commercetools.sync.sdk2.inventories.utils;

import static com.commercetools.sync.sdk2.commons.utils.TestUtils.mockGraphQLResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class InventoryEntryTransformUtilsTest {

  @Test
  void transform_InventoryReferences_ShouldResolveReferencesUsingCacheAndMapToInventoryEntryDraft()
      throws JsonProcessingException {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String inventoryEntrySku = "inventoryEntrySkuValue";

    final List<InventoryEntry> mockInventoryEntriesPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);
      when(mockInventoryEntry.getSku()).thenReturn(inventoryEntrySku);
      when(mockInventoryEntry.getQuantityOnStock()).thenReturn(1L);
      mockInventoryEntriesPage.add(mockInventoryEntry);
    }

    final String jsonStringTypes =
        "{\"typeDefinitions\":{\"results\":[{\"id\":\""
            + customTypeId
            + "\",\"key\":\""
            + customTypeKey
            + "\"}]}}";
    final ApiHttpResponse<GraphQLResponse> response = mockGraphQLResponse(jsonStringTypes);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);
    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute()).thenReturn(CompletableFuture.completedFuture(response));

    // test
    final List<InventoryEntryDraft> inventoryEntryDraftsResolved =
        InventoryTransformUtils.toInventoryEntryDrafts(
                sourceClient, referenceIdToKeyCache, mockInventoryEntriesPage)
            .toCompletableFuture()
            .join();

    // assertions
    final Optional<InventoryEntryDraft> inventoryEntryDraft1 =
        inventoryEntryDraftsResolved.stream()
            .filter(inventoryEntryDraft -> inventoryEntrySku.equals(inventoryEntryDraft.getSku()))
            .findFirst();

    assertThat(inventoryEntryDraft1)
        .hasValueSatisfying(
            inventoryEntryDraft ->
                assertThat(inventoryEntryDraft.getCustom().getType().getKey())
                    .isEqualTo(customTypeKey));
  }
}
