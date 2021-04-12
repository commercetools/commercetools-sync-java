package com.commercetools.sync.shoppinglists.utils;

import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.buildShoppingListQuery;
import static com.commercetools.sync.shoppinglists.utils.ShoppingListReferenceResolutionUtils.mapToShoppingListDrafts;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.shoppinglists.LineItem;
import io.sphere.sdk.shoppinglists.LineItemDraftBuilder;
import io.sphere.sdk.shoppinglists.ShoppingList;
import io.sphere.sdk.shoppinglists.ShoppingListDraft;
import io.sphere.sdk.shoppinglists.ShoppingListDraftBuilder;
import io.sphere.sdk.shoppinglists.TextLineItem;
import io.sphere.sdk.shoppinglists.TextLineItemDraftBuilder;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;
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

      final Reference<Customer> customerReference =
          Reference.ofResourceTypeIdAndId(Customer.referenceTypeId(), customerId);
      when(mockShoppingList.getCustomer()).thenReturn(customerReference);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), customTypeId);

      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);

      final LineItem mockLineItem = mock(LineItem.class);
      when(mockLineItem.getVariant()).thenReturn(mockProductVariant);
      when(mockLineItem.getCustom()).thenReturn(mockCustomFields);

      when(mockShoppingList.getLineItems()).thenReturn(singletonList(mockLineItem));

      final TextLineItem mockTextLineItem = mock(TextLineItem.class);
      when(mockTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish(textLineItemName));
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
                  LineItemDraftBuilder.ofSku("variant-sku", 0L)
                      .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, emptyMap()))
                      .build());

          assertThat(draft.getTextLineItems())
              .containsExactly(
                  TextLineItemDraftBuilder.of(LocalizedString.ofEnglish(textLineItemName), 0L)
                      .custom(CustomFieldsDraft.ofTypeKeyAndJson(customTypeKey, emptyMap()))
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

      final Reference<Customer> customerReference =
          Reference.ofResourceTypeIdAndId(Customer.referenceTypeId(), customerId);
      when(mockShoppingList.getCustomer()).thenReturn(customerReference);

      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId(Type.referenceTypeId(), customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockShoppingList.getCustom()).thenReturn(mockCustomFields);

      final LineItem mockLineItemWithNullVariant = mock(LineItem.class);
      when(mockLineItemWithNullVariant.getVariant()).thenReturn(null);

      when(mockShoppingList.getLineItems()).thenReturn(singletonList(mockLineItemWithNullVariant));

      final TextLineItem mockTextLineItem = mock(TextLineItem.class);
      when(mockTextLineItem.getName()).thenReturn(LocalizedString.ofEnglish("textLineItemName"));
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
                  TextLineItemDraftBuilder.of(LocalizedString.ofEnglish("textLineItemName"), 0L)
                      .custom(CustomFieldsDraft.ofTypeIdAndJson(customTypeId, emptyMap()))
                      .build());
        });
  }

  @Test
  void mapToShoppingListDrafts_WithOtherFields_ShouldReturnDraftsCorrectly() {

    final ShoppingList mockShoppingList = mock(ShoppingList.class);
    when(mockShoppingList.getName()).thenReturn(LocalizedString.ofEnglish("name"));
    when(mockShoppingList.getDescription()).thenReturn(LocalizedString.ofEnglish("desc"));
    when(mockShoppingList.getKey()).thenReturn("key");
    when(mockShoppingList.getSlug()).thenReturn(LocalizedString.ofEnglish("slug"));
    when(mockShoppingList.getDeleteDaysAfterLastModification()).thenReturn(2);
    when(mockShoppingList.getAnonymousId()).thenReturn("anonymousId");

    when(mockShoppingList.getCustomer()).thenReturn(null);
    when(mockShoppingList.getCustom()).thenReturn(null);
    when(mockShoppingList.getLineItems()).thenReturn(null);
    when(mockShoppingList.getTextLineItems()).thenReturn(null);

    final List<ShoppingListDraft> shoppingListDrafts =
        mapToShoppingListDrafts(singletonList(mockShoppingList), referenceIdToKeyCache);

    assertThat(shoppingListDrafts)
        .containsExactly(
            ShoppingListDraftBuilder.of(LocalizedString.ofEnglish("name"))
                .description(LocalizedString.ofEnglish("desc"))
                .key("key")
                .slug(LocalizedString.ofEnglish("slug"))
                .deleteDaysAfterLastModification(2)
                .anonymousId("anonymousId")
                .build());
  }

  @Test
  void buildShoppingListQuery_Always_ShouldReturnQueryWithoutReferencesExpandedExceptVariant() {
    assertThat(buildShoppingListQuery().expansionPaths())
        .containsExactly(ExpansionPath.of("lineItems[*].variant"));
  }
}
