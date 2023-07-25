package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.product.ProductSetProductPriceCustomFieldAction;
import com.commercetools.api.models.product.ProductSetProductPriceCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.PriceCustomActionBuilder;
import com.commercetools.sync.sdk2.products.models.PriceCustomTypeAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductPriceCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithProductPrice_ShouldBuildProductUpdateAction() {
    final Price price = mock(Price.class);
    when(price.getId()).thenReturn("priceId");
    final String newCustomTypeId = UUID.randomUUID().toString();

    final ProductUpdateAction updateAction =
        buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                PriceCustomTypeAdapter.of(price),
                new PriceCustomActionBuilder(),
                1L,
                PriceCustomTypeAdapter::getId,
                ignore -> ResourceTypeId.PRODUCT_PRICE.getJsonName(),
                PriceCustomTypeAdapter::getId,
                ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(ProductSetProductPriceCustomTypeAction.class);
    assertThat((ProductSetProductPriceCustomTypeAction) updateAction)
        .satisfies(
            productSetProductPriceCustomTypeAction -> {
              assertThat(productSetProductPriceCustomTypeAction.getPriceId()).isEqualTo("priceId");
              assertThat(productSetProductPriceCustomTypeAction.getType().getId())
                  .isEqualTo(newCustomTypeId);
              assertThat(productSetProductPriceCustomTypeAction.getFields().values())
                  .isEqualTo(emptyMap());
              assertThat(productSetProductPriceCustomTypeAction.getStaged()).isTrue();
            });
  }

  @Test
  void buildRemoveCustomTypeAction_WithProductPrice_ShouldBuildChannelUpdateAction() {
    final String priceId = "1";
    final ProductUpdateAction updateAction =
        new PriceCustomActionBuilder().buildRemoveCustomTypeAction(1L, priceId);

    assertThat(updateAction).isInstanceOf(ProductSetProductPriceCustomTypeAction.class);
    assertThat((ProductSetProductPriceCustomTypeAction) updateAction)
        .satisfies(
            productSetProductPriceCustomTypeAction -> {
              assertThat(productSetProductPriceCustomTypeAction.getPriceId()).isEqualTo(priceId);
              assertThat(productSetProductPriceCustomTypeAction.getType()).isEqualTo(null);
            });
  }

  @Test
  void buildSetCustomFieldAction_WithProductPrice_ShouldBuildProductUpdateAction() {
    final String priceId = "1";
    final String customFieldName = "name";
    final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");

    final ProductUpdateAction updateAction =
        new PriceCustomActionBuilder()
            .buildSetCustomFieldAction(1L, priceId, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(ProductSetProductPriceCustomFieldAction.class);
    assertThat((ProductSetProductPriceCustomFieldAction) updateAction)
        .satisfies(
            productSetProductPriceCustomFieldAction -> {
              assertThat(productSetProductPriceCustomFieldAction.getPriceId()).isEqualTo(priceId);
              assertThat(productSetProductPriceCustomFieldAction.getName())
                  .isEqualTo(customFieldName);
              assertThat(productSetProductPriceCustomFieldAction.getValue())
                  .isEqualTo(customFieldValue);
              assertThat(productSetProductPriceCustomFieldAction.getStaged()).isTrue();
            });
  }
}
