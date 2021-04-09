package com.commercetools.sync.cartdiscounts.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.commons.utils.ReferenceIdToKeyCache;
import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CartDiscountReferenceResolutionUtilsTest {

  private final ReferenceIdToKeyCache referenceIdToKeyCache =
      new CaffeineReferenceIdToKeyCacheImpl();

  @AfterEach
  void clearCache() {
    referenceIdToKeyCache.clearCache();
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
    referenceIdToKeyCache.add(customTypeId, customTypeKey);

    final List<CartDiscountDraft> referenceReplacedDrafts =
        CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(
            mockCartDiscounts, referenceIdToKeyCache);

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
            mockCartDiscounts, referenceIdToKeyCache);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getId()).isEqualTo(customTypeId);
          assertThat(draft.getCustom().getType().getKey()).isNull();
        });
  }
}
