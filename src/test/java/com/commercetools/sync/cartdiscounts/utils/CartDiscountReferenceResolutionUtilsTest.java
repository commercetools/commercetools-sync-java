package com.commercetools.sync.cartdiscounts.utils;

import static com.commercetools.sync.commons.MockUtils.getTypeMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.sphere.sdk.cartdiscounts.CartDiscount;
import io.sphere.sdk.cartdiscounts.CartDiscountDraft;
import io.sphere.sdk.cartdiscounts.queries.CartDiscountQuery;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartDiscountReferenceResolutionUtilsTest {

  @Test
  void mapToCartDiscountDrafts_WithExpandedReferences_ShouldReturnResourceIdentifiersWithKeys() {
    final Type mockCustomType = getTypeMock(UUID.randomUUID().toString(), "customTypeKey");

    final List<CartDiscount> mockCartDiscounts = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final CartDiscount mockCartDiscount = mock(CartDiscount.class);
      // Mock cartDiscounts custom fields with expanded type references.
      final CustomFields mockCustomFields = mock(CustomFields.class);
      final Reference<Type> typeReference =
          Reference.ofResourceTypeIdAndObj("resourceTypeId", mockCustomType);
      when(mockCustomFields.getType()).thenReturn(typeReference);
      when(mockCartDiscount.getCustom()).thenReturn(mockCustomFields);
      mockCartDiscounts.add(mockCartDiscount);
    }

    final List<CartDiscountDraft> referenceReplacedDrafts =
        CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(mockCartDiscounts);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getId()).isNull();
          assertThat(draft.getCustom().getType().getKey()).isEqualTo(mockCustomType.getKey());
        });
  }

  @Test
  void
      mapToCartDiscountDrafts_WithNonExpandedReferences_ShouldReturnResourceIdentifiersWithoutKeys() {
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
        CartDiscountReferenceResolutionUtils.mapToCartDiscountDrafts(mockCartDiscounts);

    referenceReplacedDrafts.forEach(
        draft -> {
          assertThat(draft.getCustom().getType().getId()).isEqualTo(customTypeId);
          assertThat(draft.getCustom().getType().getKey()).isNull();
        });
  }

  @Test
  void buildCartDiscountQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
    final CartDiscountQuery cartDiscountQuery =
        CartDiscountReferenceResolutionUtils.buildCartDiscountQuery();
    assertThat(cartDiscountQuery.expansionPaths()).containsExactly(ExpansionPath.of("custom.type"));
  }
}
