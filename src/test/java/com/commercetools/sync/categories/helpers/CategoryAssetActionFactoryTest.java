package com.commercetools.sync.categories.helpers;

import com.commercetools.sync.categories.CategorySyncOptions;
import com.commercetools.sync.categories.CategorySyncOptionsBuilder;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.categories.commands.updateactions.AddAsset;
import io.sphere.sdk.categories.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.categories.commands.updateactions.RemoveAsset;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.categories.commands.updateactions.SetAssetTags;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryAssetActionFactoryTest {
    private CategoryAssetActionFactory categoryAssetActionFactory;

    @Before
    public void setup() {
        final CategorySyncOptions syncOptions = CategorySyncOptionsBuilder.of(mock(SphereClient.class))
                                                                          .build();

        categoryAssetActionFactory = new CategoryAssetActionFactory(syncOptions);
    }

    @Test
    public void buildRemoveAssetAction_always_ShouldBuildCorrectAction() {
        final UpdateAction<Category> action = categoryAssetActionFactory.buildRemoveAssetAction("foo");
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(RemoveAsset.class);
        final RemoveAsset removeAsset = (RemoveAsset) action;
        assertThat(removeAsset.getAssetId()).isNull();
        assertThat(removeAsset.getAssetKey()).isEqualTo("foo");
    }

    @Test
    public void buildChangeAssetOrderAction_always_ShouldBuildCorrectAction() {
        final UpdateAction<Category> action = categoryAssetActionFactory.buildChangeAssetOrderAction(emptyList());
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(ChangeAssetOrder.class);
        final ChangeAssetOrder changeAssetOrder = (ChangeAssetOrder) action;
        assertThat(changeAssetOrder.getAssetOrder()).isEqualTo(emptyList());
    }

    @Test
    public void buildAddAssetAction_always_ShouldBuildCorrectAction() {
        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                       .build();


        final UpdateAction<Category> action = categoryAssetActionFactory.buildAddAssetAction(assetDraft, 0);
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(AddAsset.class);
        final AddAsset addAsset = (AddAsset) action;
        assertThat(addAsset.getAsset().getName()).isEqualTo(ofEnglish("assetName"));
        assertThat(addAsset.getAsset().getSources()).isEqualTo(emptyList());
        assertThat(addAsset.getPosition()).isEqualTo(0);
    }

    @Test
    public void buildAssetActions_always_ShouldBuildCorrectAction() {
        final Asset asset = mock(Asset.class);
        when(asset.getKey()).thenReturn("assetKey");
        when(asset.getName()).thenReturn(ofEnglish("assetName"));

        final HashSet<String> newTags = new HashSet<>();
        newTags.add("newTag");

        final AssetDraft assetDraft = AssetDraftBuilder.of(asset)
                                                       .tags(newTags)
                                                       .build();

        final List<UpdateAction<Category>> updateActions = categoryAssetActionFactory
            .buildAssetActions(asset, assetDraft);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).containsExactly(
            SetAssetTags.ofKey(asset.getKey(), newTags)
        );
    }
}
