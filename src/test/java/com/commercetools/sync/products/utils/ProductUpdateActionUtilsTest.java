package com.commercetools.sync.products.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.AssetDraft;
import com.commercetools.api.models.common.AssetDraftBuilder;
import com.commercetools.api.models.common.AssetSourceBuilder;
import com.commercetools.api.models.common.Image;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.PriceDraft;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductAddExternalImageAction;
import com.commercetools.api.models.product.ProductAddVariantAction;
import com.commercetools.api.models.product.ProductAddVariantActionBuilder;
import com.commercetools.api.models.product.ProductChangeMasterVariantAction;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductDraftBuilder;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductRemoveImageAction;
import com.commercetools.api.models.product.ProductRemoveVariantAction;
import com.commercetools.api.models.product.ProductRemoveVariantActionBuilder;
import com.commercetools.api.models.product.ProductSetAttributeAction;
import com.commercetools.api.models.product.ProductSetAttributeInAllVariantsAction;
import com.commercetools.api.models.product.ProductSetSkuActionBuilder;
import com.commercetools.api.models.product.ProductUpdateAction;
import com.commercetools.api.models.product.ProductVariant;
import com.commercetools.api.models.product.ProductVariantDraft;
import com.commercetools.api.models.product.ProductVariantDraftBuilder;
import com.commercetools.api.models.product_type.*;
import com.commercetools.sync.products.AttributeMetaData;
import com.commercetools.sync.products.ProductSyncMockUtils;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.products.ProductSyncOptionsBuilder;
import com.commercetools.sync.products.SyncFilter;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductUpdateActionUtilsTest {

  private static final String RES_ROOT =
      "com/commercetools/sync/products/utils/productVariantUpdateActionUtils/";
  private static final String OLD_PROD_WITH_VARIANTS = RES_ROOT + "productOld.json";
  private static final String OLD_PROD_WITHOUT_MV_KEY_SKU =
      RES_ROOT + "productOld_noMasterVariantKeySku.json";

  private static final String OLD_PROD_NO_ATTRS = RES_ROOT + "productOld_noAttributes.json";

  // this product's variants don't contain old master variant
  private static final String NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER =
      RES_ROOT + "productDraftNew_changeRemoveMasterVariant.json";

  // this product's variants contain only attribute update
  private static final String NEW_PROD_DRAFT_WITH_MATCHING_VARIANTS_WITH_UPDATED_ATTR_VALUES =
      RES_ROOT + "productDraftNew_matchingVariants.json";

  private static final String NEW_PROD_DRAFT_MATCHING_OLD_PRODUCT_NO_ATTRS =
      RES_ROOT + "productDraftNew_matchingProductOld_noAttributes.json";

  // this product's variants contain old master variant, but not as master any more
  private static final String NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER =
      RES_ROOT + "productDraftNew_moveMasterVariant.json";

  private static final String NEW_PROD_DRAFT_WITHOUT_MV =
      RES_ROOT + "productDraftNew_noMasterVariant.json";

  private static final String NEW_PROD_DRAFT_WITHOUT_MV_KEY =
      RES_ROOT + "productDraftNew_noMasterVariantKey.json";

  private static final String NEW_PROD_DRAFT_WITHOUT_MV_SKU =
      RES_ROOT + "productDraftNew_noMasterVariantSku.json";

  private static final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();

  @BeforeAll
  static void beforeAll() {
    final AttributeMetaData priceInfo =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("priceInfo")
                .label(LocalizedString.ofEnglish("priceInfo"))
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .level(AttributeLevelEnum.VARIANT)
                .isRequired(false)
                .isSearchable(true)
                .inputHint(TextInputHint.SINGLE_LINE)
                .build());
    attributesMetaData.put("priceInfo", priceInfo);
    final AttributeMetaData sizeAttributeMetaData =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("size")
                .label(LocalizedString.ofEnglish("size"))
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .level(AttributeLevelEnum.VARIANT)
                .isRequired(false)
                .isSearchable(true)
                .inputHint(TextInputHint.SINGLE_LINE)
                .build());
    attributesMetaData.put("size", sizeAttributeMetaData);
  }

  @Test
  void buildVariantsUpdateActions_updatesVariants() {
    // preparation
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .syncFilter(SyncFilter.of())
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, productSyncOptions, attributesMetaData);

    // check remove variants are the first in the list, but not the master variant
    assertThat(updateActions.subList(0, 2))
        .containsExactlyInAnyOrder(
            ProductRemoveVariantAction.builder().id(2L).staged(true).build(),
            ProductRemoveVariantAction.builder().id(3L).staged(true).build());

    // check add actions

    final ProductVariantDraft draftMaster = productDraftNew.getMasterVariant();
    final ProductVariantDraft draft5 = productDraftNew.getVariants().get(1);
    final ProductVariantDraft draft6 = productDraftNew.getVariants().get(2);
    final ProductVariantDraft draft7 = productDraftNew.getVariants().get(3);

    assertThat(updateActions)
        .contains(
            ProductAddVariantActionBuilder.of()
                .attributes(draftMaster.getAttributes())
                .prices(draftMaster.getPrices())
                .sku(draftMaster.getSku())
                .staged(true)
                .images(draftMaster.getImages())
                .key(draftMaster.getKey())
                .build(),
            ProductAddVariantActionBuilder.of()
                .attributes(draft5.getAttributes())
                .prices(draft5.getPrices())
                .sku(draft5.getSku())
                .staged(true)
                .key(draft5.getKey())
                .images(draft5.getImages())
                .build(),
            ProductAddVariantActionBuilder.of()
                .attributes(draft6.getAttributes())
                .prices(draft6.getPrices())
                .sku(draft6.getSku())
                .staged(true)
                .key(draft6.getKey())
                .images(draft6.getImages())
                .build(),
            ProductAddVariantActionBuilder.of()
                .attributes(draft7.getAttributes())
                .prices(draft7.getPrices())
                .sku(draft7.getSku())
                .staged(true)
                .key(draft7.getKey())
                .images(draft7.getImages())
                .assets(draft7.getAssets())
                .build());

    // variant 4 sku change
    assertThat(updateActions)
        .containsOnlyOnce(
            ProductSetSkuActionBuilder.of().variantId(4L).sku("var-44-sku").staged(true).build());

    // verify image update of variant 4
    ProductRemoveImageAction removeImageAction =
        ProductRemoveImageAction.builder()
            .variantId(4L)
            .imageUrl("https://xxx.ggg/4.png")
            .staged(true)
            .build();
    ProductAddExternalImageAction addExternalImageAction =
        ProductAddExternalImageAction.builder()
            .variantId(4L)
            .image(productDraftNew.getVariants().get(0).getImages().get(0))
            .staged(true)
            .build();

    assertThat(updateActions).containsOnlyOnce(removeImageAction);
    assertThat(updateActions).containsOnlyOnce(addExternalImageAction);
    assertThat(updateActions.indexOf(removeImageAction))
        .withFailMessage("Remove image action must be executed before add image action")
        .isLessThan(updateActions.indexOf(addExternalImageAction));

    // verify attributes changes
    assertThat(updateActions)
        .contains(
            ProductSetAttributeAction.builder()
                .variantId(4L)
                .name("priceInfo")
                .value("44/kg")
                .staged(true)
                .build());

    // change master variant must be always after variants are added/updated,
    // because it is set by SKU and we should be sure the master variant is already added and SKUs
    // are actual.
    // Also, master variant should be removed because it is missing in
    // NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER
    final int size = updateActions.size();
    assertThat(updateActions.subList(size - 2, size))
        .containsExactly(
            ProductChangeMasterVariantAction.builder().sku("var-7-sku").staged(true).build(),
            ProductRemoveVariantAction.builder().id(productOld.getMasterVariant().getId()).build());
  }

  @Test
  void buildVariantsUpdateActions_updateVariantsWithSameForAll() {
    // preparation
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(
            NEW_PROD_DRAFT_WITH_MATCHING_VARIANTS_WITH_UPDATED_ATTR_VALUES);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .syncFilter(SyncFilter.of())
            .build();

    final Map<String, AttributeMetaData> attributesMetaData = new HashMap<>();
    final AttributeMetaData priceInfo =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("priceInfo")
                .label(LocalizedString.ofEnglish("priceInfo"))
                .attributeConstraint(AttributeConstraintEnum.SAME_FOR_ALL)
                .type(AttributeTypeBuilder::textBuilder)
                .level(AttributeLevelEnum.VARIANT)
                .isRequired(false)
                .isSearchable(true)
                .inputHint(TextInputHint.SINGLE_LINE)
                .build());
    final AttributeMetaData size =
        AttributeMetaData.of(
            AttributeDefinitionBuilder.of()
                .name("size")
                .label(LocalizedString.ofEnglish("size"))
                .attributeConstraint(AttributeConstraintEnum.NONE)
                .type(AttributeTypeBuilder::textBuilder)
                .level(AttributeLevelEnum.VARIANT)
                .isRequired(false)
                .isSearchable(true)
                .inputHint(TextInputHint.SINGLE_LINE)
                .build());
    attributesMetaData.put("priceInfo", priceInfo);
    attributesMetaData.put("size", size);

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, productSyncOptions, attributesMetaData);

    // check that we only have one generated action for all the variants and no duplicates
    assertThat(updateActions.size()).isEqualTo(3);
    assertThat(updateActions)
        .containsOnlyOnce(
            ProductSetAttributeInAllVariantsAction.builder()
                .name("priceInfo")
                .value("74,90/kg")
                .staged(true)
                .build());
    // Other update actions can be duplicated per variant
    assertThat(updateActions)
        .containsOnlyOnce(
            ProductSetAttributeAction.builder()
                .variantId(2L)
                .name("size")
                .value("ca. 1 x 1200 g")
                .staged(true)
                .build());
    assertThat(updateActions)
        .containsOnlyOnce(
            ProductSetAttributeAction.builder()
                .variantId(3L)
                .name("size")
                .value("ca. 1 x 1200 g")
                .staged(true)
                .build());
  }

  @Test
  void buildVariantsUpdateActions_doesNotRemoveMaster() {
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .syncFilter(SyncFilter.of())
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, productSyncOptions, attributesMetaData);

    // check remove variants are the first in the list, but not the master variant
    assertThat(updateActions.subList(0, 3))
        .containsExactlyInAnyOrder(
            ProductRemoveVariantAction.builder().id(2L).staged(true).build(),
            ProductRemoveVariantAction.builder().id(3L).staged(true).build(),
            ProductRemoveVariantAction.builder().id(4L).staged(true).build());

    // change master variant must be always after variants are added/updated,
    // because it is set by SKU and we should be sure the master variant is already added and SKUs
    // are actual.
    assertThat(updateActions)
        .endsWith(ProductChangeMasterVariantAction.builder().sku("var-7-sku").staged(true).build());

    // Old master variant should NOT be removed because it exists in
    // NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER
    final ProductVariant oldMasterVariant = productOld.getMasterVariant();
    assertThat(updateActions)
        .filteredOn(
            action -> {
              // verify old master variant is not removed
              if (action instanceof ProductRemoveVariantAction) {
                ProductRemoveVariantAction removeVariantAction =
                    (ProductRemoveVariantAction) action;
                return Objects.equals(oldMasterVariant.getId(), removeVariantAction.getId())
                    || Objects.equals(oldMasterVariant.getSku(), removeVariantAction.getSku());
              }
              return false;
            })
        .isEmpty();
  }

  @Test
  void buildVariantsUpdateActions_withEmptyOldMasterVariantKey() {
    assertMissingMasterVariantKey(
        OLD_PROD_WITHOUT_MV_KEY_SKU,
        NEW_PROD_DRAFT_WITH_VARIANTS_MOVE_MASTER,
        ProductUpdateActionUtils.BLANK_OLD_MASTER_VARIANT_KEY);
  }

  @Test
  void
      buildVariantsUpdateActions_withEmptyNewMasterVariantOrKey_ShouldNotBuildActionAndTriggerCallback() {
    assertMissingMasterVariantKey(
        OLD_PROD_WITH_VARIANTS,
        NEW_PROD_DRAFT_WITHOUT_MV,
        ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_KEY);
    assertMissingMasterVariantKey(
        OLD_PROD_WITH_VARIANTS,
        NEW_PROD_DRAFT_WITHOUT_MV_KEY,
        ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_KEY);
  }

  @Test
  void
      buildVariantsUpdateActions_withEmptyBothMasterVariantKey_ShouldNotBuildActionAndTriggerCallback() {
    assertMissingMasterVariantKey(
        OLD_PROD_WITHOUT_MV_KEY_SKU,
        NEW_PROD_DRAFT_WITHOUT_MV_KEY,
        ProductUpdateActionUtils.BLANK_OLD_MASTER_VARIANT_KEY,
        ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_KEY);
  }

  private void assertMissingMasterVariantKey(
      final String oldProduct, final String newProduct, final String... errorMessages) {
    final ProductProjection productOld = ProductSyncMockUtils.createProductFromJson(oldProduct);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(newProduct);

    final List<String> errorsCatcher = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorsCatcher.add(exception.getMessage()))
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, syncOptions, emptyMap());
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
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

    final List<ProductUpdateAction> changeMasterVariant =
        ProductUpdateActionUtils.buildChangeMasterVariantUpdateAction(
            productOld,
            productDraftNew,
            ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class)).build());
    assertThat(changeMasterVariant).hasSize(2);
    assertThat(changeMasterVariant.get(0))
        .isEqualTo(
            ProductChangeMasterVariantAction.builder()
                .sku(productDraftNew.getMasterVariant().getSku())
                .staged(true)
                .build());
    assertThat(changeMasterVariant.get(1))
        .isEqualTo(
            ProductRemoveVariantAction.builder().id(productOld.getMasterVariant().getId()).build());
  }

  @Test
  void buildVariantsUpdateActions_withEmptyKey_ShouldNotBuildActionAndTriggerCallback() {
    assertChangeMasterVariantEmptyErrorCatcher(
        NEW_PROD_DRAFT_WITHOUT_MV_KEY, ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_KEY);
  }

  @Test
  void buildVariantsUpdateActions_withEmptySku_ShouldNotBuildActionAndTriggerCallback() {
    assertChangeMasterVariantEmptyErrorCatcher(
        NEW_PROD_DRAFT_WITHOUT_MV_SKU, ProductUpdateActionUtils.BLANK_NEW_MASTER_VARIANT_SKU);
  }

  @Test
  void buildVariantsUpdateActions_withNullNewVariants_ShouldRemoveOldVariants() {
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .syncFilter(SyncFilter.of())
            .build();

    productDraftNew.setVariants((List<ProductVariantDraft>) null);

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, productSyncOptions, attributesMetaData);

    final List<ProductRemoveVariantAction> productRemoveVariantActions =
        productOld.getVariants().stream()
            .map(
                productVariant ->
                    ProductRemoveVariantActionBuilder.of()
                        .id(productVariant.getId())
                        .staged(true)
                        .build())
            .collect(toList());

    assertThat(updateActions).containsAll(productRemoveVariantActions);
  }

  @Test
  void buildVariantsUpdateActions_withNewVariantsArrayContainingNulls_ShouldRemoveOldVariants() {
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(NEW_PROD_DRAFT_WITH_VARIANTS_REMOVE_MASTER);

    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .syncFilter(SyncFilter.of())
            .build();

    productDraftNew.setVariants((ProductVariantDraft) null);

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, productSyncOptions, attributesMetaData);

    final List<ProductRemoveVariantAction> productRemoveVariantActions =
        productOld.getVariants().stream()
            .map(
                productVariant ->
                    ProductRemoveVariantActionBuilder.of()
                        .id(productVariant.getId())
                        .staged(true)
                        .build())
            .collect(toList());

    assertThat(updateActions).containsAll(productRemoveVariantActions);
  }

  @Test
  void
      buildVariantsUpdateActions_withNewVariantsArrayContainingNullsAndOldVariants_ShouldCallErrorCallback() {
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_NO_ATTRS);
    final ProductDraft productDraftNew =
        ProductSyncMockUtils.createProductDraftFromJson(
            NEW_PROD_DRAFT_MATCHING_OLD_PRODUCT_NO_ATTRS);
    productDraftNew.getVariants().add(null);

    final List<String> errorsCatcher = new ArrayList<>();
    final ProductSyncOptions productSyncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorsCatcher.add(exception.getMessage()))
            .syncFilter(SyncFilter.of())
            .build();

    final List<ProductUpdateAction> updateActions =
        ProductUpdateActionUtils.buildVariantsUpdateActions(
            productOld, productDraftNew, productSyncOptions, Collections.emptyMap());

    assertThat(updateActions).isEmpty();
    assertThat(errorsCatcher).hasSize(1);
    assertThat(errorsCatcher.get(0))
        .containsIgnoringCase("failed")
        .contains(productOld.getKey())
        .contains(ProductUpdateActionUtils.NULL_VARIANT);
  }

  private void assertChangeMasterVariantEmptyErrorCatcher(
      final String productMockName, final String expectedErrorReason) {
    final ProductProjection productOld =
        ProductSyncMockUtils.createProductFromJson(OLD_PROD_WITH_VARIANTS);
    final ProductDraft productDraftNew_withoutKey =
        ProductSyncMockUtils.createProductDraftFromJson(productMockName);

    final List<String> errorsCatcher = new ArrayList<>();
    final ProductSyncOptions syncOptions =
        ProductSyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, updateActions) ->
                    errorsCatcher.add(exception.getMessage()))
            .build();

    final List<ProductUpdateAction> changeMasterVariant =
        ProductUpdateActionUtils.buildChangeMasterVariantUpdateAction(
            productOld, productDraftNew_withoutKey, syncOptions);
    assertThat(changeMasterVariant).hasSize(0);
    assertThat(errorsCatcher).hasSize(1);
    assertThat(errorsCatcher.get(0))
        .containsIgnoringCase("failed")
        .contains(productOld.getKey())
        .containsIgnoringCase(expectedErrorReason);
  }

  @Test
  void
      buildAddVariantUpdateActionFromDraft_WithAttribsPricesAndImages_ShouldBuildCorrectAddVariantAction() {
    // preparation
    final List<Attribute> attributeList = emptyList();
    final List<PriceDraft> priceList = emptyList();
    final List<Image> imageList = emptyList();
    final ProductVariantDraft draft =
        ProductVariantDraftBuilder.of()
            .attributes(attributeList)
            .prices(priceList)
            .sku("testSKU")
            .key("testKey")
            .images(imageList)
            .build();

    // test
    final ProductUpdateAction action =
        ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft(draft);

    // assertion
    assertThat(action).isInstanceOf(ProductAddVariantAction.class);
    final ProductAddVariantAction addVariant = (ProductAddVariantAction) action;
    assertThat(addVariant.getAttributes()).isSameAs(attributeList);
    assertThat(addVariant.getPrices()).isSameAs(priceList);
    assertThat(addVariant.getSku()).isEqualTo("testSKU");
    assertThat(addVariant.getKey()).isEqualTo("testKey");
    assertThat(addVariant.getImages()).isSameAs(imageList);
  }

  @Test
  void buildAddVariantUpdateActionFromDraft_WithNoAssets_BuildsAddVariantActionWithoutAssets() {
    // preparation
    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().sku("foo").build();

    // test
    final ProductUpdateAction action =
        ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft(productVariantDraft);

    // assertion
    assertThat(action)
        .isEqualTo(ProductAddVariantActionBuilder.of().sku("foo").staged(true).build());
  }

  @Test
  void buildAddVariantUpdateActionFromDraft_WithMultipleAssets_BuildsAddVariantActionWithAssets() {
    // preparation
    final List<AssetDraft> assetDrafts =
        IntStream.range(1, 4)
            .mapToObj(
                i ->
                    AssetDraftBuilder.of()
                        .sources(AssetSourceBuilder.of().uri("foo").build())
                        .name(LocalizedString.of(Locale.ENGLISH, "assetName"))
                        .key(i + "")
                        .build())
            .collect(toList());

    final ProductVariantDraft productVariantDraft =
        ProductVariantDraftBuilder.of().sku("foo").assets(assetDrafts).build();

    // test
    final ProductUpdateAction action =
        ProductUpdateActionUtils.buildAddVariantUpdateActionFromDraft(productVariantDraft);

    // assertion
    assertThat(action)
        .isEqualTo(
            ProductAddVariantActionBuilder.of()
                .sku("foo")
                .assets(assetDrafts)
                .staged(true)
                .build());
  }

  @Test
  void getAllVariants_WithNoVariants_ShouldReturnEmptyList() {
    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(mock(ProductTypeResourceIdentifier.class))
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .masterVariant((ProductVariantDraft) null)
            .variants(emptyList())
            .build();

    final List<ProductVariantDraft> allVariants =
        ProductUpdateActionUtils.getAllVariants(productDraft);

    assertThat(allVariants).hasSize(0);
  }

  @Test
  void getAllVariants_WithOnlyMasterVariant_ShouldReturnListWithMasterVariant() {
    final ProductVariantDraft masterVariant = ProductVariantDraftBuilder.of().build();

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(mock(ProductTypeResourceIdentifier.class))
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .masterVariant(masterVariant)
            .build();

    final List<ProductVariantDraft> allVariants =
        ProductUpdateActionUtils.getAllVariants(productDraft);

    assertThat(allVariants).containsExactly(masterVariant);
  }

  @Test
  void getAllVariants_WithOnlyVariants_ShouldReturnListWithVariants() {
    final ProductVariantDraft variant1 = ProductVariantDraftBuilder.of().build();
    final ProductVariantDraft variant2 = ProductVariantDraftBuilder.of().build();
    final List<ProductVariantDraft> variants = asList(variant1, variant2);

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(mock(ProductTypeResourceIdentifier.class))
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .masterVariant(variant1)
            .variants(variant2)
            .build();

    final List<ProductVariantDraft> allVariants =
        ProductUpdateActionUtils.getAllVariants(productDraft);

    assertThat(allVariants).containsExactlyElementsOf(variants);
  }

  @Test
  void getAllVariants_WithNullInVariants_ShouldReturnListWithVariants() {
    final ProductVariantDraft variant1 = ProductVariantDraftBuilder.of().build();
    final ProductVariantDraft variant2 = ProductVariantDraftBuilder.of().build();
    final List<ProductVariantDraft> variants = asList(variant1, variant2, null);

    final ProductDraft productDraft =
        ProductDraftBuilder.of()
            .productType(mock(ProductTypeResourceIdentifier.class))
            .name(LocalizedString.of(Locale.ENGLISH, "name"))
            .slug(LocalizedString.of(Locale.ENGLISH, "slug"))
            .masterVariant(variant1)
            .variants(variant2, null)
            .build();

    final List<ProductVariantDraft> allVariants =
        ProductUpdateActionUtils.getAllVariants(productDraft);

    assertThat(allVariants).containsExactlyElementsOf(variants);
  }
}
