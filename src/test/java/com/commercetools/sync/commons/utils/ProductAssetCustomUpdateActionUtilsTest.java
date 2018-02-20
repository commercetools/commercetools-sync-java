package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.helpers.AssetCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import org.junit.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductAssetCustomUpdateActionUtilsTest {

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithProductAsset_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction =
            buildTypedSetCustomTypeUpdateAction("key", new HashMap<>(), mock(Asset.class),
                new AssetCustomActionBuilder(), 1,
                Asset::getId, assetResource -> Asset.resourceTypeId(), Asset::getKey,
                ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build()).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildRemoveCustomTypeAction_WithProductAsset_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Product> updateAction =
            new AssetCustomActionBuilder().buildRemoveCustomTypeAction(1, "assetKey");

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildSetCustomFieldAction_WithProductAsset_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction = new AssetCustomActionBuilder()
            .buildSetCustomFieldAction(1, "assetKey", "customFieldName", mock(JsonNode.class));

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomField.class);
    }
}
