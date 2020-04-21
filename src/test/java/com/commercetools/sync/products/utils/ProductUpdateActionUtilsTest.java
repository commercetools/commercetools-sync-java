package com.commercetools.sync.products.utils;

import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.SyncFilter;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.models.AssetDraft;
import io.sphere.sdk.models.AssetDraftBuilder;
import io.sphere.sdk.models.AssetSourceBuilder;
import io.sphere.sdk.products.Image;
import io.sphere.sdk.products.PriceDraft;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductDraftBuilder;
import io.sphere.sdk.products.ProductVariant;
import io.sphere.sdk.products.ProductVariantDraft;
import io.sphere.sdk.products.ProductVariantDraftBuilder;
import io.sphere.sdk.products.attributes.AttributeConstraint;
import io.sphere.sdk.products.attributes.AttributeDefinitionBuilder;
import io.sphere.sdk.products.attributes.AttributeDraft;
import io.sphere.sdk.products.commands.updateactions.AddAsset;
import io.sphere.sdk.products.commands.updateactions.AddExternalImage;
import io.sphere.sdk.products.commands.updateactions.AddVariant;
import io.sphere.sdk.products.commands.updateactions.ChangeMasterVariant;
import io.sphere.sdk.products.commands.updateactions.RemoveImage;
import io.sphere.sdk.products.commands.updateactions.RemoveVariant;
import io.sphere.sdk.products.commands.updateactions.SetAttribute;
import io.sphere.sdk.products.commands.updateactions.SetSku;
import io.sphere.sdk.products.commands.updateactions.SetAttributeInAllVariants;
import io.sphere.sdk.producttypes.ProductType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.commercetools.sync.products.ProductSyncMockUtils.createProductDraftFromJson;
import static com.commercetools.sync.products.ProductSyncMockUtils.createProductFromJson;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_KEY;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_SKU;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.BLANK_OLD_MASTER_VARIANT_KEY;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildChangeMasterVariantUpdateAction;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.buildVariantsUpdateActions;
import static com.commercetools.sync.products.utils.ProductUpdateActionUtils.getAllVariants;
import static io.sphere.sdk.models.LocalizedString.ofEnglish;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


class ProductUpdateActionUtilsTest {

    private static final String RES_ROOT = "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/";
    private static final String OLD_PROD_WITH_VARIANTS = RES_ROOT + "productOld.json";
    private static final String OLD_PROD_WITHOUT_MV_KEY_SKU = RES_ROOT + "productOld_noMasterVariantKeySku.json";

    // this product's variants don't contain old master variant
    private static final String NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER =
        RES_ROOT + "productDraftNew_changeRemoveMasterVariant.json";

    // this product's variants contain only attribute update
    private static final String NEW_PROD_DRAFT_WITH_MATCHING_VARIANTS_WITH_UPDATED_ATTR_VALUES =
        RES_ROOT + "productDraftNew_matchingVariants.json";

    // this product's variants contain old master variant, but not as master any more
    private static final String NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER =
        RES_ROOT + "productDraftNew_moveMasterVariant.json";

    private static final String NEW_PROD_DRAFT_WITHOUT_MV = RES_ROOT + "productDraftNew_noMasterVariant.json";

    private static final String NEW_PROD_DRAFT_WITHOUT_MV_KEY = RES_ROOT + "productDraftNew_noMasterVariantKey.json";

    private static final String NEW_PROD_DRAFT_WITHOUT_MV_SKU = RES_ROOT + "productDraftNew_noMasterVariantSku.json";

    @Test
    void buildVariantsUpdateActions_updatesVariants() {
        // preparation
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .syncFilter(SyncFilter.of())
                                                                               .build();

        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeMetaData priceInfo = AttributeMetaData.of(
            AttributeDefinitionBuilder.of("priceInfo", null, null)
                                      .build());
        attributesMetaData.put("priceInfo", priceInfo);

        final List<UpdateAction<Product>> updateActions =
            buildVariantsUpdateActions(productOld, productDraftNew, productSyncOptions, attributesMetaData);

        // check remove variants are the first in the list, but not the master variant
        assertThat(updateActions.subList(0, 2))
            .containsExactlyInAnyOrder(RemoveVariant.ofVariantId(2, true), RemoveVariant.ofVariantId(3, true));

        // check add actions

        final ProductVariantDraft draftMaster = productDraftNew.getMasterVariant();
        final ProductVariantDraft draft5 = productDraftNew.getVariants().get(1);
        final ProductVariantDraft draft6 = productDraftNew.getVariants().get(2);
        final ProductVariantDraft draft7 = productDraftNew.getVariants().get(3);

        assertThat(updateActions).contains(
            AddVariant.of(draftMaster.getAttributes(), draftMaster.getPrices(), draftMaster.getSku(), true)
                      .withKey(draftMaster.getKey())
                      .withImages(draftMaster.getImages()),
            AddVariant.of(draft5.getAttributes(), draft5.getPrices(), draft5.getSku(), true)
                      .withKey(draft5.getKey())
                      .withImages(draft5.getImages()),
            AddVariant.of(draft6.getAttributes(), draft6.getPrices(), draft6.getSku(), true)
                      .withKey(draft6.getKey())
                      .withImages(draft6.getImages()),
            AddVariant.of(draft7.getAttributes(), draft7.getPrices(), draft7.getSku(), true)
                      .withKey(draft7.getKey())
                      .withImages(draft7.getImages()));

        // Check add asset actions of new variants
        assertThat(updateActions).containsAll(
            draft7.getAssets()
                  .stream()
                  .map(assetDraft -> (UpdateAction<Product>) AddAsset
                      .ofSku(draft7.getSku(), assetDraft)
                      .withStaged(true))
                  .collect(toList())
        );

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
    void buildVariantsUpdateActions_updateVariantsWithSameForAll() {
        // preparation
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew = createProductDraftFromJson(
                NEW_PROD_DRAFT_WITH_MATCHING_VARIANTS_WITH_UPDATED_ATTR_VALUES);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                .syncFilter(SyncFilter.of())
                .build();

        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
        final AttributeMetaData priceInfo = AttributeMetaData.of(
                AttributeDefinitionBuilder.of("priceInfo", null, null)
                        .attributeConstraint(AttributeConstraint.SAME_FOR_ALL)
                        .build());
        final AttributeMetaData size = AttributeMetaData.of(
                AttributeDefinitionBuilder.of("size", null, null)
                        .build());
        attributesMetaData.put("priceInfo", priceInfo);
        attributesMetaData.put("size", size);

        final List<UpdateAction<Product>> updateActions =
                buildVariantsUpdateActions(productOld, productDraftNew, productSyncOptions, attributesMetaData);

        // check that we only have one generated action for all the variants and no duplicates
        assertThat(updateActions.size()).isEqualTo(3);
        assertThat(updateActions).containsOnlyOnce(SetAttributeInAllVariants.of(
                AttributeDraft.of("priceInfo", "74,90/kg"), true));
        // Other update actions can be duplicated per variant
        assertThat(updateActions).containsOnlyOnce(SetAttribute.of(2,
                AttributeDraft.of("size", "ca. 1 x 1200 g"), true));
        assertThat(updateActions).containsOnlyOnce(SetAttribute.of(3,
                AttributeDraft.of("size", "ca. 1 x 1200 g"), true));
    }

    @Test
    void buildVariantsUpdateActions_doesNotRemoveMaster() {
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER);

        final ProductSyncOptions productSyncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
                                                                               .syncFilter(SyncFilter.of())
                                                                               .build();

        final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();


        final AttributeMetaData priceInfo = AttributeMetaData.of(
            AttributeDefinitionBuilder.of("priceInfo", null, null)
                                      .build());
        attributesMetaData.put("priceInfo", priceInfo);

        final List<UpdateAction<Product>> updateActions =
            buildVariantsUpdateActions(productOld, productDraftNew, productSyncOptions, attributesMetaData);

        // check remove variants are the first in the list, but not the master variant
        assertThat(updateActions.subList(0, 3))
            .containsExactlyInAnyOrder(RemoveVariant.ofVariantId(2, true),
                RemoveVariant.ofVariantId(3, true), RemoveVariant.ofVariantId(4, true));

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
    void buildVariantsUpdateActions_withEmptyOldMasterVariantKey() {
        assertMissingMasterVariantKey(OLD_PROD_WITHOUT_MV_KEY_SKU, NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER,
            BLANK_OLD_MASTER_VARIANT_KEY);
    }

    @Test
    void buildVariantsUpdateActions_withEmptyNewMasterVariantOrKey_ShouldNotBuildActionAndTriggerCallback() {
        assertMissingMasterVariantKey(OLD_PROD_WITH_VARIANTS, NEW_PROD_DRAFT_WITHOUT_MV, BLANK_NEW_MASTER_VARIANT_KEY);
        assertMissingMasterVariantKey(OLD_PROD_WITH_VARIANTS, NEW_PROD_DRAFT_WITHOUT_MV_KEY,
            BLANK_NEW_MASTER_VARIANT_KEY);
    }

    @Test
    void buildVariantsUpdateActions_withEmptyBothMasterVariantKey_ShouldNotBuildActionAndTriggerCallback() {
        assertMissingMasterVariantKey(OLD_PROD_WITHOUT_MV_KEY_SKU, NEW_PROD_DRAFT_WITHOUT_MV_KEY,
            BLANK_OLD_MASTER_VARIANT_KEY, BLANK_NEW_MASTER_VARIANT_KEY);
    }

    private void assertMissingMasterVariantKey(final String oldProduct,
                                               final String newProduct,
                                               final String ... errorMessages) {
        final Product productOld = createProductFromJson(oldProduct);
        final ProductDraft productDraftNew = createProductDraftFromJson(newProduct);

        final List<String> errorsCatcher = new ArrayList<>();
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((message, exception) -> errorsCatcher.add(message))
            .build();

        final List<UpdateAction<Product>> updateActions =
            buildVariantsUpdateActions(productOld, productDraftNew, syncOptions, emptyMap());
        assertThat(updateActions).isEmpty();
        assertThat(errorsCatcher).hasSize(errorMessages.length);

        // verify all expected error messages
        for (int i = 0; i < errorMessages.length; i++) {
            assertThat(errorsCatcher.get(i))
                .containsIgnoringCase("failed")
                .contains(productOld.getKey())
                .containsIgnoringCase(errorMessages[i]);
        }
    }

    @Test
    void buildChangeMasterVariantUpdateAction_changesMasterVariant() {
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew = createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

        final List<UpdateAction<Product>> changeMasterVariant =
            buildChangeMasterVariantUpdateAction(productOld, productDraftNew,
                ProductSyncOptionsBuilder.of(mock(SphereClient.class)).build());
        assertThat(changeMasterVariant).hasSize(2);
        assertThat(changeMasterVariant.get(0))
                .isEqualTo(ChangeMasterVariant.ofSku(productDraftNew.getMasterVariant().getSku(), true));
        assertThat(changeMasterVariant.get(1))
            .isEqualTo(RemoveVariant.of(productOld.getMasterData().getStaged().getMasterVariant()));
    }

    @Test
    void buildVariantsUpdateActions_withEmptyKey_ShouldNotBuildActionAndTriggerCallback() {
        assertChangeMasterVariantEmptyErrorCatcher(NEW_PROD_DRAFT_WITHOUT_MV_KEY, BLANK_NEW_MASTER_VARIANT_KEY);
    }

    @Test
    void buildVariantsUpdateActions_withEmptySku_ShouldNotBuildActionAndTriggerCallback() {
        assertChangeMasterVariantEmptyErrorCatcher(NEW_PROD_DRAFT_WITHOUT_MV_SKU, BLANK_NEW_MASTER_VARIANT_SKU);
    }

    private void assertChangeMasterVariantEmptyErrorCatcher(final String productMockName,
                                                            final String expectedErrorReason) {
        final Product productOld = createProductFromJson(OLD_PROD_WITH_VARIANTS);
        final ProductDraft productDraftNew_withoutKey = createProductDraftFromJson(productMockName);

        final List<String> errorsCatcher = new ArrayList<>();
        final ProductSyncOptions syncOptions = ProductSyncOptionsBuilder.of(mock(SphereClient.class))
            .errorCallback((message, exception) -> errorsCatcher.add(message))
            .build();

        final List<UpdateAction<Product>> changeMasterVariant =
            buildChangeMasterVariantUpdateAction(productOld, productDraftNew_withoutKey, syncOptions);
        assertThat(changeMasterVariant).hasSize(0);
        assertThat(errorsCatcher).hasSize(1);
        assertThat(errorsCatcher.get(0))
            .containsIgnoringCase("failed")
            .contains(productOld.getKey())
            .containsIgnoringCase(expectedErrorReason);
    }

    @Test
    void buildAddVariantUpdateActionFromDraft_WithAttribsPricesAndImages_ShouldBuildCorrectAddVariantAction() {
        // preparation
        final List<AttributeDraft> attributeList = emptyList();
        final List<PriceDraft> priceList = emptyList();
        final List<Image> imageList = emptyList();
        final ProductVariantDraft draft = ProductVariantDraftBuilder.of()
                                                                    .attributes(attributeList)
                                                                    .prices(priceList)
                                                                    .sku("testSKU")
                                                                    .key("testKey")
                                                                    .images(imageList)
                                                                    .build();

        // test
        final List<UpdateAction<Product>> result = buildAddVariantUpdateActionFromDraft(draft);

        // assertion
        assertThat(result).hasOnlyOneElementSatisfying(action -> {
            assertThat(action).isInstanceOf(AddVariant.class);
            final AddVariant addVariant = (AddVariant) action;
            assertThat(addVariant.getAttributes()).isSameAs(attributeList);
            assertThat(addVariant.getPrices()).isSameAs(priceList);
            assertThat(addVariant.getSku()).isEqualTo("testSKU");
            assertThat(addVariant.getKey()).isEqualTo("testKey");
            assertThat(addVariant.getImages()).isSameAs(imageList);
        });
    }

    @Test
    void buildAddVariantUpdateActionFromDraft_WithNoAssets_BuildsNoAddAssets() {
        // preparation
        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder.of()
                                                                                  .sku("foo")
                                                                                  .build();

        // test
        final List<UpdateAction<Product>> result = buildAddVariantUpdateActionFromDraft(productVariantDraft);

        // assertion
        assertThat(result).containsExactlyInAnyOrder(
            AddVariant.of(null, null, "foo", true)
        );
    }

    @Test
    void buildAddVariantUpdateActionFromDraft_WithMultipleAssets_BuildsMultipleAddAssetsActions() {
        // preparation
        final List<AssetDraft> assetDrafts = IntStream
            .range(1, 4)
            .mapToObj(i -> AssetDraftBuilder
                .of(singletonList(AssetSourceBuilder.ofUri("foo").build()), ofEnglish("assetName"))
                .key(i + "")
                .build())
            .collect(toList());

        final ProductVariantDraft productVariantDraft = ProductVariantDraftBuilder
            .of()
            .sku("foo")
            .assets(assetDrafts)
            .build();

        // test
        final List<UpdateAction<Product>> result = buildAddVariantUpdateActionFromDraft(productVariantDraft);

        // assertion
        final ArrayList<UpdateAction<Product>> expectedActions =
            new ArrayList<>(singletonList(AddVariant.of(null, null, "foo", true)));
        expectedActions.addAll(
            assetDrafts.stream()
                       .map(assetDraft -> AddAsset.ofSku(productVariantDraft.getSku(), assetDraft).withStaged(true))
                       .collect(toList())
        );

        assertThat(result).containsExactlyElementsOf(expectedActions);
    }

    @Test
    void getAllVariants_WithNoVariants_ShouldReturnListWithNullMasterVariant() {
        final ProductDraft productDraft = ProductDraftBuilder
            .of(mock(ProductType.class), ofEnglish("name"), ofEnglish("slug"), emptyList())
            .build();

        final List<ProductVariantDraft> allVariants = getAllVariants(productDraft);

        assertThat(allVariants).hasSize(1).containsOnlyNulls();
    }

    @Test
    void getAllVariants_WithOnlyMasterVariant_ShouldReturnListWithMasterVariant() {
        final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of().build();

        final ProductDraft productDraft = ProductDraftBuilder
            .of(mock(ProductType.class), ofEnglish("name"), ofEnglish("slug"), masterVariant)
            .build();

        final List<ProductVariantDraft> allVariants = getAllVariants(productDraft);

        assertThat(allVariants).containsExactly(masterVariant);
    }

    @Test
    void getAllVariants_WithOnlyVariants_ShouldReturnListWithVariants() {
        final ProductVariantDraft variant1 = ProductVariantDraftBuilder.of().build();
        final ProductVariantDraft variant2 = ProductVariantDraftBuilder.of().build();
        final List<ProductVariantDraft> variants = asList(variant1, variant2);

        final ProductDraft productDraft = ProductDraftBuilder
            .of(mock(ProductType.class), ofEnglish("name"), ofEnglish("slug"), variants)
            .build();

        final List<ProductVariantDraft> allVariants = getAllVariants(productDraft);

        assertThat(allVariants).containsExactlyElementsOf(variants);
    }

    @Test
    void getAllVariants_WithNullInVariants_ShouldReturnListWithVariants() {
        final ProductVariantDraft variant1 = ProductVariantDraftBuilder.of().build();
        final ProductVariantDraft variant2 = ProductVariantDraftBuilder.of().build();
        final List<ProductVariantDraft> variants = asList(variant1, variant2, null);

        final ProductDraft productDraft = ProductDraftBuilder
            .of(mock(ProductType.class), ofEnglish("name"), ofEnglish("slug"), variants)
            .build();

        final List<ProductVariantDraft> allVariants = getAllVariants(productDraft);

        assertThat(allVariants).containsExactlyElementsOf(variants);
    }
}