package com.commercetools.sync.commons.utils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart_discount.CartDiscount;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomFieldAction;
import com.commercetools.api.models.cart_discount.CartDiscountSetCustomTypeAction;
import com.commercetools.api.models.cart_discount.CartDiscountUpdateAction;
import com.commercetools.api.models.type.TypeResourceIdentifierBuilder;
import com.commercetools.sync.cartdiscounts.CartDiscountSyncOptionsBuilder;
import com.commercetools.sync.cartdiscounts.helpers.CartDiscountCustomActionBuilder;
import com.commercetools.sync.cartdiscounts.models.CartDiscountCustomTypeAdapter;
import com.commercetools.sync.commons.models.Custom;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartDiscountCustomUpdateActionUtilsTest {

  @Test
  void
      buildTypedSetCustomTypeUpdateAction_WithCartDiscountResource_ShouldBuildCartDiscountUpdateAction() {
    final String newCustomTypeId = UUID.randomUUID().toString();

    final CartDiscountUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                CartDiscountCustomTypeAdapter.of(mock(CartDiscount.class)),
                new CartDiscountCustomActionBuilder(),
                null,
                CartDiscountCustomTypeAdapter::getId,
                Custom::getTypeId,
                cartDiscountResource -> null,
                CartDiscountSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(CartDiscountSetCustomTypeAction.class);
    assertThat((CartDiscountSetCustomTypeAction) updateAction)
        .satisfies(
            cartDiscountSetCustomTypeAction -> {
              assertThat(cartDiscountSetCustomTypeAction.getType())
                  .isEqualTo(TypeResourceIdentifierBuilder.of().id(newCustomTypeId).build());
              assertThat(cartDiscountSetCustomTypeAction.getFields().values())
                  .isEqualTo(emptyMap());
            });
  }

  @Test
  void buildRemoveCustomTypeAction_WithCartDiscountResource_ShouldBuildCartDiscountUpdateAction() {
    final CartDiscountUpdateAction updateAction =
        new CartDiscountCustomActionBuilder().buildRemoveCustomTypeAction(null, null);

    assertThat(updateAction).isInstanceOf(CartDiscountSetCustomTypeAction.class);
    assertThat((CartDiscountSetCustomTypeAction) updateAction)
        .satisfies(
            cartDiscountSetCustomTypeAction -> {
              assertThat(cartDiscountSetCustomTypeAction.getType()).isEqualTo(null);
              assertThat(cartDiscountSetCustomTypeAction.getFields()).isEqualTo(null);
            });
  }

  @Test
  void buildSetCustomFieldAction_WithCartDiscountResource_ShouldBuildCartDiscountUpdateAction() {
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
    final String customFieldName = "name";

    final CartDiscountUpdateAction updateAction =
        new CartDiscountCustomActionBuilder()
            .buildSetCustomFieldAction(null, null, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(CartDiscountSetCustomFieldAction.class);
    assertThat((CartDiscountSetCustomFieldAction) updateAction)
        .satisfies(
            cartDiscountSetCustomTypeAction -> {
              assertThat(cartDiscountSetCustomTypeAction.getName()).isEqualTo(customFieldName);
              assertThat(cartDiscountSetCustomTypeAction.getValue()).isEqualTo(customFieldValue);
            });
  }
}
