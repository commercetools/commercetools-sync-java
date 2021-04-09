package com.commercetools.sync.shoppinglists.service.impl;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.models.ResourceIdsGraphQlRequest;
import com.commercetools.sync.commons.models.ResourceKeyIdGraphQlResult;
import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import com.commercetools.sync.shoppinglists.service.ShoppingListTransformService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ShoppingListTransformServiceImplTest {

  @Test
  void
      transform_ShoppingListReferences_ShouldResolveReferencesUsingCacheAndMapToShoppingListDraft() {
    // preparation
    final SphereClient sourceClient = mock(SphereClient.class);
    final ReferenceIdToKeyCache referenceIdToKeyCache = new CaffeineReferenceIdToKeyCacheImpl();
    final ShoppingListTransformService shoppingListTransformService =
        new ShoppingListTransformServiceImpl(sourceClient, referenceIdToKeyCache);

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
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);
      when(mockShoppingList.getKey()).thenReturn(shoppingListKey);

      final CustomFields mockLineItemCustomFields = mock(CustomFields.class);
      final Reference<Type> lineItemTypeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", lineItemCustomTypeId);
      when(mockLineItemCustomFields.getType()).thenReturn(lineItemTypeReference);

      final LineItem mockLineItem = mock(LineItem.class);
      when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
      when(mockLineItem.getCustom()).thenReturn(mockLineItemCustomFields);

      final CustomFields mockTextLineItemCustomFields = mock(CustomFields.class);
      final Reference<Type> textLineItemTypeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", textLineItemCustomTypeId);
      when(mockTextLineItemCustomFields.getType()).thenReturn(textLineItemTypeReference);

      final TextLineItem mockTextLineItem = mock(TextLineItem.class);
      when(mockTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish(textLineItemName));
      when(mockTextLineItem.getCustom()).thenReturn(mockTextLineItemCustomFields);

      when(mockShoppingList.getLineItems()).thenReturn(Collections.singletonList(mockLineItem));
      when(mockShoppingList.getTextLineItems())
          .thenReturn(Collections.singletonList(mockTextLineItem));
      mockShoppingListPage.add(mockShoppingList);
    }

    final String jsonStringCustomTypes =
        "{\"results\":[{\"id\":\""
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
            + "\"} ]}";
    final ResourceKeyIdGraphQlResult customTypesResult =
        SphereJsonUtils.readObject(jsonStringCustomTypes, ResourceKeyIdGraphQlResult.class);

    when(sourceClient.execute(any(ResourceIdsGraphQlRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(customTypesResult));

    // test
    final List<ShoppingListDraft> shoppingListDrafts =
        shoppingListTransformService
            .toShoppingListDrafts(mockShoppingListPage)
            .toCompletableFuture()
            .join();

    // assertions
    assertThat(shoppingListDrafts).hasSize(10);
    shoppingListDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getKey()).isEqualTo(customTypeKey);

          assertThat(draft.getLineItems())
              .containsExactly(
                  LineItemDraftBuilder.ofSku("variant-sku", 0L)
                      .custom(CustomFieldsDraft.ofTypeKeyAndJson(lineItemCustomTypeKey, emptyMap()))
                      .build());

          assertThat(draft.getTextLineItems())
              .containsExactly(
                  TextLineItemDraftBuilder.of(LocalizedString.ofEnglish(textLineItemName), 0L)
                      .custom(
                          CustomFieldsDraft.ofTypeKeyAndJson(textLineItemCustomTypeKey, emptyMap()))
                      .build());
        });
  }
}
