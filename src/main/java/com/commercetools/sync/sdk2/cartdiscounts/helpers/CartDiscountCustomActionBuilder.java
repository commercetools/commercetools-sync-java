package com.commercetools.sync.sdk2.cartdiscounts.helpers;

import com.commercetools.api.models.cart_discount.CartDiscountSetCustomFieldActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomTypeActionBuilder;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CartDiscountCustomActionBuilder
    implements GenericCustomActionBuilder<CartDiscountUpdateAction> {

  @Nonnull
  @Override
  public CartDiscountUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String objectId) {
    return CartDiscountSetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public CartDiscountUpdateAction buildSetCustomTypeAction(
      @Nullable Long variantId,
      @Nullable String objectId,
      @Nonnull String customTypeId,
      @Nullable Map<String, Object> customFieldsJsonMap) {
    return CartDiscountSetCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public CartDiscountUpdateAction buildSetCustomFieldAction(
      @Nullable Long variantId,
      @Nullable String objectId,
      @Nullable String customFieldName,
      @Nullable Object customFieldValue) {
    return CartDiscountSetCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
