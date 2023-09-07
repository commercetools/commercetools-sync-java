package com.commercetools.sync.products.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.product.ProductAddAssetAction;
import com.commercetools.api.models.product.ProductAddAssetActionImpl;
import com.commercetools.api.models.product.ProductChangeAssetOrderAction;
import com.commercetools.api.models.product.ProductChangeAssetOrderActionImpl;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductRemoveAssetAction;
import com.commercetools.api.models.product.ProductRemoveAssetActionImpl;
import com.commercetools.api.models.product.ProductSetAssetTagsActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductAssetActionFactoryTest {
  private ProductAssetActionFactory productAssetActionFactory;

  @BeforeEach
  void setup() {
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    productAssetActionFactory = new ProductAssetActionFactory(1L, syncOptions);
  }

  @Test
  void buildRemoveAssetAction_always_ShouldBuildCorrectAction() {
    final ProductUpdateAction action = productAssetActionFactory.buildRemoveAssetAction("foo");
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(ProductRemoveAssetActionImpl.class);
    final ProductRemoveAssetAction removeAsset = (ProductRemoveAssetAction) action;
    assertThat(removeAsset.getVariantId()).isEqualTo(1L);
    assertThat(removeAsset.getAssetKey()).isEqualTo("foo");
    assertThat(removeAsset.getStaged()).isTrue();
  }

  @Test
  void buildChangeAssetOrderAction_always_ShouldBuildCorrectAction() {
    final ProductUpdateAction action =
        productAssetActionFactory.buildChangeAssetOrderAction(emptyList());
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(ProductChangeAssetOrderActionImpl.class);
    final ProductChangeAssetOrderAction changeAssetOrder = (ProductChangeAssetOrderAction) action;
    assertThat(changeAssetOrder.getVariantId()).isEqualTo(1L);
    assertThat(changeAssetOrder.getAssetOrder()).isEqualTo(emptyList());
    assertThat(changeAssetOrder.getStaged()).isTrue();
  }

  @Test
  void buildAddAssetAction_always_ShouldBuildCorrectAction() {
    final AssetDraft assetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(ofEnglish("assetName")).build();

    final ProductUpdateAction action = productAssetActionFactory.buildAddAssetAction(assetDraft, 0);
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(ProductAddAssetActionImpl.class);
    final ProductAddAssetAction addAsset = (ProductAddAssetAction) action;
    assertThat(addAsset.getVariantId()).isEqualTo(1L);
    assertThat(addAsset.getAsset().getName()).isEqualTo(ofEnglish("assetName"));
    assertThat(addAsset.getAsset().getSources()).isEqualTo(emptyList());
    assertThat(addAsset.getPosition()).isEqualTo(0);
    assertThat(addAsset.getStaged()).isTrue();
  }

  @Test
  void buildAssetActions_always_ShouldBuildCorrectAction() {
    final ProductDraft mainProductDraft = mock(ProductDraft.class);
    final Asset asset = mock(Asset.class);
    when(asset.getKey()).thenReturn("assetKey");
    when(asset.getName()).thenReturn(ofEnglish("assetName"));

    final List<String> newTags = singletonList("newTag");

    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .key(asset.getKey())
            .name(asset.getName())
            .sources(emptyList())
            .tags(newTags)
            .build();

    final List<ProductUpdateAction> updateActions =
        productAssetActionFactory.buildAssetActions(mainProductDraft, asset, assetDraft);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions)
        .containsExactly(
            ProductSetAssetTagsActionBuilder.of()
                .variantId(1L)
                .assetKey(asset.getKey())
                .tags(newTags)
                .staged(true)
                .build());
  }
}
