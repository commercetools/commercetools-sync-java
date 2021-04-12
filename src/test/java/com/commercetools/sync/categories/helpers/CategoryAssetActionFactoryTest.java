package com.commercetools.sync.categories.helpers;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.categories.commands.updateactions.RemoveAsset;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryAssetActionFactoryTest {
  private CategoryAssetActionFactory categoryAssetActionFactory;

  @BeforeEach
  void setup() {
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mock(SphereClient.class)).build();

    categoryAssetActionFactory = new CategoryAssetActionFactory(syncOptions);
  }

  @Test
  void buildRemoveAssetAction_always_ShouldBuildCorrectAction() {
    final UpdateAction<Category> action = categoryAssetActionFactory.buildRemoveAssetAction("foo");
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(RemoveAsset.class);
    final RemoveAsset removeAsset = (RemoveAsset) action;
    assertThat(removeAsset.getAssetId()).isNull();
    assertThat(removeAsset.getAssetKey()).isEqualTo("foo");
  }

  @Test
  void buildChangeAssetOrderAction_always_ShouldBuildCorrectAction() {
    final UpdateAction<Category> action =
        categoryAssetActionFactory.buildChangeAssetOrderAction(emptyList());
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(ChangeAssetOrder.class);
    final ChangeAssetOrder changeAssetOrder = (ChangeAssetOrder) action;
    assertThat(changeAssetOrder.getAssetOrder()).isEqualTo(emptyList());
  }

  @Test
  void buildAddAssetAction_always_ShouldBuildCorrectAction() {
    final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName")).build();

    final UpdateAction<Category> action =
        categoryAssetActionFactory.buildAddAssetAction(assetDraft, 0);
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(AddAsset.class);
    final AddAsset addAsset = (AddAsset) action;
    assertThat(addAsset.getAsset().getName()).isEqualTo(ofEnglish("assetName"));
    assertThat(addAsset.getAsset().getSources()).isEqualTo(emptyList());
    assertThat(addAsset.getPosition()).isEqualTo(0);
  }

  @Test
  void buildAssetActions_always_ShouldBuildCorrectAction() {
    final HashSet<String> newTags = new HashSet<>();
    newTags.add("newTag");
    final Asset asset = mock(Asset.class);
    when(asset.getKey()).thenReturn("assetKey");
    when(asset.getName()).thenReturn(ofEnglish("assetName"));
    final AssetDraft assetDraft = AssetDraftBuilder.of(asset).tags(newTags).build();
    CategoryDraft categoryDraft = mock(CategoryDraft.class);
    final List<UpdateAction<Category>> updateActions =
        categoryAssetActionFactory.buildAssetActions(categoryDraft, asset, assetDraft);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions).containsExactly(SetAssetTags.ofKey(asset.getKey(), newTags));
  }
}
