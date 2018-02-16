package com.commercetools.sync.commons.utils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomField;
import io.sphere.sdk.categories.commands.updateactions.SetAssetCustomType;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import org.junit.Test;

import java.util.HashMap;

import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedRemoveCustomTypeUpdateAction;
import static com.commercetools.sync.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomFieldUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CategoryAssetCustomUpdateActionUtilsTest {
    private CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();

    @Test
    public void buildTypedSetCustomTypeUpdateAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction =
            GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction("key",
                new HashMap<>(), mock(Asset.class), Category.class, 1,
                Asset::getId, assetResource -> Asset.resourceTypeId(), Asset::getKey, syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildTypedRemoveCustomTypeUpdateAction_WithCategoryAsset_ShouldBuildChannelUpdateAction() {
        final UpdateAction<Category> updateAction = buildTypedRemoveCustomTypeUpdateAction(mock(Asset.class),
            Category.class, 1, Asset::getId, assetResource -> Asset.resourceTypeId(),
            Asset::getKey, syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomType.class);
    }

    @Test
    public void buildTypedSetCustomFieldUpdateAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
        final UpdateAction<Category> updateAction = buildTypedSetCustomFieldUpdateAction(
            "name", mock(JsonNode.class), mock(Asset.class), Category.class, 1, Asset::getId,
            assetResource -> Asset.resourceTypeId(),
            assetResource -> null,
            syncOptions).orElse(null);

        assertThat(updateAction).isNotNull();
        assertThat(updateAction).isInstanceOf(SetAssetCustomField.class);
    }
}
