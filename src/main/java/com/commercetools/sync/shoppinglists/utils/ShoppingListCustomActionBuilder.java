package com.commercetools.sync.shoppinglists.utils;

import com.commercetools.api.models.shopping_list.ShoppingListSetCustomFieldActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListSetCustomTypeActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.sync.commons.helpers.GenericCustomActionBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ShoppingListCustomActionBuilder
    implements GenericCustomActionBuilder<ShoppingListUpdateAction> {

  private static final ShoppingListCustomActionBuilder builder =
      new ShoppingListCustomActionBuilder();

  private ShoppingListCustomActionBuilder() {
    super();
  }

  @Nonnull
  @SuppressFBWarnings
  public static ShoppingListCustomActionBuilder of() {
    return builder;
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildRemoveCustomTypeAction(
      @Nullable final Long variantId, @Nullable final String objectId) {
    return ShoppingListSetCustomTypeActionBuilder.of().build();
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildSetCustomTypeAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nonnull final String customTypeId,
      @Nullable final Map<String, Object> customFieldsJsonMap) {

    return ShoppingListSetCustomTypeActionBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.id(customTypeId))
        .fields(fieldContainerBuilder -> fieldContainerBuilder.values(customFieldsJsonMap))
        .build();
  }

  @Nonnull
  @Override
  public ShoppingListUpdateAction buildSetCustomFieldAction(
      @Nullable final Long variantId,
      @Nullable final String objectId,
      @Nullable final String customFieldName,
      @Nullable final Object customFieldValue) {

    return ShoppingListSetCustomFieldActionBuilder.of()
        .name(customFieldName)
        .value(customFieldValue)
        .build();
  }
}
