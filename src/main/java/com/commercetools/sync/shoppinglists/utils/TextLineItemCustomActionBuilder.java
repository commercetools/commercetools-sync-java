package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetTextLineItemCustomTypeActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TextLineItemCustomActionBuilder
    implements GenericCustomActionBuilder<ShoppingListUpdateAction> {

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String textLineItemId) {

    return ShoppingListSetTextLineItemCustomTypeActionBuilder.of()
        .textLineItemId(textLineItemId)
        .build();
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String textLineItemId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {

    return ShoppingListSetTextLineItemCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .textLineItemId(textLineItemId)
        .build();
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String textLineItemId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {

    return ShoppingListSetTextLineItemCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .textLineItemId(textLineItemId)
        .build();
  }
}
