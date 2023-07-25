package com.commercetools.sync.sdk2.commons.utils;

import static com.commercetools.sync.sdk2.commons.utils.GenericUpdateActionUtils.buildTypedSetCustomTypeUpdateAction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.product.ProductSetAssetCustomFieldAction;
import com.commercetools.api.models.product.ProductSetAssetCustomTypeAction;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.sync.sdk2.commons.models.AssetCustomTypeAdapter;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.sdk2.products.helpers.AssetCustomActionBuilder;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductAssetCustomUpdateActionUtilsTest {

  @Test
  void buildTypedSetCustomTypeUpdateAction_WithProductAsset_ShouldBuildProductUpdateAction() {
    final Asset asset = mock(Asset.class);
    when(asset.getKey()).thenReturn("assetKey");
    final String newCustomTypeId = UUID.randomUUID().toString();
    final long variantId = 1L;

    final ProductUpdateAction updateAction =
        buildTypedSetCustomTypeUpdateAction(
                newCustomTypeId,
                new HashMap<>(),
                AssetCustomTypeAdapter.of(asset),
                new AssetCustomActionBuilder(),
                variantId,
                AssetCustomTypeAdapter::getId,
                ignore -> ResourceTypeId.ASSET.getJsonName(),
                AssetCustomTypeAdapter::getKey,
                ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build())
            .orElse(null);

    assertThat(updateAction).isInstanceOf(ProductSetAssetCustomTypeAction.class);
    ProductSetAssetCustomTypeAction productSetAssetCustomTypeAction =
        (ProductSetAssetCustomTypeAction) updateAction;
    assertThat(productSetAssetCustomTypeAction.getAssetId()).isEqualTo(asset.getId());
    assertThat(productSetAssetCustomTypeAction.getAssetKey()).isEqualTo(asset.getKey());
    assertThat(productSetAssetCustomTypeAction.getType().getId()).isEqualTo(newCustomTypeId);
  }

  @Test
  void buildRemoveCustomTypeAction_WithProductAsset_ShouldBuildProductUpdateAction() {
    final String assetKey = "assetKey";

    final ProductUpdateAction updateAction =
        new AssetCustomActionBuilder().buildRemoveCustomTypeAction(1L, assetKey);

    assertThat(updateAction).isInstanceOf(ProductSetAssetCustomTypeAction.class);
    ProductSetAssetCustomTypeAction productSetAssetCustomTypeAction =
        (ProductSetAssetCustomTypeAction) updateAction;
    assertThat(productSetAssetCustomTypeAction.getAssetId()).isEqualTo(null);
    assertThat(productSetAssetCustomTypeAction.getAssetKey()).isEqualTo(null);
    assertThat(productSetAssetCustomTypeAction.getType()).isEqualTo(null);
  }

  @Test
  void buildSetCustomFieldAction_WithProductAsset_ShouldBuildProductUpdateAction() {
    final String customFieldValue = "foo";
    final String customFieldName = "name";
    final String assetKey = "assetKey";
    final long variantId = 1;

    final ProductUpdateAction updateAction =
        new AssetCustomActionBuilder()
            .buildSetCustomFieldAction(variantId, assetKey, customFieldName, customFieldValue);

    assertThat(updateAction).isInstanceOf(ProductSetAssetCustomFieldAction.class);
    ProductSetAssetCustomFieldAction productSetAssetCustomFieldAction =
        (ProductSetAssetCustomFieldAction) updateAction;
    assertThat(productSetAssetCustomFieldAction.getAssetId()).isEqualTo(null);
    assertThat(productSetAssetCustomFieldAction.getAssetKey()).isEqualTo(assetKey);
    assertThat(productSetAssetCustomFieldAction.getName()).isEqualTo(customFieldName);
    assertThat(productSetAssetCustomFieldAction.getValue()).isEqualTo(customFieldValue);
  }
}
