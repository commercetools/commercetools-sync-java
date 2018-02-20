package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.AssetCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategoryAssetCustomUpdateActionUtilsTest {

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction("key",
                new HashMap<>(), mock(Asset.class), new AssetCustomActionBuilder(), 1,
                Asset::getId, assetResource -> Asset.resourceTypeId(), Asset::getKey,
                CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build())
                                    .orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildRemoveCustomTypeAction_WithCategoryAsset_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Category> updateAction =
            new AssetCustomActionBuilder().buildRemoveCustomTypeAction(1, "assetKey");

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildSetCustomFieldAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction = new AssetCustomActionBuilder()
            .buildSetCustomFieldAction(1, "assetKey", "name", mock(JsonNode.class));

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomField.class);
    }
}
