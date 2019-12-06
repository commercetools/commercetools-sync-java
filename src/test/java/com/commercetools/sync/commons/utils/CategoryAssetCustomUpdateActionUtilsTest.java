package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.categories.helpers.AssetCustomActionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static com.commercetools.sync.commons.asserts.actions.AssertionsForUpdateActions.assertThat;
import static io.sphere.sdk.models.ResourceIdentifier.ofId;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryAssetCustomUpdateActionUtilsTest {

    @Test
    void buildTypedSetCustomTypeUpdateAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
        final Asset asset = mock(Asset.class);
        when(asset.getKey()).thenReturn("assetKey");
        final String newCustomTypeId = UUID.randomUUID().toString();

        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(newCustomTypeId,
                new HashMap<>(), asset, new AssetCustomActionBuilder(), 1,
                Asset::getId, assetResource -> Asset.resourceTypeId(), Asset::getKey,
                CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build())
                                    .orElse(null);

        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
        assertThat((SetAssetCustomType) updateAction)
            .hasValues("setAssetCustomType", asset.getId(), asset.getKey(), emptyMap(), ofId(newCustomTypeId));
    }

    @Test
    void buildRemoveCustomTypeAction_WithCategoryAsset_ShouldBuildChannelUpdateAction() {
        final String assetKey = "assetKey";

        final UpdateAction<Category> updateAction =
            new AssetCustomActionBuilder().buildRemoveCustomTypeAction(1, assetKey);

        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
        assertThat((SetAssetCustomType) updateAction)
            .hasValues("setAssetCustomType", null, assetKey,  null, ofId(null));
    }

    @Test
    void buildSetCustomFieldAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
        final JsonNode customFieldValue = JsonNodeFactory.instance.textNode("foo");
        final String customFieldName = "name";
        final String assetKey = "assetKey";
        final int variantId = 1;

        final UpdateAction<Category> updateAction = new AssetCustomActionBuilder()
            .buildSetCustomFieldAction(variantId, assetKey, customFieldName, customFieldValue);

        assertThat(updateAction).isInstanceOf(SetAssetCustomField.class);
        assertThat((SetAssetCustomField) updateAction)
            .hasValues("setAssetCustomField", null, assetKey, customFieldName, customFieldValue);
    }
}
