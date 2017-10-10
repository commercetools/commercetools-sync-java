package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.SyncFilter;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductData;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.ProductVariantDraftDsl;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.commercetools.sync.commons.utils.CollectionUtils.collectionToMap;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftFromJson;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeMasterVariantUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildRemoveVariantUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ProductUpdateActionUtilsTest {

    private static final String RES_ROOT = "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/";
    private static final String OLD_PROD_WITH_VARIANTS = RES_ROOT + "productOld.json";

    // this product's variants don't contain old master variant
    private static final String NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER =
        RES_ROOT + "productDraftNew_changeRemoveMasterVariant.json";

    // this product's variants contain old master variant, but not as master any more
    private static final String NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER =
        RES_ROOT + "productDraftNew_moveMasterVariant.json";

    @Test
    public void buildVariantsUpdateActions_updatesVariants() throws Exception {
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

        final ProductSyncOptions productSyncOptions = mock(ProductSyncOptions.class);
        when(productSyncOptions.getSyncFilter()).thenReturn(SyncFilter.of());

        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeMetaData priceInfo = mock(AttributeMetaData.class);
        when(priceInfo.getName()).thenReturn("priceInfo");
        when(priceInfo.isRequired()).thenReturn(false);
        attributesMetaData.put("priceInfo", priceInfo);

        final List<UpdateAction<Product>> updateActions =
            buildVariantsUpdateActions(productOld, productDraftNew, productSyncOptions, attributesMetaData);

        // check remove variants are the first in the list, but not the master variant
        assertThat(updateActions.subList(0, 2))
            .containsExactlyInAnyOrder(RemoveVariant.of(2), RemoveVariant.of(3));

        // check add actions
        ProductVariantDraft draftMaster = productDraftNew.getMasterVariant();
        ProductVariantDraft draft5 = productDraftNew.getVariants().get(1);
        ProductVariantDraft draft6 = productDraftNew.getVariants().get(2);
        assertThat(updateActions).contains(
            buildAddVariantUpdateActionFromDraft(draftMaster),
            buildAddVariantUpdateActionFromDraft(draft5),
            buildAddVariantUpdateActionFromDraft(draft6));

        // variant 4 sku change
        assertThat(updateActions).containsOnlyOnce(SetSku.of(4, "var-44-sku", true));

        // verify image update of variant 4
        RemoveImage removeImage = RemoveImage.ofVariantId(4, "https://xxx.ggg/4.png", true);
        AddExternalImage addExternalImage =
            AddExternalImage.ofVariantId(4, productDraftNew.getVariants().get(0).getImages().get(0), true);
        assertThat(updateActions).containsOnlyOnce(removeImage);
        assertThat(updateActions).containsOnlyOnce(addExternalImage);
        assertThat(updateActions.indexOf(removeImage))
            .withFailMessage("Remove image action must be executed before add image action")
            .isLessThan(updateActions.indexOf(addExternalImage));

        // verify attributes changes
        assertThat(updateActions).contains(SetAttribute.ofVariantId(4, "priceInfo", "44/kg", true));

        // change master variant must be always after variants are added/updated,
        // because it is set by SKU and we should be sure the master variant is already added and SKUs are actual.
        // Also, master variant should be removed because it is missing in NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER
        final int size = updateActions.size();
        assertThat(updateActions.subList(size - 2, size)).containsExactly(
            ChangeMasterVariant.ofSku("var-7-sku", true),
            RemoveVariant.of(productOld.getMasterData().getStaged().getMasterVariant()));
    }

    @Test
    public void buildVariantsUpdateActions_doesNotRemoveMaster() throws Exception {
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER);

        final ProductSyncOptions productSyncOptions = mock(ProductSyncOptions.class);
        when(productSyncOptions.getSyncFilter()).thenReturn(SyncFilter.of());

        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeMetaData priceInfo = mock(AttributeMetaData.class);
        when(priceInfo.getName()).thenReturn("priceInfo");
        when(priceInfo.isRequired()).thenReturn(false);
        attributesMetaData.put("priceInfo", priceInfo);

        final List<UpdateAction<Product>> updateActions =
            buildVariantsUpdateActions(productOld, productDraftNew, productSyncOptions, attributesMetaData);

        // check remove variants are the first in the list, but not the master variant
        assertThat(updateActions.subList(0, 3))
            .containsExactlyInAnyOrder(RemoveVariant.of(2), RemoveVariant.of(3), RemoveVariant.of(4));

        // change master variant must be always after variants are added/updated,
        // because it is set by SKU and we should be sure the master variant is already added and SKUs are actual.
        assertThat(updateActions).endsWith(ChangeMasterVariant.ofSku("var-7-sku", true));

        // Old master variant should NOT be removed because it exists in NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER
        final ProductVariant oldMasterVariant = productOld.getMasterData().getStaged().getMasterVariant();
        assertThat(updateActions).filteredOn(action -> {
                // verify old master variant is not removed
                if (action instanceof RemoveVariant) {
                    RemoveVariant removeVariantAction = (RemoveVariant)action;
                    return Objects.equals(oldMasterVariant.getId(), removeVariantAction.getId())
                        || Objects.equals(oldMasterVariant.getSku(), removeVariantAction.getSku());
                }
                return false;
            }
        ).isEmpty();
    }

    @Test
    public void buildRemoveVariantUpdateAction_removesMissedVariants() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

        ProductData oldStaged = productOld.getMasterData().getStaged();
        Map<String, ProductVariant> oldVariants =
            collectionToMap(oldStaged.getAllVariants(), ProductVariant::getKey);

        ArrayList<ProductVariantDraft> newVariants = new ArrayList<>(productDraftNew.getVariants());
        newVariants.add(productDraftNew.getMasterVariant());

        List<RemoveVariant> updateActions = buildRemoveVariantUpdateActions(oldVariants, newVariants);
        assertThat(updateActions)
            .containsExactlyInAnyOrder(RemoveVariant.of(1), RemoveVariant.of(2), RemoveVariant.of(3));
        // removes master (1) and two other variants (2, 3)
    }

    @Test
    public void buildChangeMasterVariantUpdateAction_changesMasterVariant() throws Exception {
        Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

        List<UpdateAction<Product>> changeMasterVariant =
                buildChangeMasterVariantUpdateAction(mock(ProductSyncOptions.class), productOld, productDraftNew);
        assertThat(changeMasterVariant).hasSize(2);
        assertThat(changeMasterVariant.get(0))
                .isEqualTo(ChangeMasterVariant.ofSku(productDraftNew.getMasterVariant().getSku(), true));
        assertThat(changeMasterVariant.get(1))
            .isEqualTo(RemoveVariant.of(productOld.getMasterData().getStaged().getMasterVariant()));
    }

    @Test
    public void buildAddVariantUpdateActionFromDraft_returnsProperties() throws Exception {
        List<AttributeDraft> attributeList = Collections.emptyList();
        List<PriceDraft> priceList = Collections.emptyList();
        List<Image> imageList = Collections.emptyList();
        ProductVariantDraftDsl draft = ProductVariantDraftBuilder.of()
                .attributes(attributeList)
                .prices(priceList)
                .sku("testSKU")
                .key("testKey")
                .images(imageList)
                .build();

        AddVariant addVariant = buildAddVariantUpdateActionFromDraft(draft);
        assertThat(addVariant.getAttributes()).isSameAs(attributeList);
        assertThat(addVariant.getPrices()).isSameAs(priceList);
        assertThat(addVariant.getSku()).isEqualTo("testSKU");
        assertThat(addVariant.getKey()).isEqualTo("testKey");
        assertThat(addVariant.getImages()).isSameAs(imageList);
    }
}