package com.commercetools.sync.cartdiscounts.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CartDiscountReferenceResolutionUtilsTest {

  Map<String, String> idToKeyValueMap = new HashMap<>();

  @AfterEach
  void clearCache() {
    idToKeyValueMap.clear();
  }

  @Test
  void
      mapToCartDiscountDrafts_WithNonExpandedReferencesAndIdIsCached_ShouldReturnResourceIdentifiersWithKeys() {
    final String customTypeId = UUID.randomUUID().toString();
    final String customTypeKey = "customTypeKey";

    final List<CartDiscount> mockCartDiscounts = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final CartDiscount mockCartDiscount = mock(CartDiscount.class);
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCartDiscount.getCustom()).thenReturn(mockCustomFields);
      mockCartDiscounts.add(mockCartDiscount);
    }

    // Cache customTypeId and customTypeKey Value
    idToKeyValueMap.put(customTypeId, customTypeKey);

    final List<CartDiscountDraft> referenceReplacedDrafts =
        CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(
            mockCartDiscounts, idToKeyValueMap);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getId()).isNull();
          assertThat(draft.getCustom().getType().getKey()).isEqualTo(customTypeKey);
        });
  }

  @Test
  void
      mapToCartDiscountDrafts_WithNonExpandedReferencesAndIdNotCached_ShouldReturnResourceIdentifiersWithoutKeys() {
    final String customTypeId = UUID.randomUUID().toString();

    final List<CartDiscount> mockCartDiscounts = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final CartDiscount mockCartDiscount = mock(CartDiscount.class);
      // Mock cartDiscounts custom fields with non-expanded type references.
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndId("resourceTypeId", customTypeId);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCartDiscount.getCustom()).thenReturn(mockCustomFields);

      mockCartDiscounts.add(mockCartDiscount);
    }

    final List<CartDiscountDraft> referenceReplacedDrafts =
        CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(
            mockCartDiscounts, idToKeyValueMap);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getId()).isEqualTo(customTypeId);
          assertThat(draft.getCustom().getType().getKey()).isNull();
        });
  }

  @Test
  void buildCartDiscountQuery_Always_ShouldReturnQueryWithoutReferencesExpanded() {
    final CartDiscountQuery cartDiscountQuery =
        CartDiscountReferenceResolutionUtils.buildCartDiscountQuery();
    assertThat(cartDiscountQuery.expansionPaths()).isEmpty();
  }
}
