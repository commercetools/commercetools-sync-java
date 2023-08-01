package com.commercetools.sync.sdk2.commons.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategorySetAssetCustomFieldAction;
import com.commercetools.api.models.category.CategorySetAssetCustomTypeAction;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.categories.helpers.AssetCustomActionBuilder;
import com.commercetools.sync.sdk2.commons.models.AssetCustomTypeAdapter;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryAssetCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
    final Asset asset = mock(Asset.class);
    when(asset.getKey()).thenReturn("assetKey");
    final String newCustomTypeId = UUID.randomUUID().toString();

    final CategoryUpdateAction updateAction =
        GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                AssetCustomTypeAdapter.of(asset),
                new AssetCustomActionBuilder(),
                1L,
                AssetCustomTypeAdapter::getId,
                ignore -> ResourceTypeId.ASSET.getJsonName(),
                AssetCustomTypeAdapter::getKey,
                CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(CategorySetAssetCustomTypeAction.class);
    CategorySetAssetCustomTypeAction categorySetAssetCustomTypeAction =
        (CategorySetAssetCustomTypeAction) updateAction;
    assertThat(categorySetAssetCustomTypeAction.getAssetId()).isEqualTo(asset.getId());
    assertThat(categorySetAssetCustomTypeAction.getAssetKey()).isEqualTo(asset.getKey());
    assertThat(categorySetAssetCustomTypeAction.getType().getId()).isEqualTo(newCustomTypeId);
  }

  @Test
  void buildRemoveCustomTypeAction_WithCategoryAsset_ShouldBuildChannelUpdateAction() {
    final String assetKey = "assetKey";

    final CategoryUpdateAction updateAction =
        new AssetCustomActionBuilder().buildRemoveCustomTypeAction(1L, assetKey);

    assertThat(updateAction).isInstanceOf(CategorySetAssetCustomTypeAction.class);
    CategorySetAssetCustomTypeAction categorySetAssetCustomTypeAction =
        (CategorySetAssetCustomTypeAction) updateAction;
    assertThat(categorySetAssetCustomTypeAction.getAssetId()).isEqualTo(null);
    assertThat(categorySetAssetCustomTypeAction.getAssetKey()).isEqualTo(null);
    assertThat(categorySetAssetCustomTypeAction.getType()).isEqualTo(null);
  }

  @Test
  void buildSetCustomFieldAction_WithCategoryAsset_ShouldBuildCategoryUpdateAction() {
    final String customFieldValue = "foo";
    final String customFieldName = "name";
    final String assetKey = "assetKey";
    final long variantId = 1;

    final CategoryUpdateAction updateAction =
        new AssetCustomActionBuilder()
            .buildSetCustomFieldAction(variantId, assetKey, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(CategorySetAssetCustomFieldAction.class);
    CategorySetAssetCustomFieldAction categorySetAssetCustomFieldAction =
        (CategorySetAssetCustomFieldAction) updateAction;
    assertThat(categorySetAssetCustomFieldAction.getAssetId()).isEqualTo(null);
    assertThat(categorySetAssetCustomFieldAction.getAssetKey()).isEqualTo(assetKey);
    assertThat(categorySetAssetCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(categorySetAssetCustomFieldAction.getValue()).isEqualTo(customFieldValue);
  }
}
