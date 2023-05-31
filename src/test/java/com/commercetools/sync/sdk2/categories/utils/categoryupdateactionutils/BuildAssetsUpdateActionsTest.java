package com.commercetools.sync.sdk2.categories.utils.categoryupdateactionutils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.categories.utils.CategoryUpdateActionUtils.buildAssetsUpdateActions;
import static com.commercetools.sync.sdk2.commons.utils.AssetsUpdateActionUtils.ASSET_KEY_NOT_SET;
import static com.commercetools.sync.sdk2.commons.utils.TestUtils.readObjectFromResource;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.*;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSource;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.sync.sdk2.categories.CategorySyncOptions;
import com.commercetools.sync.sdk2.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildAssetsUpdateActionsTest {
  private static final String RES_ROOT =
      "com/commercetools/sync/categories/utils/categoryupdateactionutils/assets/";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY =
      RES_ROOT + "category-draft-with-assets-abc-without-key-for-c.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ABC =
      RES_ROOT + "category-draft-with-assets-abc.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ABB =
      RES_ROOT + "category-draft-with-assets-abb.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES =
      RES_ROOT + "category-draft-with-assets-abc-with-changes.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_AB =
      RES_ROOT + "category-draft-with-assets-ab.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ABCD =
      RES_ROOT + "category-draft-with-assets-abcd.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ABD =
      RES_ROOT + "category-draft-with-assets-abd.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_CAB =
      RES_ROOT + "category-draft-with-assets-cab.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_CB =
      RES_ROOT + "category-draft-with-assets-cb.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ACBD =
      RES_ROOT + "category-draft-with-assets-acbd.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_ADBC =
      RES_ROOT + "category-draft-with-assets-adbc.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_CBD =
      RES_ROOT + "category-draft-with-assets-cbd.json";
  private static final String CATEGORY_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES =
      RES_ROOT + "category-draft-with-assets-cbd-with-changes.json";

  private List<String> warningCallBackMessages;
  private CategorySyncOptions syncOptions;

  @BeforeEach
  void setupTest() {
    warningCallBackMessages = new ArrayList<>();
    syncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .warningCallback(
                (exception, newResource, oldResource) ->
                    warningCallBackMessages.add(exception.getMessage()))
            .build();
  }

  @Test
  void buildAssetsUpdateActions_WithNullNewAssetsAndExistingAssets_ShouldBuild3RemoveActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(
            oldCategory,
            CategoryDraftBuilder.of().name(ofEnglish("name")).slug(ofEnglish("slug")).build(),
            syncOptions);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            CategoryRemoveAssetActionBuilder.of().assetKey("a").build(),
            CategoryRemoveAssetActionBuilder.of().assetKey("b").build(),
            CategoryRemoveAssetActionBuilder.of().assetKey("c").build());
  }

  @Test
  void buildAssetsUpdateActions_WithNullNewAssetsAndNoOldAssets_ShouldNotBuildActions() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getAssets()).thenReturn(emptyList());

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(
            oldCategory,
            CategoryDraftBuilder.of().name(ofEnglish("name")).slug(ofEnglish("slug")).build(),
            syncOptions);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAssetsUpdateActions_WithNewAssetsAndNoOldAssets_ShouldBuild3AddActions() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getAssets()).thenReturn(emptyList());

    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("a")
                        .tags(emptyList())
                        .build())
                .position(0)
                .build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("b")
                        .tags(emptyList())
                        .build())
                .position(1)
                .build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("c")
                        .tags(emptyList())
                        .build())
                .position(2)
                .build());
  }

  @Test
  void
      buildAssetsUpdateActions_WithNewAssetsAndOneWithoutKeyAndNoOldAssets_ShouldTriggerWarningCallBack() {
    final Category oldCategory = mock(Category.class);
    when(oldCategory.getAssets()).thenReturn(emptyList());

    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("a")
                        .tags(emptyList())
                        .build())
                .position(0)
                .build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("b")
                        .tags(emptyList())
                        .build())
                .position(1)
                .build());
    assertThat(warningCallBackMessages.get(0))
        .isEqualTo(format(ASSET_KEY_NOT_SET, "name: {en=asset name}"));
  }

  @Test
  void
      buildAssetsUpdateActions_WithNewAssetsAndOneWithoutKeyAndOldAssets_ShouldTriggerWarningCallBack() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(CategoryRemoveAssetActionBuilder.of().assetKey("c").build());
    assertThat(warningCallBackMessages.get(0))
        .isEqualTo(format(ASSET_KEY_NOT_SET, "name: {en=asset name}"));
  }

  @Test
  void
      buildAssetsUpdateActions_WithNewAssetsAndOldAssetsAndOneWithoutKey_ShouldTriggerWarningCallBack() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("c")
                        .tags(emptyList())
                        .build())
                .position(2)
                .build());
    assertThat(warningCallBackMessages.get(0)).isEqualTo(format(ASSET_KEY_NOT_SET, "id: 3"));
  }

  @Test
  void buildAssetsUpdateActions_WithIdenticalAssets_ShouldNotBuildUpdateActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildAssetsUpdateActions_WithDuplicateAssetKeys_ShouldNotBuildActionsAndTriggerErrorCb() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABB, CategoryDraft.class);

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final CategorySyncOptions syncOptions =
        CategorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  if (exception.getCause() != null) {
                    errorMessages.add(exception.getCause().getMessage());
                  }
                  exceptions.add(exception.getCause());
                })
            .build();

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(2);
    assertThat(errorMessages.get(0))
        .matches(
            "Failed to build update actions for the assets of the category with "
                + "the key 'null'.");
    assertThat(errorMessages.get(1))
        .matches(
            ".*DuplicateKeyException: Supplied asset drafts have duplicate keys. "
                + "Asset keys are expected to be unique inside their container \\(a product variant or a category\\).");
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            "Supplied asset drafts have duplicate "
                + "keys. Asset keys are expected to be unique inside their container (a product variant or a category).");
    assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void buildAssetsUpdateActions_WithSameAssetPositionButChangesWithin_ShouldBuildUpdateActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    final List<String> expectedNewTags = new ArrayList<>();
    expectedNewTags.add("new tag");

    final AssetSource assetSourceWithNullUri =
        AssetSourceBuilder.of().uri("").key("new source").build();
    assetSourceWithNullUri.setUri(null);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            CategoryChangeAssetNameActionBuilder.of()
                .assetKey("a")
                .name(ofEnglish("asset new name"))
                .build(),
            CategoryChangeAssetNameActionBuilder.of()
                .assetKey("b")
                .name(ofEnglish("asset new name 2"))
                .build(),
            CategoryChangeAssetNameActionBuilder.of()
                .assetKey("c")
                .name(ofEnglish("asset new name 3"))
                .build(),
            CategorySetAssetTagsActionBuilder.of().assetKey("a").tags(expectedNewTags).build(),
            CategorySetAssetSourcesActionBuilder.of()
                .assetKey("a")
                .sources(
                    asList(AssetSourceBuilder.of().uri("new uri").build(), assetSourceWithNullUri))
                .build(),
            CategorySetAssetSourcesActionBuilder.of()
                .assetKey("c")
                .sources(singletonList(AssetSourceBuilder.of().uri("uri").key("newKey").build()))
                .build());
  }

  @Test
  void buildAssetsUpdateActions_WithOneMissingAsset_ShouldBuildRemoveAssetAction() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_AB, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(CategoryRemoveAssetActionBuilder.of().assetKey("c").build());
  }

  @Test
  void buildAssetsUpdateActions_WithOneExtraAsset_ShouldBuildAddAssetAction() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABCD, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(3)
                .build());
  }

  @Test
  void buildAssetsUpdateActions_WithOneAssetSwitch_ShouldBuildRemoveAndAddAssetActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABD, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryRemoveAssetActionBuilder.of().assetKey("c").build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(2)
                .build());
  }

  @Test
  void buildAssetsUpdateActions_WithDifferent_ShouldBuildChangeAssetOrderAction() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CAB, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryChangeAssetOrderActionBuilder.of().assetOrder(asList("3", "1", "2")).build());
  }

  @Test
  void
      buildAssetsUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CB, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryRemoveAssetActionBuilder.of().assetKey("a").build(),
            CategoryChangeAssetOrderActionBuilder.of().assetOrder(asList("3", "2")).build());
  }

  @Test
  void buildAssetsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ACBD, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryChangeAssetOrderActionBuilder.of().assetOrder(asList("1", "3", "2")).build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(3)
                .build());
  }

  @Test
  void buildAssetsUpdateActions_WithAddedAssetInBetween_ShouldBuildAddWithCorrectPositionActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ADBC, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(1)
                .build());
  }

  @Test
  void buildAssetsUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAssetActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CBD, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryRemoveAssetActionBuilder.of().assetKey("a").build(),
            CategoryChangeAssetOrderActionBuilder.of().assetOrder(asList("3", "2")).build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(2)
                .build());
  }

  @Test
  void
      buildAssetsUpdateActions_WithAddedRemovedAndDifOrderAndNewName_ShouldBuildAllDiffAssetActions() {
    final Category oldCategory =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
    final CategoryDraft newCategoryDraft =
        readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES, CategoryDraft.class);

    final List<CategoryUpdateAction> updateActions =
        buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

    assertThat(updateActions)
        .containsExactly(
            CategoryRemoveAssetActionBuilder.of().assetKey("a").build(),
            CategoryChangeAssetNameActionBuilder.of()
                .assetKey("c")
                .name(ofEnglish("new name"))
                .build(),
            CategoryChangeAssetOrderActionBuilder.of().assetOrder(asList("3", "2")).build(),
            CategoryAddAssetActionBuilder.of()
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(2)
                .build());
  }
}
