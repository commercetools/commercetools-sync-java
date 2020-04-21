package com.commercetools.sync.categories.utils.categoryupdateactionutils;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import com.commercetools.sync.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.commons.exceptions.DuplicateKeyException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.commercetools.sync.categories.utils.CategoryUpdateActionUtils.buildAssetsUpdateActions;
import static com.commercetools.sync.commons.utils.AssetsUpdateActionUtils.ASSET_KEY_NOT_SET;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildAssetsUpdateActionsTest {
    private static final String RES_ROOT =
        "com/commercetools/sync/categories/utils/categoryupdateactionutils/assets/";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY = RES_ROOT
        + "category-draft-with-assets-abc-without-key-for-c.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ABC = RES_ROOT + "category-draft-with-assets-abc.json";
    private static final String CATEGORY_DRAFT_WITH_ASSETS_ABB = RES_ROOT + "category-draft-with-assets-abb.json";
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

    private List<String> warningCallBackMessages;
    private CategorySyncOptions syncOptions;

    @BeforeEach
    void setupTest() {
        warningCallBackMessages = new ArrayList<>();
        syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .warningCallback((warning) -> warningCallBackMessages.add(warning))
            .build();
    }

    @Test
    void buildAssetsUpdateActions_WithNullNewAssetsAndExistingAssets_ShouldBuild3RemoveActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory,
                CategoryDraftBuilder.of(ofEnglish("name"), ofEnglish("slug")).assets(null).build(), syncOptions);

        assertThat(updateActions).containsExactlyInAnyOrder(
            RemoveAsset.ofKey("a"),
            RemoveAsset.ofKey("b"),
            RemoveAsset.ofKey("c"));
    }

    @Test
    void buildAssetsUpdateActions_WithNullNewAssetsAndNoOldAssets_ShouldNotBuildActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getAssets()).thenReturn(emptyList());

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory,
                CategoryDraftBuilder.of(ofEnglish("name"), ofEnglish("slug")).assets(null).build(), syncOptions);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAssetsUpdateActions_WithNewAssetsAndNoOldAssets_ShouldBuild3AddActions() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getAssets()).thenReturn(emptyList());

        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

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
    void buildAssetsUpdateActions_WithNewAssetsAndOneWithoutKeyAndNoOldAssets_ShouldTriggerWarningCallBack() {
        final Category oldCategory = mock(Category.class);
        when(oldCategory.getAssets()).thenReturn(emptyList());

        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactlyInAnyOrder(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("a").tags(emptySet()).build(), 0),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("b").tags(emptySet()).build(), 1)


        );
        assertThat(warningCallBackMessages.get(0)).isEqualTo(format(ASSET_KEY_NOT_SET,
            "name: LocalizedString(en -> asset name)"));
    }


    @Test
    void buildAssetsUpdateActions_WithNewAssetsAndOneWithoutKeyAndOldAssets_ShouldTriggerWarningCallBack() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactlyInAnyOrder(RemoveAsset.ofKey("c"));
        assertThat(warningCallBackMessages.get(0)).isEqualTo(format(ASSET_KEY_NOT_SET,
            "name: LocalizedString(en -> asset name)"));
    }

    @Test
    void buildAssetsUpdateActions_WithNewAssetsAndOldAssetsAndOneWithoutKey_ShouldTriggerWarningCallBack() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_WITHOUT_KEY, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactlyInAnyOrder(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("c").tags(emptySet()).build(), 2)

        );
        assertThat(warningCallBackMessages.get(0)).isEqualTo(format(ASSET_KEY_NOT_SET, "id: 3" ));

    }




    @Test
    void buildAssetsUpdateActions_WithIdenticalAssets_ShouldNotBuildUpdateActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, CategoryDraft.class);


        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).isEmpty();
    }

    @Test
    void buildAssetsUpdateActions_WithDuplicateAssetKeys_ShouldNotBuildActionsAndTriggerErrorCb() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABB, CategoryDraft.class);

        final List<String> errorMessages = new ArrayList<>();
        final List<Throwable> exceptions = new ArrayList<>();
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((errorMessage, exception) -> {
                errorMessages.add(errorMessage);
                exceptions.add(exception);
            })
            .build();

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).isEmpty();
        assertThat(errorMessages).hasSize(1);
        assertThat(errorMessages.get(0)).matches("Failed to build update actions for the assets of the category with "
            + "the key 'null'. Reason: .*DuplicateKeyException: Supplied asset drafts have duplicate keys. Asset "
            + "keys are expected to be unique inside their container \\(a product variant or a category\\).");
        assertThat(exceptions).hasSize(1);
        assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
        assertThat(exceptions.get(0).getMessage()).contains("Supplied asset drafts have duplicate "
            + "keys. Asset keys are expected to be unique inside their container (a product variant or a category).");
        assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void buildAssetsUpdateActions_WithSameAssetPositionButChangesWithin_ShouldBuildUpdateActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

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
    void buildAssetsUpdateActions_WithOneMissingAsset_ShouldBuildRemoveAssetAction() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_AB,
            CategoryDraft.class);


        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(RemoveAsset.ofKey("c"));
    }

    @Test
    void buildAssetsUpdateActions_WithOneExtraAsset_ShouldBuildAddAssetAction() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABCD, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("d")
                    .tags(emptySet())
                    .build(), 3));
    }

    @Test
    void buildAssetsUpdateActions_WithOneAssetSwitch_ShouldBuildRemoveAndAddAssetActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABD,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofKey("c"),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("d").tags(emptySet()).build(), 2)
        );

    }

    @Test
    void buildAssetsUpdateActions_WithDifferent_ShouldBuildChangeAssetOrderAction() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CAB, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(ChangeAssetOrder.of(asList("3", "1", "2")));
    }

    @Test
    void buildAssetsUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CB, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(RemoveAsset.ofKey("a"), ChangeAssetOrder.of(asList("3", "2")));
    }

    @Test
    void buildAssetsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ACBD, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(
            ChangeAssetOrder.of(asList("1", "3", "2")),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("d").tags(emptySet()).build(), 3)
        );
    }

    @Test
    void buildAssetsUpdateActions_WithAddedAssetInBetween_ShouldBuildAddWithCorrectPositionActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ADBC, CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("d").tags(emptySet()).build(), 1)
        );
    }

    @Test
    void buildAssetsUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAssetActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft =
            readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CBD, CategoryDraft.class);


        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofKey("a"),
            ChangeAssetOrder.of(asList("3", "2")),
            AddAsset.of(
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                    .key("d").tags(emptySet()).build(), 2)
        );
    }

    @Test
    void buildAssetsUpdateActions_WithAddedRemovedAndDifOrderAndNewName_ShouldBuildAllDiffAssetActions() {
        final Category oldCategory = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_ABC, Category.class);
        final CategoryDraft newCategoryDraft = readObjectFromResource(CATEGORY_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES,
            CategoryDraft.class);

        final List<UpdateAction<Category>> updateActions =
            buildAssetsUpdateActions(oldCategory, newCategoryDraft, syncOptions);

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
