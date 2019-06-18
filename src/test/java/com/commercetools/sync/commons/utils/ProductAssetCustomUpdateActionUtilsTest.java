package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAssetCustomUpdateActionUtilsTest {

    @Test
    void buildTypedSetCustomTypeUpdateAction_WithProductAsset_ShouldBuildProductUpdateAction() {
        final Asset asset = mock(Asset.class);
        when(asset.getKey()).thenReturn("assetKey");
        final String newCustomTypeId = "key";
        final int variantId = 1;

        final UpdateAction<Product> updateAction =
            buildTypedSetCustomTypeUpdateAction(newCustomTypeId, new HashMap<>(), asset,
                new AssetCustomActionBuilder(), variantId,
                Asset::getId, assetResource -> Asset.resourceTypeId(), Asset::getKey,
                ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
        assertThat((SetAssetCustomType) updateAction)
            .hasValues("setAssetCustomType", asset.getId(), null, variantId, true, emptyMap(), ofId(newCustomTypeId));
    }

    @Test
    void buildRemoveCustomTypeAction_WithProductAsset_ShouldBuildChannelUpdateAction() {
        final int variantId = 1;
        final UpdateAction<Product> updateAction =
            new AssetCustomActionBuilder().buildRemoveCustomTypeAction(variantId, "assetKey");

        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
        assertThat((SetAssetCustomType) updateAction)
            .hasValues("setAssetCustomType", null, null, variantId, true, null, ofId(null));
    }

    @Test
    void buildSetCustomFieldAction_WithProductAsset_ShouldBuildProductUpdateAction() {
        final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
        final String customFieldName = "name";
        final String assetKey = "assetKey";
        final int variantId = 1;

        final UpdateAction<Product> updateAction = new AssetCustomActionBuilder()
            .buildSetCustomFieldAction(variantId, assetKey, customFieldName, customFieldValue);

        assertThat(updateAction).isInstanceOf(SetAssetCustomField.class);
        assertThat((SetAssetCustomField) updateAction)
            .hasValues("setAssetCustomField", null, null, variantId, true, customFieldName, customFieldValue);
    }
}
