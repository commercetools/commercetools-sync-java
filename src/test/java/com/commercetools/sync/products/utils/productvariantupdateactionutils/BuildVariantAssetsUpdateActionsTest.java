package com.commercetools.sync.products.utils.productvariantupdateactionutils;

import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.ChangeAssetOrder;
import io.sphere.sdk.products.commands.updateactions.RemoveAsset;
import org.junit.Test;

import java.util.List;

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
    private static final ProductSyncOptions SYNC_OPTIONS = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                                    .build();

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
    public void testCase2() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_AB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(RemoveAsset.class);
    }

    @Test
    public void testCase3() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABCD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(AddAsset.class);
        assertThat(((AddAsset)updateActions.get(0)).getPosition()).isEqualTo(3);
    }

    @Test
    public void testCase4() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ABD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(2);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(RemoveAsset.class);
        assertThat(updateActions.get(1)).isExactlyInstanceOf(AddAsset.class);
        assertThat(((AddAsset)updateActions.get(1)).getPosition()).isEqualTo(2);
    }

    @Test
    public void testCase5() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CAB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(ChangeAssetOrder.class);
    }

    @Test
    public void testCase6() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CB, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(2);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(RemoveAsset.class);
        assertThat(updateActions.get(1)).isExactlyInstanceOf(ChangeAssetOrder.class);
    }

    @Test
    public void testCase7() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ACBD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(2);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(ChangeAssetOrder.class);
        assertThat(updateActions.get(1)).isExactlyInstanceOf(AddAsset.class);
        assertThat(((AddAsset)updateActions.get(1)).getPosition()).isEqualTo(3);
    }

    @Test
    public void testCase8() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_ADBC, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(1);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(AddAsset.class);
        assertThat(((AddAsset)updateActions.get(0)).getPosition()).isEqualTo(1);
    }

    @Test
    public void testCase9() {
        final Product oldProduct = readObjectFromResource(PRODUCT_WITH_ASSETS_ABC, Product.class);
        final ProductDraft newProductDraft = readObjectFromResource(PRODUCT_DRAFT_WITH_ASSETS_CBD, ProductDraft.class);


        final ProductVariant oldMasterVariant = oldProduct.getMasterData().getStaged().getMasterVariant();
        final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();


        final List<UpdateAction<Product>> updateActions =
            buildProductVariantAssetsUpdateActions(oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

        assertThat(updateActions).isNotEmpty();
        assertThat(updateActions).hasSize(3);
        assertThat(updateActions.get(0)).isExactlyInstanceOf(RemoveAsset.class);
        assertThat(updateActions.get(1)).isExactlyInstanceOf(ChangeAssetOrder.class);
        assertThat(updateActions.get(2)).isExactlyInstanceOf(AddAsset.class);
        assertThat(((AddAsset)updateActions.get(2)).getPosition()).isEqualTo(2);
    }
}
