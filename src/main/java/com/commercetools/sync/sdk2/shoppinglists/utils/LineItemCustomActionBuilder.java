package com.commercetools.sync.sdk2.shoppinglists.utils;

import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetLineItemCustomTypeActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.sdk2.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LineItemCustomActionBuilder
    implements GenericCustomActionBuilder<ShoppingListUpdateAction> {

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String lineItemId) {
    return ShoppingListSetLineItemCustomTypeActionBuilder.of().lineItemId(lineItemId).build();
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String lineItemId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {
    return ShoppingListSetLineItemCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .lineItemId(lineItemId)
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String lineItemId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {
    return ShoppingListSetLineItemCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .lineItemId(lineItemId)
        .build();
  }
}
