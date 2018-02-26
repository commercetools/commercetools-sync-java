package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.ProductSyncOptions;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.Asset;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildChangeAssetOrderUpdateAction;
import static com.commercetools.sync.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAssetsUpdateActions;
import static io.sphere.sdk.json.SphereJsonUtils.readObjectFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BuildVariantAssetsUpdateActionsTest {
    private static final String RES_ROOT =
        "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/assets/";
    private static final String PRODUCT_WITH_ASSETS_ABC = RES_ROOT + "product-with-assets-abc.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABC = RES_ROOT + "product-draft-with-assets-abc.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_AB = RES_ROOT + "product-draft-with-assets-ab.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABCD = RES_ROOT + "product-draft-with-assets-abcd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ABD = RES_ROOT + "product-draft-with-assets-abd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CAB = RES_ROOT + "product-draft-with-assets-cab.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CB = RES_ROOT + "product-draft-with-assets-cb.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ACBD = RES_ROOT + "product-draft-with-assets-acbd.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_ADBC = RES_ROOT + "product-draft-with-assets-adbc.json";
    private static final String PRODUCT_DRAFT_WITH_ASSETS_CBD = RES_ROOT + "product-draft-with-assets-cbd.json";


    @Test
    public void buildChangeAssetOrderUpdateAction_WithDifferentOrder_ShouldBuildAction() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CAB, ProductDraft.class);


        final List<Asset> oldAssets = oldProduct.getMasterData().getStaged().getMasterVariant().getAssets();
        final List<AssetDraft> newAssetDrafts = newProductDraft.getMasterVariant().getAssets();

        final Optional<UpdateAction<Product>> changeAssetOrderUpdateAction =
            buildChangeAssetOrderUpdateAction(1, oldAssets, newAssetDrafts);

        assertThat(changeAssetOrderUpdateAction).isNotEmpty();
        assertThat(changeAssetOrderUpdateAction).containsInstanceOf(ChangeAssetOrder.class);
        final ChangeAssetOrder updateAction = (ChangeAssetOrder) changeAssetOrderUpdateAction.get();
        assertThat(updateAction.getAssetOrder()).containsExactly("3", "1", "2");
    }

    @Test
    public void testCase1() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABC, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions = buildProductVariantAssetsUpdateActions(oldMasterVariant,
            newMasterVariant, mock(ProductSyncOptions.class));

        assertThat(updateActions).isEmpty();
    }

    @Test
    public void testCase2() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_AB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions = buildProductVariantAssetsUpdateActions(oldMasterVariant,
            newMasterVariant, mock(ProductSyncOptions.class));

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(RemoveAsset.class);
    }
}
