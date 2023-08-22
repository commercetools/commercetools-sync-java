package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.mockGraphQLResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ByProjectKeyGraphqlPost;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.commercetools.api.models.shopping_list.ShoppingListLineItemDraftBuilder;
import com.commercetools.api.models.shopping_list.TextLineItem;
import com.commercetools.api.models.shopping_list.TextLineItemDraftBuilder;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ShoppingListTransformUtilsTest {

  @Test
  void transform_ShoppingListReferences_ShouldResolveReferencesUsingCacheAndMapToShoppingListDraft()
      throws JsonProcessingException {
    // preparation
    final ProjectApiRoot sourceClient = mock(ProjectApiRoot.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();

    final String customTypeId = UUID.randomUUID().toString();
    final String lineItemCustomTypeId = UUID.randomUUID().toString();
    final String textLineItemCustomTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String lineItemCustomTypeKey = "lineItemCustomTypeKey";
    final String textLineItemCustomTypeKey = "textLineItemCustomTypeKey";
    final String shoppingListKey = "shoppingListKeyValue";

    final String textLineItemName = "textLineItemName";

    final ProductVariant mockProductVariant = mock(ProductVariant.class);
    when(mockProductVariant.getSku()).thenReturn("variant-sku");

    final List<ShoppingList> mockShoppingListPage = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final ShoppingList mockShoppingList = mock(ShoppingList.class);
      when(mockShoppingList.getName()).thenReturn(ofEnglish("testShoppingList"));
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);
      when(mockShoppingList.getKey()).thenReturn(shoppingListKey);

      final CustomFields mockLineItemCustomFields = mock(CustomFields.class);
      final TypeReference lineItemTypeReference =
          TypeReferenceBuilder.of().id(lineItemCustomTypeId).build();
      when(mockLineItemCustomFields.getType()).thenReturn(lineItemTypeReference);

      final ShoppingListLineItem mockLineItem = mock(ShoppingListLineItem.class);
      when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
      when(mockLineItem.getCustom()).thenReturn(mockLineItemCustomFields);

      final CustomFields mockTextLineItemCustomFields = mock(CustomFields.class);
      final TypeReference textLineItemTypeReference =
          TypeReferenceBuilder.of().id(textLineItemCustomTypeId).build();
      when(mockTextLineItemCustomFields.getType()).thenReturn(textLineItemTypeReference);

      final TextLineItem mockTextLineItem = mock(TextLineItem.class);
      when(mockTextLineItem.getName()).thenReturn(ofEnglish(textLineItemName));
      when(mockTextLineItem.getCustom()).thenReturn(mockTextLineItemCustomFields);

      when(mockShoppingList.getLineItems()).thenReturn(Collections.singletonList(mockLineItem));
      when(mockShoppingList.getTextLineItems())
          .thenReturn(Collections.singletonList(mockTextLineItem));
      mockShoppingListPage.add(mockShoppingList);
    }

    final String jsonStringCustomTypes =
        "{\"typeDefinitions\":{\"results\":[{\"id\":\""
            + customTypeId
            + "\","
            + "\"key\":\""
            + customTypeKey
            + "\"}, "
            + " {\"id\":\""
            + lineItemCustomTypeId
            + "\","
            + "\"key\":\""
            + lineItemCustomTypeKey
            + "\"}, "
            + " {\"id\":\""
            + textLineItemCustomTypeId
            + "\","
            + "\"key\":\""
            + textLineItemCustomTypeKey
            + "\"} ]}}";
    final ApiHttpResponse<GraphQLResponse> customTypesResponse =
        mockGraphQLResponse(jsonStringCustomTypes);

    final ByProjectKeyGraphqlPost byProjectKeyGraphQlPost = mock(ByProjectKeyGraphqlPost.class);

    when(sourceClient.graphql()).thenReturn(mock());
    when(sourceClient.graphql().post(any(GraphQLRequest.class)))
        .thenReturn(byProjectKeyGraphQlPost);
    when(byProjectKeyGraphQlPost.execute())
        .thenReturn(CompletableFuture.completedFuture(customTypesResponse));

    // test
    final List<ShoppingListDraft> shoppingListDrafts =
        ShoppingListTransformUtils.toShoppingListDrafts(
                sourceClient, referenceIdToKeyCache, mockShoppingListPage)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(shoppingListDrafts).hasSize(10);
    shoppingListDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getKey()).isEqualTo(customTypeKey);

          assertThat(draft.getLineItems())
              .containsExactly(
                  ShoppingListLineItemDraftBuilder.of()
                      .sku("variant-sku")
                      .quantity(0L)
                      .custom(
                          CustomFieldsDraftBuilder.of()
                              .type(
                                  typeResourceIdentifierBuilder ->
                                      typeResourceIdentifierBuilder.key(lineItemCustomTypeKey))
                              .build())
                      .build());

          assertThat(draft.getTextLineItems())
              .containsExactly(
                  TextLineItemDraftBuilder.of()
                      .name(ofEnglish(textLineItemName))
                      .quantity(0L)
                      .custom(
                          CustomFieldsDraftBuilder.of()
                              .type(
                                  typeResourceIdentifierBuilder ->
                                      typeResourceIdentifierBuilder.key(textLineItemCustomTypeKey))
                              .build())
                      .build());
        });
  }
}
