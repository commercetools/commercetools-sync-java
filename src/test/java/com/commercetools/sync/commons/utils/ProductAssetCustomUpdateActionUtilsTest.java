package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.products.commands.updateactions.SetAssetCustomType;
import org.junit.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedRemoveCustomTypeUpdateAction;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomFieldUpdateAction;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProductAssetCustomUpdateActionUtilsTest {
    private ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithProductAsset_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction =
            buildTypedSetCustomTypeUpdateAction("key", new HashMap<>(), mock(Asset.class), Product.class, 1,
                Asset::getId, assetResource -> Asset.resourceTypeId(), Asset::getKey, syncOptions)
                .orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithProductAsset_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Product> updateAction = buildTypedRemoveCustomTypeUpdateAction(mock(Asset.class), Product.class, 1,
            Asset::getId, assetResource -> Asset.resourceTypeId(),
            Asset::getKey, syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithProductAsset_ShouldBuildProductUpdateAction() {
        final UpdateAction<Product> updateAction = buildTypedSetCustomFieldUpdateAction(
            "name", mock(JsonNode.class), mock(Asset.class), Product.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(),
            assetResource -> null,
            syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomField.class);
    }
}
