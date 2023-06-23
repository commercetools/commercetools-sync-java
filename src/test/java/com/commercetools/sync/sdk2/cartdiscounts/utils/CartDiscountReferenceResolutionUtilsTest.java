package com.commercetools.sync.sdk2.cartdiscounts.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountDraft;
import com.commercetools.api.models.cart_discount.CartDiscountValueRelative;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.type.CustomFields;
import com.commercetools.api.models.type.TypeReference;
import com.commercetools.api.models.type.TypeReferenceBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
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
      final CartDiscount mockCartDiscount = createMockCartDiscount();
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
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
      final CartDiscount mockCartDiscount = createMockCartDiscount();
      // Mock cartDiscounts custom fields with non-expanded type references.
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final TypeReference typeReference = TypeReferenceBuilder.of().id(customTypeId).build();
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

  private static CartDiscount createMockCartDiscount() {
    final CartDiscount mockCartDiscount = mock(CartDiscount.class);
    when(mockCartDiscount.getName()).thenReturn(LocalizedString.ofEnglish("test"));
    final CartDiscountValueRelative cartDiscountValue = mock(CartDiscountValueRelative.class);
    when(cartDiscountValue.getPermyriad()).thenReturn(10L);
    when(mockCartDiscount.getValue()).thenReturn(cartDiscountValue);
    when(mockCartDiscount.getCartPredicate()).thenReturn("test");
    when(mockCartDiscount.getSortOrder()).thenReturn("test");
    return mockCartDiscount;
  }
}
