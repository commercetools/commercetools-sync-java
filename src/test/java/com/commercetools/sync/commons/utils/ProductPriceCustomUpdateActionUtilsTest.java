package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Price;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomField;
import io.sphere.sdk.products.commands.updateactions.SetProductPriceCustomType;
import org.junit.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedRemoveCustomTypeUpdateAction;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomFieldUpdateAction;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductPriceCustomUpdateActionUtilsTest {
    private ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithProductPrice_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction =
            buildTypedSetCustomTypeUpdateAction("key", new HashMap<>(), mock(Price.class), Product.class, 1,
                Price::getId, priceResource -> Price.resourceTypeId(), Price::getId, syncOptions)
                .orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithProductPrice_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Product> updateAction = buildTypedRemoveCustomTypeUpdateAction(mock(Price.class),
            Product.class, 1, Price::getId, priceResource -> Price.resourceTypeId(), Price::getId,
            syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetProductPriceCustomType.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithProductPrice_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction = buildTypedSetCustomFieldUpdateAction(
            "name", mock(JsonNode.class), mock(Price.class), Product.class, 1, Price::getId,
            priceResource -> Price.resourceTypeId(),
            priceResource -> null,
            syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetProductPriceCustomField.class);
    }
}
