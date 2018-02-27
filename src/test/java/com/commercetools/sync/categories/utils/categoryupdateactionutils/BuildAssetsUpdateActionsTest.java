package com.commercetools.sync.categories.utils.categoryupdateactionutils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.categories.CategoryDraftBuilder;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.categories.commands.updateactions.RemoveAsset;
import io.sphere.sdk.categories.commands.updateactions.SetAssetSources;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildAssetsUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildAssetsUpdateActionsTest {
    private static final String RES_ROOT =
        "com/commercetools/sync/categories/utils/categoryupdateactionutils/assets/";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ABC = RES_ROOT + "category-draft-with-assets-abc.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES =
        RES_ROOT + "category-draft-with-assets-abc-with-changes.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_AB = RES_ROOT + "category-draft-with-assets-ab.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ABCD = RES_ROOT + "category-draft-with-assets-abcd.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ABD = RES_ROOT + "category-draft-with-assets-abd.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_CAB = RES_ROOT + "category-draft-with-assets-cab.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_CB = RES_ROOT + "category-draft-with-assets-cb.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ACBD = RES_ROOT + "category-draft-with-assets-acbd.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ADBC = RES_ROOT + "category-draft-with-assets-adbc.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_CBD = RES_ROOT + "category-draft-with-assets-cbd.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES =
        RES_ROOT + "category-draft-with-assets-cbd-with-changes.json";
    private static final CategorySyncOptions SYNC_OPTIONS = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                      .build();

    @Test
    public void buildAssetsUpdateActions_WithNullNewAssetsAndExistingAssets_ShouldBuild3RemoveActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory,
                CategoryDraftBuilder.of(ofEnglish("name"), ofEnglish("slug")).assets(null).build(), SYNC_OPTIONS);

        assertThat(updateActions).containsExactlyInAnyOrder(
            RemoveAsset.ofKey("a"),
            RemoveAsset.ofKey("b"),
            RemoveAsset.ofKey("c"));
    }

    @Test
    public void buildAssetsUpdateActions_WithNullNewAssetsAndNoOldAssets_ShouldNotBuildActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getAssets()).thenReturn(emptyList());

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory,
                CategoryDraftBuilder.of(ofEnglish("name"), ofEnglish("slug")).assets(null).build(), SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildAssetsUpdateActions_WithNewAssetsAndNoOldAssets_ShouldBuild3AddActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getAssets()).thenReturn(emptyList());

        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactlyInAnyOrder(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("a").tags(emptySet()).build(), 0),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("b").tags(emptySet()).build(), 1),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("c").tags(emptySet()).build(), 2)
        );
    }

    @Test
    public void buildAssetsUpdateActions_WithIdenticalAssets_ShouldNotBuildUpdateActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, CategoryDraft.class);


        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildAssetsUpdateActions_WithSameAssetPositionButChangesWithin_ShouldBuildUpdateActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        final HashSet<String> expectedNewTags = new HashSet<>();
        expectedNewTags.add("new tag");

        assertThat(updateActions).containsExactlyInAnyOrder(
            ChangeAssetName.ofKey("a", ofEnglish("asset new name")),
            ChangeAssetName.ofKey("b", ofEnglish("asset new name 2")),
            ChangeAssetName.ofKey("c", ofEnglish("asset new name 3")),
            SetAssetTags.ofKey("a", expectedNewTags),
            SetAssetSources.ofKey("a",
                asList(AssetSourceBuilder.ofUri("new uri").build(),
                    AssetSourceBuilder.ofUri(null).key("new source").build())),
            SetAssetSources.ofKey("c",
                singletonList(AssetSourceBuilder.ofUri("uri").key("newKey").build()))
        );
    }

    @Test
    public void buildAssetsUpdateActions_WithOneMissingAsset_ShouldBuildRemoveAssetAction() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_AB,
            CategoryDraft.class);


        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(RemoveAsset.ofKey("c"));
    }

    @Test
    public void buildAssetsUpdateActions_WithOneExtraAsset_ShouldBuildAddAssetAction() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABCD, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d")
                                 .tags(emptySet())
                                 .build(), 3));
    }

    @Test
    public void buildAssetsUpdateActions_WithOneAssetSwitch_ShouldBuildRemoveAndAddAssetActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABD,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofKey("c"),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build(), 2)
        );

    }

    @Test
    public void buildAssetsUpdateActions_WithDifferent_ShouldBuildChangeAssetOrderAction() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CAB, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(ChangeAssetOrder.of(asList("3", "1", "2")));
    }

    @Test
    public void buildAssetsUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CB, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(RemoveAsset.ofKey("a"), ChangeAssetOrder.of(asList("3", "2")));
    }

    @Test
    public void buildAssetsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ACBD, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            ChangeAssetOrder.of(asList("1", "3", "2")),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build(), 3)
        );
    }

    @Test
    public void buildAssetsUpdateActions_WithAddedAssetInBetween_ShouldBuildAddWithCorrectPositionActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ADBC, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build(), 1)
        );
    }

    @Test
    public void buildAssetsUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAssetActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CBD, CategoryDraft.class);


        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofKey("a"),
            ChangeAssetOrder.of(asList("3", "2")),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build(), 2)
        );
    }

    @Test
    public void buildAssetsUpdateActions_WithAddedRemovedAndDifOrderAndNewName_ShouldBuildAllDiffAssetActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofKey("a"),
            ChangeAssetName.ofKey("c", ofEnglish("new name")),
            ChangeAssetOrder.of(asList("3", "2")),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build(), 2)
        );
    }
}
