package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.PriceCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import org.junit.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductPriceCustomUpdateActionUtilsTest {

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithProductPrice_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction =
            buildTypedSetCustomTypeUpdateAction("key", new HashMap<>(), mock(Price.class),
                new PriceCustomActionBuilder(), 1, Price::getId, priceResource -> Price.resourceTypeId(),
                Price::getId, ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildRemoveCustomTypeAction_WithProductPrice_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Product> updateAction = new PriceCustomActionBuilder().buildRemoveCustomTypeAction(1, "1");

        assertThat(updateAction).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildSetCustomFieldAction_WithProductPrice_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction = new PriceCustomActionBuilder()
            .buildSetCustomFieldAction(1, "1", "name", mock(JsonNode.class));

        assertThat(updateAction).isInstanceOf(SetProductPriceCustomField.class);
    }
}
