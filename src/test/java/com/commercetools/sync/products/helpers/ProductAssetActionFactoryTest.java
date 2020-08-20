package com.commercetools.sync.products.helpers;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAssetActionFactoryTest {
    private ProductAssetActionFactory productAssetActionFactory;

    @BeforeEach
    void setup() {
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                        .build();

        productAssetActionFactory = new ProductAssetActionFactory(1, syncOptions);
    }

    @Test
    void buildRemoveAssetAction_always_ShouldBuildCorrectAction() {
        final UpdateAction<Product> action = productAssetActionFactory.buildRemoveAssetAction("foo");
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(RemoveAsset.class);
        final RemoveAsset removeAsset = (RemoveAsset) action;
        assertThat(removeAsset.getVariantId()).isEqualTo(1);
        assertThat(removeAsset.getAssetKey()).isEqualTo("foo");
        assertThat(removeAsset.isStaged()).isTrue();
    }

    @Test
    void buildChangeAssetOrderAction_always_ShouldBuildCorrectAction() {
        final UpdateAction<Product> action = productAssetActionFactory.buildChangeAssetOrderAction(emptyList());
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(ChangeAssetOrder.class);
        final ChangeAssetOrder changeAssetOrder = (ChangeAssetOrder) action;
        assertThat(changeAssetOrder.getVariantId()).isEqualTo(1);
        assertThat(changeAssetOrder.getAssetOrder()).isEqualTo(emptyList());
        assertThat(changeAssetOrder.isStaged()).isTrue();
    }

    @Test
    void buildAddAssetAction_always_ShouldBuildCorrectAction() {
        final AssetDraft assetDraft = AssetDraftBuilder.of(emptyList(), ofEnglish("assetName"))
                                                       .build();


        final UpdateAction<Product> action = productAssetActionFactory.buildAddAssetAction(assetDraft, 0);
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(AddAsset.class);
        final AddAsset addAsset = (AddAsset) action;
        assertThat(addAsset.getVariantId()).isEqualTo(1);
        assertThat(addAsset.getAsset().getName()).isEqualTo(ofEnglish("assetName"));
        assertThat(addAsset.getAsset().getSources()).isEqualTo(emptyList());
        assertThat(addAsset.getPosition()).isEqualTo(0);
        assertThat(addAsset.isStaged()).isTrue();
    }

    @Test
    void buildAssetActions_always_ShouldBuildCorrectAction() {
        final Product mainProduct = mock(Product.class);
        final ProductDraft mainProductDraft = mock(ProductDraft.class);
        final Asset asset = mock(Asset.class);
        when(asset.getKey()).thenReturn("assetKey");
        when(asset.getName()).thenReturn(ofEnglish("assetName"));

        final HashSet<String> newTags = new HashSet<>();
        newTags.add("newTag");

        final AssetDraft assetDraft = AssetDraftBuilder.of(asset)
                                                       .tags(newTags)
                                                       .build();

        final List<UpdateAction<Product>> updateActions = productAssetActionFactory
            .buildAssetActions(mainProduct, mainProductDraft, asset, assetDraft);

        assertThat(updateActions).isNotNull();
        assertThat(updateActions).containsExactly(
            SetAssetTags.ofVariantIdAndAssetKey(1, asset.getKey(), newTags, true)
        );
    }
}
