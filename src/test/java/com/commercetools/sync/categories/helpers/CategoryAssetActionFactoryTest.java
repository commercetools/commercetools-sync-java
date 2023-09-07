package com.commercetools.sync.categories.helpers;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.CategoryAddAssetAction;
import com.commercetools.api.models.category.CategoryChangeAssetOrderAction;
import com.commercetools.api.models.category.CategoryDraft;
import com.commercetools.api.models.category.CategoryRemoveAssetAction;
import com.commercetools.api.models.category.CategorySetAssetTagsActionBuilder;
import com.commercetools.api.models.category.CategoryUpdateAction;
import com.commercetools.api.models.common.Asset;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryAssetActionFactoryTest {
  private CategoryAssetActionFactory categoryAssetActionFactory;

  @BeforeEach
  void setup() {
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

    categoryAssetActionFactory = new CategoryAssetActionFactory(syncOptions);
  }

  @Test
  void buildRemoveAssetAction_always_ShouldBuildCorrectAction() {
    final CategoryUpdateAction action = categoryAssetActionFactory.buildRemoveAssetAction("foo");
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(CategoryRemoveAssetAction.class);
    final CategoryRemoveAssetAction removeAsset = (CategoryRemoveAssetAction) action;
    assertThat(removeAsset.getAssetId()).isNull();
    assertThat(removeAsset.getAssetKey()).isEqualTo("foo");
  }

  @Test
  void buildChangeAssetOrderAction_always_ShouldBuildCorrectAction() {
    final CategoryUpdateAction action =
        categoryAssetActionFactory.buildChangeAssetOrderAction(emptyList());
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(CategoryChangeAssetOrderAction.class);
    final CategoryChangeAssetOrderAction changeAssetOrder = (CategoryChangeAssetOrderAction) action;
    assertThat(changeAssetOrder.getAssetOrder()).isEqualTo(emptyList());
  }

  @Test
  void buildAddAssetAction_always_ShouldBuildCorrectAction() {
    final AssetDraft assetDraft =
        AssetDraftBuilder.of().sources(emptyList()).name(ofEnglish("assetName")).build();

    final CategoryUpdateAction action =
        categoryAssetActionFactory.buildAddAssetAction(assetDraft, 0);
    assertThat(action).isNotNull();
    assertThat(action).isInstanceOf(CategoryAddAssetAction.class);
    final CategoryAddAssetAction addAsset = (CategoryAddAssetAction) action;
    assertThat(addAsset.getAsset().getName()).isEqualTo(ofEnglish("assetName"));
    assertThat(addAsset.getAsset().getSources()).isEqualTo(emptyList());
    assertThat(addAsset.getPosition()).isEqualTo(0);
  }

  @Test
  void buildAssetActions_always_ShouldBuildCorrectAction() {
    final List<String> newTags = Collections.singletonList("newTag");
    final Asset asset = mock(Asset.class);
    when(asset.getKey()).thenReturn("assetKey");
    when(asset.getName()).thenReturn(ofEnglish("assetName"));
    final AssetDraft assetDraft =
        AssetDraftBuilder.of()
            .key("assetKey")
            .name(ofEnglish("assetName"))
            .tags(newTags)
            .sources(Collections.emptyList())
            .build();
    CategoryDraft categoryDraft = mock(CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        categoryAssetActionFactory.buildAssetActions(categoryDraft, asset, assetDraft);

    assertThat(updateActions).isNotNull();
    assertThat(updateActions)
        .containsExactly(
            CategorySetAssetTagsActionBuilder.of().assetKey(asset.getKey()).tags(newTags).build());
  }
}
