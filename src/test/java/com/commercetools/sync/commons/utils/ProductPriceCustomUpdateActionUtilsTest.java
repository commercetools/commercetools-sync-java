package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.PriceCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductPriceCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithProductPrice_ShouldBuildProductUpdateAction() {
    final Price price = mock(Price.class);
    when(price.getId()).thenReturn("priceId");
    final String newCustomTypeId = UUID.randomUUID().toString();

    final UpdateAction<Product> updateAction =
        buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                price,
                new PriceCustomActionBuilder(),
                1,
                Price::getId,
                priceResource -> Price.resourceTypeId(),
                Price::getId,
                ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(SetProductPriceCustomType.class);
    assertThat((SetProductPriceCustomType) updateAction)
        .hasValues(
            "setProductPriceCustomType", price.getId(), true, emptyMap(), ofId(newCustomTypeId));
  }

  @Test
  void buildRemoveCustomTypeAction_WithProductPrice_ShouldBuildChannelUpdateAction() {
    final String priceId = "1";
    final UpdateAction<Product> updateAction =
        new PriceCustomActionBuilder().buildRemoveCustomTypeAction(1, priceId);

    assertThat(updateAction).isInstanceOf(SetProductPriceCustomType.class);
    assertThat((SetProductPriceCustomType) updateAction)
        .hasValues("setProductPriceCustomType", priceId, true, null, ofId(null));
  }

  @Test
  void buildSetCustomFieldAction_WithProductPrice_ShouldBuildProductUpdateAction() {
    final String priceId = "1";
    final String customFieldName = "name";
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");

    final UpdateAction<Product> updateAction =
        new PriceCustomActionBuilder()
            .buildSetCustomFieldAction(1, priceId, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(SetProductPriceCustomField.class);
    assertThat((SetProductPriceCustomField) updateAction)
        .hasValues("setProductPriceCustomField", true, priceId, customFieldName, customFieldValue);
  }
}
