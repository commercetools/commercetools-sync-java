package com.commercetools.sync.sdk2.shoppinglists.utils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.customer.CustomerReference;
import com.commercetools.api.models.customer.CustomerReferenceBuilder;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraft;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ShoppingListsReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
  }

  @Test
  void
      mapToShoppingListDrafts_WithUnExpandedReferencesIdsCached_ShouldReturnResourceIdentifiersWithKeys() {
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";
    final String customerId = UUID.randomUUID().toString();
    final String customerKey = "customerKey";

    final Map<String, String> idToKeyValueMap = new HashMap<>();

    idToKeyValueMap.put(customTypeId, customTypeKey);
    idToKeyValueMap.put(customerId, customerKey);

    referenceIdToKeyCache.addAll(idToKeyValueMap);

    final ProductVariant mockProductVariant = mock(ProductVariant.class);
    when(mockProductVariant.getSku()).thenReturn("variant-sku");

    final List<ShoppingList> mockShoppingLists = new ArrayList<>();
    mockShoppingLists.add(null);

    final String textLineItemName = "textLineItemName";
    for (int i = 0; i < 3; i++) {
      final ShoppingList mockShoppingList = mock(ShoppingList.class);
      when(mockShoppingList.getName()).thenReturn(ofEnglish("testShoppingList"));

      final CustomerReference customerReference =
          CustomerReferenceBuilder.of().id(customerId).build();
      when(mockShoppingList.getCustomer()).thenReturn(customerReference);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();

      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);

      final ShoppingListLineItem mockLineItem = mock(ShoppingListLineItem.class);
      when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
      when(mockLineItem.getCustom()).thenReturn(mockCustomFields);

      when(mockShoppingList.getLineItems()).thenReturn(singletonList(mockLineItem));

      final TextLineItem mockTextLineItem = mock(TextLineItem.class);
      when(mockTextLineItem.getName()).thenReturn(ofEnglish(textLineItemName));
      when(mockTextLineItem.getCustom()).thenReturn(mockCustomFields);

      when(mockShoppingList.getTextLineItems()).thenReturn(singletonList(mockTextLineItem));

      mockShoppingLists.add(mockShoppingList);
    }

    final List<ShoppingListDraft> shoppingListDrafts =
        mapToShoppingListDrafts(mockShoppingLists, referenceIdToKeyCache);

    assertThat(shoppingListDrafts).hasSize(3);
    shoppingListDrafts.forEach(
        draft -> {
          assertThat(draft.getCustomer().getKey()).isEqualTo(customerKey);
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
                                      typeResourceIdentifierBuilder.key(customTypeKey))
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
                                      typeResourceIdentifierBuilder.key(customTypeKey))
                              .build())
                      .build());
        });
  }

  @Test
  void
      mapToShoppingListDrafts_WithNonExpandedReferences_ShouldReturnResourceIdentifiersWithoutReferencedKeys() {
    final String customTypeId = UUID.randomUUID().toString();
    final String customerId = UUID.randomUUID().toString();

    final List<ShoppingList> mockShoppingLists = new ArrayList<>();
    mockShoppingLists.add(null);

    for (int i = 0; i < 3; i++) {
      final ShoppingList mockShoppingList = mock(ShoppingList.class);
      when(mockShoppingList.getName()).thenReturn(ofEnglish("testShoppingList"));

      final CustomerReference customerReference =
          CustomerReferenceBuilder.of().id(customerId).build();
      when(mockShoppingList.getCustomer()).thenReturn(customerReference);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);

      final ShoppingListLineItem mockLineItemWithNullVariant = mock(ShoppingListLineItem.class);
      when(mockLineItemWithNullVariant.getVariant()).thenReturn(null);

      when(mockShoppingList.getLineItems()).thenReturn(singletonList(mockLineItemWithNullVariant));

      final TextLineItem mockTextLineItem = mock(TextLineItem.class);
      when(mockTextLineItem.getName()).thenReturn(ofEnglish("textLineItemName"));
      when(mockTextLineItem.getCustom()).thenReturn(mockCustomFields);

      when(mockShoppingList.getTextLineItems()).thenReturn(singletonList(mockTextLineItem));

      mockShoppingLists.add(mockShoppingList);
    }

    final List<ShoppingListDraft> shoppingListDrafts =
        mapToShoppingListDrafts(mockShoppingLists, referenceIdToKeyCache);

    assertThat(shoppingListDrafts).hasSize(3);
    shoppingListDrafts.forEach(
        draft -> {
          assertThat(draft.getCustomer().getId()).isEqualTo(customerId);
          assertThat(draft.getCustom().getType().getKey()).isNull();

          assertThat(draft.getLineItems()).isEqualTo(emptyList());

          assertThat(draft.getTextLineItems())
              .containsExactly(
                  TextLineItemDraftBuilder.of()
                      .name(ofEnglish("textLineItemName"))
                      .quantity(0L)
                      .custom(
                          CustomFieldsDraftBuilder.of()
                              .type(
                                  typeResourceIdentifierBuilder ->
                                      typeResourceIdentifierBuilder.id(customTypeId))
                              .build())
                      .build());
        });
  }

  @Test
  void mapToShoppingListDrafts_WithOtherFields_ShouldReturnDraftsCorrectly() {

    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getName()).thenReturn(ofEnglish("name"));
    when(mockShoppingList.getDescription()).thenReturn(ofEnglish("desc"));
    when(mockShoppingList.getKey()).thenReturn("key");
    when(mockShoppingList.getSlug()).thenReturn(ofEnglish("slug"));
    when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(2L);
    when(mockShoppingList.getAnonymousId()).thenReturn("anonymousId");

    when(mockShoppingList.getCustomer()).thenReturn(null);
    when(mockShoppingList.getCustom()).thenReturn(null);
    when(mockShoppingList.getLineItems()).thenReturn(null);
    when(mockShoppingList.getTextLineItems()).thenReturn(null);

    final List<ShoppingListDraft> shoppingListDrafts =
        mapToShoppingListDrafts(singletonList(mockShoppingList), referenceIdToKeyCache);

    assertThat(shoppingListDrafts)
        .containsExactly(
            ShoppingListDraftBuilder.of()
                .name(ofEnglish("name"))
                .description(ofEnglish("desc"))
                .lineItems(List.of())
                .textLineItems(List.of())
                .key("key")
                .slug(ofEnglish("slug"))
                .deleteDaysAfterLastModification(2L)
                .anonymousId("anonymousId")
                .build());
  }
}
