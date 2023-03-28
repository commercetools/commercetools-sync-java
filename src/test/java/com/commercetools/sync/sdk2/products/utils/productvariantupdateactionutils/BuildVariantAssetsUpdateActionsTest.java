package com.commercetools.sync.sdk2.products.utils.productvariantupdateactionutils;

import static com.commercetools.api.models.common.LocalizedString.ofEnglish;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductDraftFromJson;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.sdk2.products.ProductSyncMockUtils.getBuilderWithProductTypeRefKey;
import static com.commercetools.sync.sdk2.products.utils.ProductVariantUpdateActionUtils.buildProductVariantAssetsUpdateActions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.product.ProductAddAssetActionBuilder;
import com.commercetools.api.models.product.ProductChangeAssetNameActionBuilder;
import com.commercetools.api.models.product.ProductChangeAssetOrderActionBuilder;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductRemoveAssetActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetSourcesActionBuilder;
import com.commercetools.api.models.product.ProductSetAssetTagsActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.sync.sdk2.commons.exceptions.BuildUpdateActionException;
import com.commercetools.sync.sdk2.commons.exceptions.DuplicateKeyException;
import com.commercetools.sync.sdk2.products.ProductSyncOptions;
import com.commercetools.sync.sdk2.products.ProductSyncOptionsBuilder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildVariantAssetsUpdateActionsTest {

  private static final String RES_ROOT =
      "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/assets/";
  private static final String PRODUCT_WITHOUT_ASSETS = RES_ROOT + "product-without-assets.json";
  private static final String PRODUCT_WITH_ASSETS_ABC = RES_ROOT + "product-with-assets-abc.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ABC =
      RES_ROOT + "product-draft-with-assets-abc.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ABB =
      RES_ROOT + "product-draft-with-assets-abb.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES =
      RES_ROOT + "product-draft-with-assets-abc-with-changes.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_AB =
      RES_ROOT + "product-draft-with-assets-ab.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ABCD =
      RES_ROOT + "product-draft-with-assets-abcd.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ABD =
      RES_ROOT + "product-draft-with-assets-abd.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_CAB =
      RES_ROOT + "product-draft-with-assets-cab.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_CB =
      RES_ROOT + "product-draft-with-assets-cb.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ACBD =
      RES_ROOT + "product-draft-with-assets-acbd.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_ADBC =
      RES_ROOT + "product-draft-with-assets-adbc.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_CBD =
      RES_ROOT + "product-draft-with-assets-cbd.json";
  private static final String PRODUCT_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES =
      RES_ROOT + "product-draft-with-assets-cbd-with-changes.json";
  private static final ProductSyncOptions SYNC_OPTIONS =
      ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build();

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithNullNewAssetsAndExistingAssets_ShouldBuild3RemoveActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    ProductVariantDraft variant = ProductVariantDraftBuilder.of().build();
    final ProductDraft newProduct =
        getBuilderWithProductTypeRefKey("productTypeKey").plusVariants(variant).build();
    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct,
            newProduct,
            oldMasterVariant,
            newProduct.getVariants().get(0),
            SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .staged(true)
                .build(),
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("b")
                .staged(true)
                .build(),
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("c")
                .staged(true)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithNullNewAssetsAndNoOldAssets_ShouldNotBuildActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductVariant productVariant = mock(ProductVariant.class);
    when(productVariant.getAssets()).thenReturn(emptyList());
    ProductVariantDraft variant = ProductVariantDraftBuilder.of().build();
    final ProductDraft newProduct =
        getBuilderWithProductTypeRefKey("productTypeKey").plusVariants(variant).build();
    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct,
            newProduct,
            productVariant,
            ProductVariantDraftBuilder.of().build(),
            SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void buildProductVariantAssetsUpdateActions_WithNewAssetsAndNoOldAssets_ShouldBuild3AddActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITHOUT_ASSETS);
    final ProductVariant productVariant = oldProduct.getMasterVariant();
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ABC);

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct,
            newProductDraft,
            productVariant,
            newProductDraft.getMasterVariant(),
            SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductAddAssetActionBuilder.of()
                .variantId(productVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset " + "name"))
                        .key("a")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(0)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(productVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset " + "name"))
                        .key("b")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(1)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(productVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset " + "name"))
                        .key("c")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(2)
                .build());
  }

  @Test
  void buildProductVariantAssetsUpdateActions_WithIdenticalAssets_ShouldNotBuildUpdateActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ABC);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions).isEmpty();
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithDuplicateAssetKeys_ShouldNotBuildActionsAndTriggerErrorCb() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ABB);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, syncOptions);

    assertThat(updateActions).isEmpty();
    assertThat(errorMessages).hasSize(1);
    assertThat(errorMessages.get(0))
        .matches(
            "Failed to build update actions for the assets of the product "
                + "variant with the sku 'mv-sku'. Reason: .*DuplicateKeyException: Supplied asset drafts have "
                + "duplicate keys. Asset keys are expected to be unique inside their container \\(a product"
                + " variant or a "
                + "category\\).");
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.get(0)).isExactlyInstanceOf(BuildUpdateActionException.class);
    assertThat(exceptions.get(0).getMessage())
        .contains(
            "Supplied asset drafts have duplicate "
                + "keys. Asset keys are expected to be unique inside their container (a product variant or a "
                + "category).");
    assertThat(exceptions.get(0).getCause()).isExactlyInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithSameAssetPositionButChangesWithin_ShouldBuildUpdateActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft =
        createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ABC_WITH_CHANGES);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    final List<String> expectedNewTags = singletonList("new tag");

    assertThat(updateActions)
        .containsExactlyInAnyOrder(
            ProductChangeAssetNameActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .name(ofEnglish("asset new name"))
                .staged(true)
                .build(),
            ProductChangeAssetNameActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("b")
                .name(ofEnglish("asset new name 2"))
                .staged(true)
                .build(),
            ProductChangeAssetNameActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("c")
                .name(ofEnglish("asset new name 3"))
                .staged(true)
                .build(),
            ProductSetAssetTagsActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .tags(expectedNewTags)
                .staged(true)
                .build(),
            ProductSetAssetSourcesActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .sources(
                    asList(
                        AssetSourceBuilder.of().uri("new uri").build(),
                        AssetSourceBuilder.of().uri("new source").build()))
                .staged(true)
                .build(),
            ProductSetAssetSourcesActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("c")
                .sources(singletonList(AssetSourceBuilder.of().uri("uri").key("newKey").build()))
                .staged(true)
                .build());
  }

  @Test
  void buildProductVariantAssetsUpdateActions_WithOneMissingAsset_ShouldBuildRemoveAssetAction() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_AB);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("c")
                .staged(true)
                .build());
  }

  @Test
  void buildProductVariantAssetsUpdateActions_WithOneExtraAsset_ShouldBuildAddAssetAction() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ABCD);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductAddAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(singletonList(AssetSourceBuilder.of().uri("uri").build()))
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(3)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithOneAssetSwitch_ShouldBuildRemoveAndAddAssetActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ABD);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("c")
                .staged(true)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(2)
                .build());
  }

  @Test
  void buildProductVariantAssetsUpdateActions_WithDifferent_ShouldBuildChangeAssetOrderAction() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_CAB);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductChangeAssetOrderActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetOrder(asList("3", "1", "2"))
                .staged(true)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithRemovedAndDifferentOrder_ShouldBuildChangeOrderAndRemoveActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_CB);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .staged(true)
                .build(),
            ProductChangeAssetOrderActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetOrder(asList("3", "2"))
                .staged(true)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithAddedAndDifferentOrder_ShouldBuildChangeOrderAndAddActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ACBD);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductChangeAssetOrderActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetOrder(asList("1", "3", "2"))
                .staged(true)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(3)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithAddedAssetInBetween_ShouldBuildAddWithCorrectPositionActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_ADBC);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductAddAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(1)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithAddedRemovedAndDifOrder_ShouldBuildAllThreeMoveAssetActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft = createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_CBD);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .staged(true)
                .build(),
            ProductChangeAssetOrderActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetOrder("3", "2")
                .staged(true)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .position(2)
                .staged(true)
                .build());
  }

  @Test
  void
      buildProductVariantAssetsUpdateActions_WithAddedRemovedAndDifOrderAndNewName_ShouldBuildAllDiffAssetActions() {
    final ProductProjection oldProduct = createProductFromJson(PRODUCT_WITH_ASSETS_ABC);
    final ProductDraft newProductDraft =
        createProductDraftFromJson(PRODUCT_DRAFT_WITH_ASSETS_CBD_WITH_CHANGES);

    final ProductVariant oldMasterVariant = oldProduct.getMasterVariant();
    final ProductVariantDraft newMasterVariant = newProductDraft.getMasterVariant();

    final List<ProductUpdateAction> updateActions =
        buildProductVariantAssetsUpdateActions(
            oldProduct, newProductDraft, oldMasterVariant, newMasterVariant, SYNC_OPTIONS);

    assertThat(updateActions)
        .containsExactly(
            ProductRemoveAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("a")
                .staged(true)
                .build(),
            ProductChangeAssetNameActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetKey("c")
                .name(ofEnglish("new name"))
                .staged(true)
                .build(),
            ProductChangeAssetOrderActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .assetOrder("3", "2")
                .staged(true)
                .build(),
            ProductAddAssetActionBuilder.of()
                .variantId(oldMasterVariant.getId())
                .asset(
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("uri").build())
                        .name(ofEnglish("asset name"))
                        .key("d")
                        .tags(emptyList())
                        .build())
                .staged(true)
                .position(2)
                .build());
  }
}
