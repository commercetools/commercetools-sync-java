package com.commercetools.sync.products.utils.productvariantupdateactionutils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetName;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import io.sphere.sdk.products.commands.updateactions.SetAssetSources;
import io.sphere.sdk.products.commands.updateactions.SetAssetTags;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAssetsUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BuildVariantAssetsUpdateActionsTest {

    private static final String RES_ROOT =
        "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/assets/";
    private static final String PRODUCT_WITH_ASSETS_ABC = RES_ROOT + "product-with-assets-abc.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABC = RES_ROOT + "product-draft-with-assets-abc.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES =
        RES_ROOT + "product-draft-with-assets-abc-with-changes.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_AB = RES_ROOT + "product-draft-with-assets-ab.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABCD = RES_ROOT + "product-draft-with-assets-abcd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABD = RES_ROOT + "product-draft-with-assets-abd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CAB = RES_ROOT + "product-draft-with-assets-cab.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CB = RES_ROOT + "product-draft-with-assets-cb.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ACBD = RES_ROOT + "product-draft-with-assets-acbd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ADBC = RES_ROOT + "product-draft-with-assets-adbc.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CBD = RES_ROOT + "product-draft-with-assets-cbd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES =
        RES_ROOT + "product-draft-with-assets-cbd-with-changes.json";
    private static final ProductSyncOptions SYNC_OPTIONS = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                    .build();

    @Test
    public void buildProductVariantAssetsUpdateActions_WithNullNewAssetsAndExistingAssets_ShouldBuild3RemoveActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);

        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();

        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant,
                ProductVariantDraftBuilder.of().build().withAssets(null), SYNC_OPTIONS);

        assertThat(updateActions).containsExactlyInAnyOrder(
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "a", true),
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "b", true),
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "c", true));
    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithNullNewAssetsAndNoOldAssets_ShouldNotBuildActions() {
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getAssets()).thenReturn(emptyList());

        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(productVariant,
                ProductVariantDraftBuilder.of().build().withAssets(null), SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithNewAssetsAndNoOldAssets_ShouldBuild3AddActions() {
        final ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getAssets()).thenReturn(emptyList());

        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABC, ProductDraft.class);

        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(productVariant, newProductDraft.getMasterVariant(), SYNC_OPTIONS);

        assertThat(updateActions).containsExactlyInAnyOrder(
            AddAsset.ofVariantId(productVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("a").tags(emptySet()).build()).withStaged(true).withPosition(0),
            AddAsset.ofVariantId(productVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("b").tags(emptySet()).build()).withStaged(true).withPosition(1),
            AddAsset.ofVariantId(productVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("c").tags(emptySet()).build()).withStaged(true).withPosition(2)
        );
    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithIdenticalAssets_ShouldNotBuildUpdateActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABC, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void
        buildProductVariantAssetsUpdateActions_WithSameAssetPositionButChangesWithin_ShouldBuildUpdateActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES,
            ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        final HashSet<String> expectedNewTags = new HashSet<>();
        expectedNewTags.add("new tag");

        assertThat(updateActions).containsExactlyInAnyOrder(
            ChangeAssetName.ofAssetKeyAndVariantId(oldMasterVariant.getId(), "a", ofEnglish("asset new name"), true),
            ChangeAssetName.ofAssetKeyAndVariantId(oldMasterVariant.getId(), "b", ofEnglish("asset new name 2"), true),
            ChangeAssetName.ofAssetKeyAndVariantId(oldMasterVariant.getId(), "c", ofEnglish("asset new name 3"), true),
            SetAssetTags.ofVariantIdAndAssetKey(oldMasterVariant.getId(), "a", expectedNewTags, true),
            SetAssetSources.ofVariantIdAndAssetKey(oldMasterVariant.getId(), "a",
                asList(AssetSourceBuilder.ofUri("new uri").build(),
                    AssetSourceBuilder.ofUri(null).key("new source").build()), true),
            SetAssetSources.ofVariantIdAndAssetKey(oldMasterVariant.getId(), "c",
                singletonList(AssetSourceBuilder.ofUri("uri").key("newKey").build()), true)
        );
    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithOneMissingAsset_ShouldBuildRemoveAssetAction() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_AB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "c", true)
        );
    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithOneExtraAsset_ShouldBuildAddAssetAction() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABCD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            AddAsset.ofVariantId(oldMasterVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build()).withStaged(true).withPosition(3)
        );
    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithOneAssetSwitch_ShouldBuildRemoveAndAddAssetActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "c", true),
            AddAsset.ofVariantId(oldMasterVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build()).withStaged(true).withPosition(2)
        );

    }

    @Test
    public void buildProductVariantAssetsUpdateActions_WithDifferent_ShouldBuildChangeAssetOrderAction() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CAB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            ChangeAssetOrder.ofVariantId(oldMasterVariant.getId(), asList("3", "1", "2"), true)
        );
    }

    @Test
    public void
        buildProductVariantAssetsUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "a", true),
            ChangeAssetOrder.ofVariantId(oldMasterVariant.getId(), asList("3", "2"), true)
        );
    }

    @Test
    public void
        buildProductVariantAssetsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ACBD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            ChangeAssetOrder.ofVariantId(oldMasterVariant.getId(), asList("1", "3", "2"), true),
            AddAsset.ofVariantId(oldMasterVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build()).withStaged(true).withPosition(3)
        );
    }

    @Test
    public void
        buildProductVariantAssetsUpdateActions_WithAddedAssetInBetween_ShouldBuildAddWithCorrectPositionActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ADBC, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            AddAsset.ofVariantId(oldMasterVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build()).withStaged(true).withPosition(1)
        );
    }

    @Test
    public void
        buildProductVariantAssetsUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAssetActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CBD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "a", true),
            ChangeAssetOrder.ofVariantId(oldMasterVariant.getId(), asList("3", "2"), true),
            AddAsset.ofVariantId(oldMasterVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build()).withStaged(true).withPosition(2)
        );
    }

    @Test
    public void
        buildProductVariantAssetsUpdateActions_WithAddedRemovedAndDifOrderAndNewName_ShouldBuildAllDiffAssetActions() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES,
            ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).containsExactly(
            RemoveAsset.ofVariantIdWithKey(oldMasterVariant.getId(), "a", true),
            ChangeAssetName.ofAssetKeyAndVariantId(oldMasterVariant.getId(), "c", ofEnglish("new name"), true),
            ChangeAssetOrder.ofVariantId(oldMasterVariant.getId(), asList("3", "2"), true),
            AddAsset.ofVariantId(oldMasterVariant.getId(),
                AssetDraftBuilder.of(singletonList(AssetSourceBuilder.ofUri("uri").build()), ofEnglish("asset name"))
                                 .key("d").tags(emptySet()).build()).withStaged(true).withPosition(2)
        );
    }
}
